#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

MATRIX_FILE="${MATRIX_FILE:-$ROOT_DIR/scripts/security/csrf-attack-matrix.json}"
COMPOSE_FILE="${COMPOSE_FILE:-$ROOT_DIR/docker/security/docker-compose.csrf-matrix.yml}"
ENV_FILE="${ENV_FILE:-$ROOT_DIR/docker/security/env.csrf-matrix.example}"
OUTPUT_DIR="${OUTPUT_DIR:-/tmp/syncly-csrf-matrix}"
RUN_ID="${RUN_ID:-$(date -u +%Y%m%dT%H%M%SZ)}"
RESULT_FILE="${RESULT_FILE:-$OUTPUT_DIR/results-$RUN_ID.ndjson}"
SUMMARY_FILE="${SUMMARY_FILE:-$OUTPUT_DIR/summary-$RUN_ID.json}"
META_FILE="${META_FILE:-$OUTPUT_DIR/meta-$RUN_ID.json}"

TRUSTED_ORIGIN="${TRUSTED_ORIGIN:-http://localhost:5173}"
TRUSTED_REFERER="${TRUSTED_REFERER:-http://localhost:5173/matrix}"
MALICIOUS_ORIGIN="${MALICIOUS_ORIGIN:-https://evil.example}"
MALICIOUS_REFERER="${MALICIOUS_REFERER:-https://evil.example/attack}"
LOOKALIKE_ORIGIN="${LOOKALIKE_ORIGIN:-https://syncly-io.com.evil.com}"
USER_AGENT="${USER_AGENT:-CsrfMatrix/1.0}"

SKIP_STACK_UP="${SKIP_STACK_UP:-0}"
KEEP_STACK="${KEEP_STACK:-0}"
DEFAULT_BCRYPT='$2a$10$SHBeLpz3IRNpx6gvHLW6Je6IcJa4p/zdNBrFOGtFK93eZUKov4qO2'

log() {
  printf '[csrf-matrix] %s\n' "$*"
}

fail() {
  printf '[csrf-matrix][error] %s\n' "$*" >&2
  exit 1
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

origin_value() {
  local mode="$1"
  case "$mode" in
    trusted)
      printf '%s' "$TRUSTED_ORIGIN"
      ;;
    malicious)
      printf '%s' "$MALICIOUS_ORIGIN"
      ;;
    lookalike)
      printf '%s' "$LOOKALIKE_ORIGIN"
      ;;
    none)
      printf ''
      ;;
    *)
      fail "지원하지 않는 origin mode: $mode"
      ;;
  esac
}

referer_value() {
  local mode="$1"
  case "$mode" in
    trusted)
      printf '%s' "$TRUSTED_REFERER"
      ;;
    malicious)
      printf '%s' "$MALICIOUS_REFERER"
      ;;
    none)
      printf ''
      ;;
    *)
      fail "지원하지 않는 referer mode: $mode"
      ;;
  esac
}

endpoint_path() {
  local endpoint="$1"
  case "$endpoint" in
    reissue)
      printf '/api/auth/reissue'
      ;;
    logout)
      printf '/api/auth/logout'
      ;;
    *)
      fail "지원하지 않는 endpoint: $endpoint"
      ;;
  esac
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
  mysql_exec "INSERT INTO members (email, password, name, social_login_provider, is_deleted, created_at, updated_at) VALUES ('${email_esc}', '${hash_esc}', 'csrf-matrix-user', 'LOCAL', 0, NOW(), NOW());"
}

login_refresh() {
  local header_file body_file status refresh
  header_file="$(mktemp)"
  body_file="$(mktemp)"

  curl -sS -D "$header_file" -o "$body_file" -X POST "$API_BASE/api/auth/login" \
    -H "Content-Type: application/json" \
    -H "User-Agent: $USER_AGENT" \
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

  rm -f "$header_file" "$body_file"
  printf '%s' "$refresh"
}

should_run_scenario() {
  local scenario_id="$1"
  if [[ -z "${SCENARIOS:-}" ]]; then
    return 0
  fi
  local token
  IFS=',' read -r -a tokens <<< "$SCENARIOS"
  for token in "${tokens[@]}"; do
    if [[ "$token" == "$scenario_id" ]]; then
      return 0
    fi
  done
  return 1
}

