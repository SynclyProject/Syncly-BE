#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

MATRIX_FILE="${MATRIX_FILE:-$ROOT_DIR/scripts/security/jwt-attack-matrix.json}"
COMPOSE_FILE="${COMPOSE_FILE:-$ROOT_DIR/docker/security/docker-compose.jwt-matrix.yml}"
ENV_FILE="${ENV_FILE:-$ROOT_DIR/docker/security/env.jwt-matrix.example}"
OUTPUT_DIR="${OUTPUT_DIR:-/tmp/syncly-jwt-matrix}"
RUN_ID="${RUN_ID:-$(date -u +%Y%m%dT%H%M%SZ)}"
RESULT_FILE="${RESULT_FILE:-$OUTPUT_DIR/results-$RUN_ID.ndjson}"
SUMMARY_FILE="${SUMMARY_FILE:-$OUTPUT_DIR/summary-$RUN_ID.json}"
META_FILE="${META_FILE:-$OUTPUT_DIR/meta-$RUN_ID.json}"

TRUSTED_ORIGIN="${TRUSTED_ORIGIN:-http://localhost:5173}"
VICTIM_UA="${VICTIM_UA:-MatrixVictim/1.0}"
ATTACKER_UA="${ATTACKER_UA:-MatrixAttacker/1.0}"

SKIP_STACK_UP="${SKIP_STACK_UP:-0}"
KEEP_STACK="${KEEP_STACK:-0}"

DEFAULT_BCRYPT='$2a$10$SHBeLpz3IRNpx6gvHLW6Je6IcJa4p/zdNBrFOGtFK93eZUKov4qO2'

log() {
  printf '[jwt-matrix] %s\n' "$*"
}

fail() {
  printf '[jwt-matrix][error] %s\n' "$*" >&2
  exit 1
}

timestamp_ms() {
  if command -v perl >/dev/null 2>&1; then
    perl -MTime::HiRes=time -e 'printf("%.0f\n", time()*1000)'
  else
    date +%s
  fi
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || fail "필수 명령어 없음: $1"
}

compose() {
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" "$@"
}

cleanup() {
  if [[ "$KEEP_STACK" == "1" || "$SKIP_STACK_UP" == "1" ]]; then
    return
  fi
  log "스택 종료"
  compose down -v >/dev/null 2>&1 || true
}

extract_http_status() {
  local header_file="$1"
  awk '/^HTTP/{code=$2} END{print code+0}' "$header_file"
}

extract_refresh_cookie() {
  local header_file="$1"
  tr -d '\r' < "$header_file" | sed -n 's/^Set-Cookie: REFRESH=\([^;]*\).*/\1/p' | tail -n 1
}

json_code() {
  local body_file="$1"
  jq -r '.code // empty' "$body_file" 2>/dev/null || true
}

