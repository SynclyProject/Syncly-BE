## 근본 목적
현행 JWT/Redis JTI 회전 구조를 변경하지 않고, 탈취 탐지 정확도와 정상 사용자 오감지를 정량 검증하여 운영에서 신뢰할 수 있는 근거를 확보한다.

## 비목적
토큰 구조 변경, Redis 키 구조 변경, 재발급/CSRF 로직 리팩터링, 신규 방어 설계 제안은 이번 문서의 범위가 아니다.

## 검증 범위
- 대상 엔드포인트: `/api/auth/login`, `/api/auth/reissue`, `/api/auth/logout`
- 핵심 관찰 키
  - `refresh:current:{userId}:{deviceId}`
  - `CASHED:UA_HASH:{userId}:{deviceId}`
- 검증 관점
  - 동시성
  - 재사용(탈취 토큰)
  - 응답 유실
  - 응답 순서 역전

## 공격 매트릭스
| ID | 분류 | 시나리오 | GT(공격) |
|---|---|---|---|
| N1 | 정상 | 단일 정상 재발급 | false |
| N2 | 정상 | 응답 유실 후 동일 UA 재시도(10초 이내) | false |
| N3 | 정상 | 응답 유실 후 동일 UA 재시도(10초 초과) | false |
| N4 | 정상 | 동일 UA 동시 2요청(정순 반영) | false |
| N5 | 정상 | 동일 UA 동시 2요청(역순 반영) | false |
| A1 | 공격 | 탈취 RT 재사용(상이 UA) | true |
| A2 | 공격 | 탈취 RT 재사용(동일 UA spoof, 10초 이내) | true |
| A3 | 공격 | 탈취 RT 재사용(동일 UA spoof, 10초 초과) | true |
| A4 | 공격 | 탈취 RT 재사용(UA 없음) | true |
| A5 | 공격 | 선점 공격(attacker first) | true |
| A6 | 공격 | 로그아웃 후 탈취 RT 재사용 | true |
| A7 | 공격 | 탈취 RT 50병렬 재사용 | true |
| A8 | 공격 | victim/attacker 동시 재발급(상이 UA) | true |

매트릭스 원본: `scripts/security/jwt-attack-matrix.json`

## Redis 상태 전이 기준
| 전이 | 조건 | Redis 변화 |
|---|---|---|
| T0 | 로그인 성공 | `null -> J0` |
| T1 | `rotateAtomic` 성공 | `Jold -> Jnew`, `CASHED:UA_HASH` 갱신 |
| T2 | mismatch + uaHash 일치 | `setCurrent(newJti)` |
| T3 | mismatch + uaHash 불일치/없음 | `refresh:current` 삭제 + `CASHED:UA_HASH` 삭제 |
| T4 | 로그아웃(유효 refresh) | `refresh:current` 삭제 + `CASHED:UA_HASH` 삭제 |

## TP/FN/FP/TN 산출 규칙
요청 단위 탐지 플래그(`detected`)는 아래를 모두 만족할 때 `true`로 기록한다.
- 응답 코드가 `JWT401_06`
- 요청 후 `refresh:current:{userId}:{deviceId}` 값이 비어 있음

분류 규칙
- TP: `gt_attack=true` and `detected=true`
- FN: `gt_attack=true` and `detected=false`
- FP: `gt_attack=false` and `detected=true`
- TN: `gt_attack=false` and `detected=false`

집계 지표
- 탐지율: `TP / (TP + FN)`
- 오탐률: `FP / (FP + TN)`
- 오탐 건수: `FP`

인시던트 단위 집계는 동일 `scenario_id`에서 한 번이라도 `detected=true`면 탐지로 계산한다.

## 자동화 실행 절차
1. 스택 기동
```bash
cp docker/security/env.jwt-matrix.example docker/security/env.jwt-matrix.local
# 필요 시 값 수정
```

2. 실행
```bash
ENV_FILE=$(pwd)/docker/security/env.jwt-matrix.local \
./scripts/security/run-jwt-attack-matrix.sh
```

3. 산출물
- 요청 단위 결과: `/tmp/syncly-jwt-matrix/results-<RUN_ID>.ndjson`
- 집계 결과: `/tmp/syncly-jwt-matrix/summary-<RUN_ID>.json`
- 실행 메타: `/tmp/syncly-jwt-matrix/meta-<RUN_ID>.json`

4. 재집계
```bash
./scripts/security/score-jwt-attack-matrix.sh /tmp/syncly-jwt-matrix/results-<RUN_ID>.ndjson scripts/security/jwt-attack-matrix.json
```

## 이슈/PR 문서 기재 원칙
이 검증 작업의 이슈 및 PR에는 아래 한계를 반드시 명시한다.
- uaHash 기반 허용은 동일 UA 문자열 재현 상황에서 공격 요청이 정상 재시도로 분류될 가능성을 가진다.
- 동시 재발급과 응답 순서 역전이 결합되면, 클라이언트 최종 쿠키와 서버 `refresh:current`가 불일치할 수 있다.