write_result() {
  local scenario_id="$1"
  local title="$2"
  local group="$3"
  local gt_attack="$4"
  local endpoint="$5"
  local origin_mode="$6"
  local referer_mode="$7"
  local send_cookie="$8"
  local measurable="${9}"
  local status="${10}"
  local body_code="${11}"
  local blocked="${12}"
  local csrf_detected="${13}"

  jq -nc \
    --arg scenario_id "$scenario_id" \
    --arg title "$title" \
    --arg group "$group" \
    --arg endpoint "$endpoint" \
    --arg origin_mode "$origin_mode" \
    --arg referer_mode "$referer_mode" \
    --arg status "$status" \
    --arg body_code "$body_code" \
    --arg timestamp "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
    --argjson gt_attack "$gt_attack" \
    --argjson send_cookie "$send_cookie" \
    --argjson measurable "$measurable" \
    --argjson blocked "$blocked" \
    --argjson csrf_detected "$csrf_detected" \
    '{scenario_id:$scenario_id,title:$title,group:$group,gt_attack:$gt_attack,endpoint:$endpoint,origin_mode:$origin_mode,referer_mode:$referer_mode,send_cookie:$send_cookie,measurable:$measurable,status:$status,body_code:$body_code,detected:$blocked,blocked:$blocked,csrf_detected:$csrf_detected,timestamp:$timestamp}' \
    >> "$RESULT_FILE"
}

run_scenario() {
  local scenario_json="$1"

  local scenario_id title group gt_attack endpoint origin_mode referer_mode send_cookie
  local measurable
  scenario_id="$(jq -r '.id' <<< "$scenario_json")"
  title="$(jq -r '.title' <<< "$scenario_json")"
  group="$(jq -r '.group' <<< "$scenario_json")"
  gt_attack="$(jq -r '.gt_attack' <<< "$scenario_json")"
  endpoint="$(jq -r '.endpoint' <<< "$scenario_json")"
  origin_mode="$(jq -r '.origin' <<< "$scenario_json")"
  referer_mode="$(jq -r '.referer' <<< "$scenario_json")"
  send_cookie="$(jq -r '.send_cookie' <<< "$scenario_json")"
  measurable="$(jq -r 'if has("measurable") then .measurable else true end' <<< "$scenario_json")"

  if ! should_run_scenario "$scenario_id"; then
    return
  fi

  log "시나리오 시작: $scenario_id ($title)"

  local path origin referer refresh header_file body_file status body_code blocked csrf_detected
  path="$(endpoint_path "$endpoint")"
  origin="$(origin_value "$origin_mode")"
  referer="$(referer_value "$referer_mode")"

  refresh=""
  if [[ "$send_cookie" == "true" ]]; then
    refresh="$(login_refresh)"
  fi

  header_file="$(mktemp)"
  body_file="$(mktemp)"

  local -a args
  args=( -sS -D "$header_file" -o "$body_file" -X POST "$API_BASE$path" -H "User-Agent: $USER_AGENT" )

  if [[ -n "$origin" ]]; then
    args+=( -H "Origin: $origin" )
  fi
  if [[ -n "$referer" ]]; then
    args+=( -H "Referer: $referer" )
  fi
  if [[ "$send_cookie" == "true" ]]; then
    args+=( -H "Cookie: REFRESH=$refresh" )
  fi

  curl "${args[@]}" >/dev/null

  status="$(extract_http_status "$header_file")"
  body_code="$(json_code "$body_file")"

  blocked=false
  csrf_detected=false
  if [[ "$status" == "403" ]]; then
    blocked=true
  fi
  if [[ "$status" == "403" && "$body_code" == "CSRF403" ]]; then
    csrf_detected=true
  fi

  write_result "$scenario_id" "$title" "$group" "$gt_attack" "$endpoint" "$origin_mode" "$referer_mode" "$send_cookie" "$measurable" "$status" "$body_code" "$blocked" "$csrf_detected"

  rm -f "$header_file" "$body_file"
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

  API_BASE="${API_BASE:-http://localhost:${APP_PORT:-8081}}"
  MATRIX_EMAIL="${MATRIX_EMAIL:-csrf.matrix@syncly.local}"
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

  while IFS= read -r scenario_json; do
    [[ -z "$scenario_json" ]] && continue
    run_scenario "$scenario_json"
  done < <(jq -c '.[]' "$MATRIX_FILE")

  jq -nc \
    --arg run_id "$RUN_ID" \
    --arg api_base "$API_BASE" \
    --arg matrix_file "$MATRIX_FILE" \
    --arg compose_file "$COMPOSE_FILE" \
    --arg env_file "$ENV_FILE" \
    --arg result_file "$RESULT_FILE" \
    --arg generated_at "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
    '{run_id:$run_id,api_base:$api_base,matrix_file:$matrix_file,compose_file:$compose_file,env_file:$env_file,result_file:$result_file,generated_at:$generated_at}' > "$META_FILE"

  "$ROOT_DIR/scripts/security/score-csrf-attack-matrix.sh" "$RESULT_FILE" "$MATRIX_FILE" > "$SUMMARY_FILE"

  log "실행 완료"
  log "결과 파일: $RESULT_FILE"
  log "집계 파일: $SUMMARY_FILE"
  log "메타 파일: $META_FILE"
}

main "$@"