b64url_decode() {
  local input="$1"
  local mod=$(( ${#input} % 4 ))
  if [[ "$mod" -eq 2 ]]; then
    input+="=="
  elif [[ "$mod" -eq 3 ]]; then
    input+="="
  elif [[ "$mod" -eq 1 ]]; then
    input+="==="
  fi

  if base64 --help 2>&1 | grep -q -- '--decode'; then
    printf '%s' "$input" | tr '_-' '/+' | base64 --decode 2>/dev/null || true
  else
    printf '%s' "$input" | tr '_-' '/+' | base64 -D 2>/dev/null || true
  fi
}

jwt_claim() {
  local token="$1"
  local claim="$2"
  local payload
  payload="$(printf '%s' "$token" | cut -d'.' -f2)"
  b64url_decode "$payload" | jq -r --arg c "$claim" '.[$c] // empty' 2>/dev/null || true
}

redis_get() {
  local key="$1"
  compose exec -T redis redis-cli --raw GET "$key" | tr -d '\r' || true
}

mysql_exec() {
  local sql="$1"
  compose exec -T mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" -e "$sql" >/dev/null
}

wait_for_mysql() {
  log "MySQL 준비 대기"
  local i
  for i in $(seq 1 60); do
    if compose exec -T mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e 'SELECT 1' >/dev/null 2>&1; then
      return
    fi
    sleep 2
  done
  fail "MySQL 준비 실패"
}

wait_for_api() {
  log "API 준비 대기"
  local i code
  for i in $(seq 1 180); do
    code="$(curl -s -o /dev/null -w '%{http_code}' "$API_BASE/api/auth/login" || true)"
    if [[ "$code" != "000" ]]; then
      return
    fi
    sleep 2
  done
  fail "API 준비 실패: $API_BASE"
}

seed_member() {
  log "테스트 멤버 시드"
  local hash="${MATRIX_PASSWORD_BCRYPT:-$DEFAULT_BCRYPT}"
  local email_esc="${MATRIX_EMAIL//\'/\\\'}"
  local hash_esc="${hash//\'/\\\'}"
  mysql_exec "DELETE FROM members WHERE email='${email_esc}';"
  mysql_exec "INSERT INTO members (email, password, name, social_login_provider, is_deleted, created_at, updated_at) VALUES ('${email_esc}', '${hash_esc}', 'matrix-user', 'LOCAL', 0, NOW(), NOW());"
}

login_and_capture() {
  local header_file body_file status refresh
  header_file="$(mktemp)"
  body_file="$(mktemp)"

  curl -sS -D "$header_file" -o "$body_file" -X POST "$API_BASE/api/auth/login" \
    -H "Content-Type: application/json" \
    -H "User-Agent: $VICTIM_UA" \
    --data "{\"email\":\"$MATRIX_EMAIL\",\"password\":\"$MATRIX_PASSWORD\"}" >/dev/null

  status="$(extract_http_status "$header_file")"
  if [[ "$status" != "200" ]]; then
    cat "$body_file" >&2
    rm -f "$header_file" "$body_file"
    fail "로그인 실패: HTTP $status"
  fi

  refresh="$(extract_refresh_cookie "$header_file")"
  if [[ -z "$refresh" ]]; then
    rm -f "$header_file" "$body_file"
    fail "로그인 성공했지만 REFRESH 쿠키가 없음"
  fi

  VICTIM_RT="$refresh"
  ATTACKER_RT="$refresh"

  rm -f "$header_file" "$body_file"
}

current_key_from_token() {
  local token="$1"
  local sub did
  sub="$(jwt_claim "$token" sub)"
  did="$(jwt_claim "$token" did)"
  printf 'refresh:current:%s:%s' "$sub" "$did"
}

ua_key_from_token() {
  local token="$1"
  local sub did
  sub="$(jwt_claim "$token" sub)"
  did="$(jwt_claim "$token" did)"
  printf 'CASHED:UA_HASH:%s:%s' "$sub" "$did"
}

write_result_line() {
  local json_line="$1"
  printf '%s\n' "$json_line" >> "$RESULT_FILE"
}

ua_value() {
  local mode="$1"
  case "$mode" in
    same|victim)
      printf '%s' "$VICTIM_UA"
      ;;
    diff|attacker)
      printf '%s' "$ATTACKER_UA"
      ;;
    none)
      printf ''
      ;;
    *)
      fail "지원하지 않는 ua_mode: $mode"
      ;;
  esac
}

