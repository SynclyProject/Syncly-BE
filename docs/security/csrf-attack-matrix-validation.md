## 근본 목적
Cross-site 구조에서 SameSite 쿠키 단독 방어가 불충분한 전제를 기준으로, 현재 Origin/Referer 기반 CSRF 필터가 공격 요청을 차단하고 정상 요청을 통과시키는지 자동화 실험으로 검증한다.

## 비목적
인증 설계 변경, CSRF 필터 로직 변경, 쿠키 정책 변경은 이번 문서의 범위가 아니다.

## 실험 질문
- 공격 요청(악성 Origin/Referer + 쿠키)을 안정적으로 차단하는가?
- 정상 요청(신뢰 Origin/Referer + 쿠키)을 안정적으로 통과시키는가?
- 헤더 조합 혼합(악성 Origin + 신뢰 Referer)에서 어떤 판정이 발생하는가?

## 검증 키 포인트
- 필터 적용 조건: `/api/auth/reissue`, `/api/auth/logout`, 비안전 메서드, `REFRESH` 쿠키 존재
- 차단 신호: `HTTP 403` + 응답 코드 `CSRF403`
- 집계 지표
  - TP: 공격 차단
  - FN: 공격 미차단
  - FP: 정상 오차단
  - TN: 정상 통과

## 시나리오 매트릭스
원본: `scripts/security/csrf-attack-matrix.json`

- 정상(C): C1~C4
- 공격(A): A1~A6

## 자동화 실행
```bash
cp docker/security/env.csrf-matrix.example docker/security/env.csrf-matrix.local
ENV_FILE=$(pwd)/docker/security/env.csrf-matrix.local \
./scripts/security/run-csrf-attack-matrix.sh
```

## 산출물
- 요청 결과: `/tmp/syncly-csrf-matrix/results-<RUN_ID>.ndjson`
- 집계 결과: `/tmp/syncly-csrf-matrix/summary-<RUN_ID>.json`
- 실행 메타: `/tmp/syncly-csrf-matrix/meta-<RUN_ID>.json`

## 결과 해석 원칙
- request level은 개별 요청 판정 정확도를 평가한다.
- incident level은 시나리오 단위 방어 성공률을 평가한다.
- `악성 Origin + 신뢰 Referer`처럼 혼합 헤더에서 미탐이 발생하면 정책 한계로 기록한다.
- 포트폴리오 문구는 측정값과 한계를 함께 제시한다.

## 이슈/PR 기재 필수 한계
- Origin/Referer 중 하나만 신뢰되어도 통과되므로, 혼합 헤더 시나리오에서 우회 여지가 있는지 실측 결과로 명시한다.
- CORS 허용 목록과 CSRF trusted 목록의 불일치로 정상 요청 차단이 발생할 수 있음을 결과 해석에 포함한다.

## 2026-02-26 1차 실행 결과
- 실행 명령: `./scripts/security/run-csrf-attack-matrix.sh`
- 결과 파일: `/tmp/syncly-csrf-matrix/results-20260226T113929Z.ndjson`
- 집계 파일: `/tmp/syncly-csrf-matrix/summary-20260226T113929Z.json`

요청 단위(request level)에서 두 가지 관점으로 분리해 해석했다.

- `security_block_level` (HTTP 403 차단 기준)
  - TP=6, FN=0, FP=0, TN=3
  - detection_rate=1.0, false_positive_rate=0.0
  - 의미: 측정 대상 공격 6건은 모두 차단됐다.

- `csrf_filter_level` (`CSRF403` 코드 기준)
  - TP=2, FN=4, FP=0, TN=3
  - detection_rate=0.3333, false_positive_rate=0.0
  - 의미: 공격 차단의 상당수는 CSRF 필터 응답(`CSRF403`)이 아니라 다른 계층(주로 CORS 403)에서 발생했다.

시나리오 해석:
- A3/A4는 `CSRF403`로 차단되어 CSRF 필터가 직접 동작했다.
- A1/A2/A5/A6은 403 차단이지만 `CSRF403` 코드가 없어, 실행 관찰상 CORS 단계 차단으로 분류된다.
- C1/C2/C3는 정상 통과(200)로 오차단은 관찰되지 않았다.
- C4(쿠키 없음)는 필터 적용 대상이 아니므로 측정 지표에서 제외하고 동작 확인용 진단 케이스로만 사용했다.