append_step_result() {
  local scenario_id="$1"
  local step_id="$2"
  local actor="$3"
  local gt_attack="$4"
  local ua_mode="$5"
  local apply_cookie="$6"
  local note="$7"
  local token_before="$8"
  local before_current="$9"
  local before_ua="${10}"
  local header_file="${11}"
  local body_file="${12}"

  local status body_code new_refresh response_jti used_jti
  local current_key ua_key after_current after_ua
  local detected=false

  status="$(extract_http_status "$header_file")"
  body_code="$(json_code "$body_file")"
  new_refresh="$(extract_refresh_cookie "$header_file")"

  used_jti="$(jwt_claim "$token_before" jti)"
  if [[ -n "$new_refresh" ]]; then
    response_jti="$(jwt_claim "$new_refresh" jti)"
  else
    response_jti=""
  fi

  current_key="$(current_key_from_token "$token_before")"
  ua_key="$(ua_key_from_token "$token_before")"
  sleep 0.05
  after_current="$(redis_get "$current_key")"
  after_ua="$(redis_get "$ua_key")"

  if [[ "$body_code" == "JWT401_06" && -z "$after_current" ]]; then
    detected=true
  fi

  if [[ "$apply_cookie" == "apply" && -n "$new_refresh" ]]; then
    if [[ "$actor" == "victim" ]]; then
      VICTIM_RT="$new_refresh"
    else
      ATTACKER_RT="$new_refresh"
    fi
  fi

  write_result_line "$(jq -nc \
    --arg scenario_id "$scenario_id" \
    --arg step_id "$step_id" \
    --arg actor "$actor" \
    --arg ua_mode "$ua_mode" \
    --arg apply_cookie "$apply_cookie" \
    --arg status "$status" \
    --arg body_code "$body_code" \
    --arg used_jti "$used_jti" \
    --arg response_jti "$response_jti" \
    --arg before_current "$before_current" \
    --arg after_current "$after_current" \
    --arg before_ua_hash "$before_ua" \
    --arg after_ua_hash "$after_ua" \
    --arg note "$note" \
    --arg timestamp "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
    --argjson gt_attack "$gt_attack" \
    --argjson detected "$detected" \
    '{scenario_id:$scenario_id,step_id:$step_id,actor:$actor,gt_attack:$gt_attack,ua_mode:$ua_mode,apply_cookie:$apply_cookie,status:$status,body_code:$body_code,detected:$detected,used_jti:$used_jti,response_jti:$response_jti,redis_before:{current:$before_current,ua_hash:$before_ua_hash},redis_after:{current:$after_current,ua_hash:$after_ua_hash},note:$note,timestamp:$timestamp}')"
}

request_reissue() {
  local token="$1"
  local ua_mode="$2"
  local header_file="$3"
  local body_file="$4"

  local ua
  ua="$(ua_value "$ua_mode")"

  if [[ -n "$ua" ]]; then
    curl -sS -D "$header_file" -o "$body_file" -X POST "$API_BASE/api/auth/reissue" \
      -H "Origin: $TRUSTED_ORIGIN" \
      -H "Referer: $TRUSTED_ORIGIN/matrix" \
      -H "User-Agent: $ua" \
      -H "Cookie: REFRESH=$token" >/dev/null
  else
    curl -sS -D "$header_file" -o "$body_file" -X POST "$API_BASE/api/auth/reissue" \
      -H "Origin: $TRUSTED_ORIGIN" \
      -H "Referer: $TRUSTED_ORIGIN/matrix" \
      -H "User-Agent:" \
      -H "Cookie: REFRESH=$token" >/dev/null
  fi
}

reissue_step() {
  local scenario_id="$1"
  local step_id="$2"
  local gt_attack="$3"
  local actor="$4"
  local ua_mode="$5"
  local apply_cookie="$6"
  local note="$7"

  local token header_file body_file current_key ua_key before_current before_ua
  if [[ "$actor" == "victim" ]]; then
    token="$VICTIM_RT"
  else
    token="$ATTACKER_RT"
  fi

  current_key="$(current_key_from_token "$token")"
  ua_key="$(ua_key_from_token "$token")"
  before_current="$(redis_get "$current_key")"
  before_ua="$(redis_get "$ua_key")"

  header_file="$(mktemp)"
  body_file="$(mktemp)"

  request_reissue "$token" "$ua_mode" "$header_file" "$body_file"
  append_step_result "$scenario_id" "$step_id" "$actor" "$gt_attack" "$ua_mode" "$apply_cookie" "$note" "$token" "$before_current" "$before_ua" "$header_file" "$body_file"

  rm -f "$header_file" "$body_file"
}

logout_step() {
  local scenario_id="$1"
  local step_id="$2"
  local gt_attack="$3"
  local actor="$4"
  local ua_mode="$5"
  local note="$6"

  local token header_file body_file status body_code

  if [[ "$actor" == "victim" ]]; then
    token="$VICTIM_RT"
  else
    token="$ATTACKER_RT"
  fi

  header_file="$(mktemp)"
  body_file="$(mktemp)"

  curl -sS -D "$header_file" -o "$body_file" -X POST "$API_BASE/api/auth/logout" \
    -H "Origin: $TRUSTED_ORIGIN" \
    -H "Referer: $TRUSTED_ORIGIN/matrix" \
    -H "User-Agent: $(ua_value "$ua_mode")" \
    -H "Cookie: REFRESH=$token" >/dev/null

  status="$(extract_http_status "$header_file")"
  body_code="$(json_code "$body_file")"

  write_result_line "$(jq -nc \
    --arg scenario_id "$scenario_id" \
    --arg step_id "$step_id" \
    --arg actor "$actor" \
    --arg status "$status" \
    --arg body_code "$body_code" \
    --arg note "$note" \
    --arg timestamp "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
    --argjson gt_attack "$gt_attack" \
    '{scenario_id:$scenario_id,step_id:$step_id,actor:$actor,gt_attack:$gt_attack,ua_mode:"logout",apply_cookie:"n/a",status:$status,body_code:$body_code,detected:false,note:$note,timestamp:$timestamp}')"

  rm -f "$header_file" "$body_file"
}

parallel_pair_shared_token() {
  local scenario_id="$1"
  local gt_attack="$2"
  local actor1="$3"
  local ua1="$4"
  local actor2="$5"
  local ua2="$6"
  local apply_order="$7"
  local note="$8"

  local shared_token
  shared_token="$VICTIM_RT"

  local d1 d2 current_key ua_key before_current before_ua
  d1="$(mktemp -d)"
  d2="$(mktemp -d)"

  current_key="$(current_key_from_token "$shared_token")"
  ua_key="$(ua_key_from_token "$shared_token")"
  before_current="$(redis_get "$current_key")"
  before_ua="$(redis_get "$ua_key")"

  (
    request_reissue "$shared_token" "$ua1" "$d1/header" "$d1/body"
    timestamp_ms > "$d1/end_ts"
  ) &
  local p1=$!

  (
    request_reissue "$shared_token" "$ua2" "$d2/header" "$d2/body"
    timestamp_ms > "$d2/end_ts"
  ) &
  local p2=$!

  wait "$p1" "$p2"

  local order
  if [[ "$apply_order" == "completion" ]]; then
    order="$(
      printf '1 %s\n' "$(cat "$d1/end_ts")"
      printf '2 %s\n' "$(cat "$d2/end_ts")"
    )"
    mapfile -t ORDERED < <(printf '%s\n' "$order" | sort -k2n | awk '{print $1}')
  elif [[ "$apply_order" == "reverse_completion" ]]; then
    order="$(
      printf '1 %s\n' "$(cat "$d1/end_ts")"
      printf '2 %s\n' "$(cat "$d2/end_ts")"
    )"
    mapfile -t ORDERED < <(printf '%s\n' "$order" | sort -k2nr | awk '{print $1}')
  else
    fail "지원하지 않는 apply_order: $apply_order"
  fi

  local idx
  for idx in "${ORDERED[@]}"; do
    if [[ "$idx" == "1" ]]; then
      append_step_result "$scenario_id" "$scenario_id.1" "$actor1" "$gt_attack" "$ua1" "apply" "$note" "$shared_token" "$before_current" "$before_ua" "$d1/header" "$d1/body"
    else
      append_step_result "$scenario_id" "$scenario_id.2" "$actor2" "$gt_attack" "$ua2" "apply" "$note" "$shared_token" "$before_current" "$before_ua" "$d2/header" "$d2/body"
    fi
  done

  rm -rf "$d1" "$d2"
}

parallel_many_attacker_reuse() {
  local scenario_id="$1"
  local gt_attack="$2"
  local count="$3"
  local note="$4"

  local stolen_token
  stolen_token="$ATTACKER_RT"

  local tmpdir current_key ua_key before_current before_ua
  tmpdir="$(mktemp -d)"
  local i

  current_key="$(current_key_from_token "$stolen_token")"
  ua_key="$(ua_key_from_token "$stolen_token")"
  before_current="$(redis_get "$current_key")"
  before_ua="$(redis_get "$ua_key")"

  for i in $(seq 1 "$count"); do
    (
      request_reissue "$stolen_token" diff "$tmpdir/h-$i" "$tmpdir/b-$i"
      timestamp_ms > "$tmpdir/t-$i"
    ) &
  done
  wait

  for i in $(seq 1 "$count"); do
    append_step_result "$scenario_id" "$scenario_id.$i" attacker "$gt_attack" diff drop "$note" "$stolen_token" "$before_current" "$before_ua" "$tmpdir/h-$i" "$tmpdir/b-$i"
  done

  rm -rf "$tmpdir"
}

gt_attack_for() {
  local scenario_id="$1"
  jq -r --arg id "$scenario_id" '.[] | select(.id == $id) | .gt_attack' "$MATRIX_FILE"
}

run_scenario() {
  local scenario_id="$1"
  local gt_attack
  gt_attack="$(gt_attack_for "$scenario_id")"

  log "시나리오 시작: $scenario_id"
  login_and_capture

  case "$scenario_id" in
    N1)
      reissue_step "$scenario_id" "$scenario_id.1" "$gt_attack" victim victim apply "정상 단일 재발급"
      ;;

    N2)
      reissue_step "$scenario_id" "$scenario_id.1" "$gt_attack" victim victim drop "응답 유실 가정: 쿠키 미반영"
      sleep 1
      reissue_step "$scenario_id" "$scenario_id.2" "$gt_attack" victim victim apply "10초 이내 동일 UA 재시도"
      ;;

    N3)
      reissue_step "$scenario_id" "$scenario_id.1" "$gt_attack" victim victim drop "응답 유실 가정: 쿠키 미반영"
      sleep 11
      reissue_step "$scenario_id" "$scenario_id.2" "$gt_attack" victim victim apply "10초 초과 동일 UA 재시도"
      ;;

    N4)
      parallel_pair_shared_token "$scenario_id" "$gt_attack" victim victim victim victim completion "동일 UA 동시 2요청(완료 순 반영)"
      ;;

    N5)
      parallel_pair_shared_token "$scenario_id" "$gt_attack" victim victim victim victim reverse_completion "동일 UA 동시 2요청(역순 반영)"
      ;;

    A1)
      reissue_step "$scenario_id" "$scenario_id.1" "$gt_attack" victim victim apply "피해자 정상 재발급"
      reissue_step "$scenario_id" "$scenario_id.2" "$gt_attack" attacker diff apply "탈취 토큰 재사용(상이 UA)"
      ;;

    A2)
      reissue_step "$scenario_id" "$scenario_id.1" "$gt_attack" victim victim apply "피해자 정상 재발급"
      reissue_step "$scenario_id" "$scenario_id.2" "$gt_attack" attacker same apply "탈취 토큰 재사용(동일 UA spoof)"
      ;;

    A3)
      reissue_step "$scenario_id" "$scenario_id.1" "$gt_attack" victim victim apply "피해자 정상 재발급"
      sleep 11
      reissue_step "$scenario_id" "$scenario_id.2" "$gt_attack" attacker same apply "탈취 토큰 재사용(동일 UA spoof, 10초 초과)"
      ;;

    A4)
      reissue_step "$scenario_id" "$scenario_id.1" "$gt_attack" victim victim apply "피해자 정상 재발급"
      reissue_step "$scenario_id" "$scenario_id.2" "$gt_attack" attacker none apply "탈취 토큰 재사용(UA 없음)"
      ;;

    A5)
      reissue_step "$scenario_id" "$scenario_id.1" "$gt_attack" attacker diff apply "선점 공격: attacker 먼저 사용"
      reissue_step "$scenario_id" "$scenario_id.2" "$gt_attack" victim victim apply "피해자 후속 재발급"
      ;;

    A6)
      logout_step "$scenario_id" "$scenario_id.1" "$gt_attack" victim victim "피해자 로그아웃"
      reissue_step "$scenario_id" "$scenario_id.2" "$gt_attack" attacker diff apply "로그아웃 이후 탈취 토큰 재사용"
      ;;

    A7)
      reissue_step "$scenario_id" "$scenario_id.1" "$gt_attack" victim victim apply "피해자 정상 재발급"
      parallel_many_attacker_reuse "$scenario_id" "$gt_attack" 50 "탈취 토큰 50병렬 재사용"
      ;;

    A8)
      parallel_pair_shared_token "$scenario_id" "$gt_attack" victim victim attacker diff completion "victim/attacker 동시 재발급"
      ;;

    *)
      fail "정의되지 않은 시나리오: $scenario_id"
      ;;
  esac
}

main() {
  require_cmd docker
  require_cmd curl
  require_cmd jq

  [[ -f "$MATRIX_FILE" ]] || fail "매트릭스 파일 없음: $MATRIX_FILE"
  [[ -f "$COMPOSE_FILE" ]] || fail "compose 파일 없음: $COMPOSE_FILE"
  [[ -f "$ENV_FILE" ]] || fail "env 파일 없음: $ENV_FILE"

  # shellcheck disable=SC1090
  set -a; source "$ENV_FILE"; set +a

  API_BASE="${API_BASE:-http://localhost:${APP_PORT:-8080}}"
  MATRIX_EMAIL="${MATRIX_EMAIL:-matrix.user@syncly.local}"
  MATRIX_PASSWORD="${MATRIX_PASSWORD:-Aa!12345}"
  MATRIX_PASSWORD_BCRYPT="${MATRIX_PASSWORD_BCRYPT:-$DEFAULT_BCRYPT}"

  mkdir -p "$OUTPUT_DIR"
  : > "$RESULT_FILE"

  if [[ "$SKIP_STACK_UP" != "1" ]]; then
    log "docker compose 스택 시작"
    compose up -d
  fi

  trap cleanup EXIT

  wait_for_mysql
  wait_for_api
  seed_member

  local scenarios=()
  if [[ -n "${SCENARIOS:-}" ]]; then
    IFS=',' read -r -a scenarios <<< "$SCENARIOS"
  else
    mapfile -t scenarios < <(jq -r '.[].id' "$MATRIX_FILE")
  fi

  local sid
  for sid in "${scenarios[@]}"; do
    run_scenario "$sid"
  done

  jq -nc \
    --arg run_id "$RUN_ID" \
    --arg api_base "$API_BASE" \
    --arg matrix_file "$MATRIX_FILE" \
    --arg compose_file "$COMPOSE_FILE" \
    --arg env_file "$ENV_FILE" \
    --arg result_file "$RESULT_FILE" \
    --arg trusted_origin "$TRUSTED_ORIGIN" \
    --arg victim_ua "$VICTIM_UA" \
    --arg attacker_ua "$ATTACKER_UA" \
    --arg started_at "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
    '{run_id:$run_id,api_base:$api_base,matrix_file:$matrix_file,compose_file:$compose_file,env_file:$env_file,result_file:$result_file,trusted_origin:$trusted_origin,victim_ua:$victim_ua,attacker_ua:$attacker_ua,generated_at:$started_at}' \
    > "$META_FILE"

  "$ROOT_DIR/scripts/security/score-jwt-attack-matrix.sh" "$RESULT_FILE" "$MATRIX_FILE" > "$SUMMARY_FILE"

  log "실행 완료"
  log "결과 파일: $RESULT_FILE"
  log "집계 파일: $SUMMARY_FILE"
  log "메타 파일: $META_FILE"
}

main "$@"
