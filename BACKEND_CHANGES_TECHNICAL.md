# feat/#125: 노트 기능 + 실시간 협업 편집 기술 명세서

## 개요
이 브랜치는 **노트(Note) 기능**과 **실시간 협업 편집(Operational Transformation 기반)**을 지원하는 백엔드 인프라를 구현합니다.

---

## 1. 노트 도메인 추가 (Note Domain)

### 1.1 새로운 엔드포인트
- `POST /api/notes/{noteId}/images/presigned-url` - 노트 이미지 업로드용 Presigned URL 생성

### 1.2 WebSocket 엔드포인트
- `WS /ws/note` - 노트 실시간 협업 편집용 WebSocket 연결
  - 메시지 형식: STOMP 프로토콜
  - 토픽: `/topic/notes/{noteId}/...` (구독)
  - 전송: `/app/notes/{noteId}/...` (발행)

---

## 2. 보안 및 인증 강화

### 2.1 SecurityConfig 변경사항
```java
// 기존 WebSocket 엔드포인트 (/ws-stomp)에 추가로 노트 WebSocket 보호
"/ws/note",          // 인증 불필요 (WebSocketInterceptor에서 JWT 검증)
"/ws/note/**"
```

**특징:**
- 노트 WebSocket 연결 시 JWT 토큰이 없으면 차단
- 노트 구독/발행 시 워크스페이스 멤버십 확인

### 2.2 StompHandler 권한 검증
```
CONNECT: JWT 토큰 검증
  ↓
SUBSCRIBE/SEND:
  - 노트 ID 추출 (destination 파싱)
  - 노트 존재 여부 확인
  - 사용자가 해당 워크스페이스 멤버인지 확인
  - 위반 시: NoteException(NOTE_ACCESS_DENIED) 발생
```

**정규식 패턴:**
```
^/(app|topic|queue)/notes/(\d+)/
예: /app/notes/123/edit, /topic/notes/123/cursor
```

---

## 3. 예외 처리 강화

### 3.1 GlobalExceptionHandler 추가
| 예외 타입 | HTTP 상태 | 처리 내용 |
|---------|---------|---------|
| `NoteException` | 동적 | 노트 관련 비즈니스 로직 오류 |
| `RedisConnectionFailureException` | 503 | Redis 서버 연결 실패 |
| `OptimisticLockingFailureException` | 409 | 동시 편집 충돌 (revision 불일치) |
| `NoHandlerFoundException` | 404 | 존재하지 않는 엔드포인트 |
| `Exception` (일반) | 500 | 예상치 못한 서버 오류 |

**응답 형식:**
```json
{
  "code": "Note409_2",
  "message": "동시 편집 충돌이 발생했습니다",
  "data": {
    "code": "Note409_2",
    "timestamp": "2025-10-25T10:30:45",
    "path": "/api/notes/123",
    "action": "RELOAD"  // 클라이언트 액션 지정
  }
}
```

### 3.2 WebSocketExceptionHandler 강화
```
NoteException
  ↓
determineClientAction(errorCode)
  ↓
클라이언트에게 /queue/errors로 메시지 전송
  ↓
클라이언트는 action에 따라:
  - RELOAD: 페이지 새로고침
  - RETRY: 작업 재시도
  - RECONNECT: WebSocket 재연결
  - REDIRECT_LOGIN: 로그인 페이지로 이동
```

---

## 4. WebSocket 세션 관리

### 4.1 WebSocketEventListener 개선

**CONNECT 시:**
```
Redis:
  SET WS:SESSIONS:{sessionId} → userId
  SADD WS:ONLINE_USERS → userId
```

**DISCONNECT 시:**
```
1️⃣ 일반 WebSocket 세션 정리
   DEL WS:SESSIONS:{sessionId}
   SREM WS:ONLINE_USERS userId

2️⃣ 노트 WebSocket 세션 정리 (있는 경우)
   noteSessionData = GET WS:NOTE_SESSIONS:{sessionId}
   format: "{noteId}:{workspaceMemberId}"
   ↓
   noteRedisService.removeUser(noteId, workspaceMemberId)
   noteRedisService.removeCursor(noteId, workspaceMemberId)
   DEL WS:NOTE_SESSIONS:{sessionId}
```

### 4.2 Redis 키 추가
```
WS:NOTE_SESSIONS:{sessionId} → "{noteId}:{workspaceMemberId}"
```

---

## 5. S3 통합 개선

### 5.1 노트 이미지 업로드 경로
```
기존: uploads/{UUID}.{ext}
신규: notes/{noteId}/{UUID}.{ext}
```

### 5.2 S3Util 새로운 메서드
```java
public boolean objectExists(String objectKey)
```
- headObject를 사용하여 S3 객체 존재 여부 빠르게 확인
- Exception 발생 시 false 반환

---

## 6. 모니터링 및 메트릭

### 6.1 Actuator 설정
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  endpoint:
    health:
      show-details: always
```

**노출 엔드포인트:**
- `GET /actuator/health` - 애플리케이션 상태
- `GET /actuator/metrics` - 메트릭 목록
- `GET /actuator/prometheus` - Prometheus 형식 메트릭

### 6.2 자동 저장 메트릭
```yaml
note.autosave.duration      # 자동 저장 소요 시간
note.autosave.save.time     # 저장 시점
note.manual.save.time       # 수동 저장 시점
```

---

## 7. 로깅 설정

### 7.1 WebSocket 편집 디버깅 로그
```xml
<logger name="com.project.syncly.domain.note.controller.NoteWebSocketController" level="DEBUG"/>
<logger name="com.project.syncly.domain.note.service.OTService" level="DEBUG"/>
<logger name="com.project.syncly.domain.note.engine.OTEngine" level="DEBUG"/>
```

**실시간 편집 추적:**
- 메시지 수신/발송
- OT 변환 과정
- 엔진 상태 변화

---

## 8. 스케줄러 설정

### 8.1 자동 저장 스케줄러
```yaml
scheduler:
  note:
    auto-save:
      fixed-delay: 30000      # 30초 간격
      initial-delay: 30000    # 시작 후 30초 후 첫 실행
```

**동작:**
- 30초마다 자동 저장 작업 실행
- Redis에 저장된 변경사항을 DB에 저장
- Actuator 메트릭으로 모니터링 가능

---

## 9. 의존성 추가

### 9.1 build.gradle
```gradle
// Actuator & Micrometer (모니터링 메트릭용)
implementation 'org.springframework.boot:spring-boot-starter-actuator'
implementation 'io.micrometer:micrometer-core'
```

---

## 10. 클라이언트 대응 액션 명세

WebSocket 에러 응답에 `action` 필드 포함:

| Action | 의미 | 클라이언트 처리 |
|--------|------|----------------|
| `RELOAD` | 페이지 새로고침 필요 | location.reload() |
| `RETRY` | 작업 재시도 가능 | 지수 백오프로 재시도 |
| `RECONNECT` | WebSocket 재연결 필요 | WebSocket 재연결 시도 |
| `REDIRECT_LOGIN` | 로그인 필요 | 로그인 페이지로 이동 |
| `IGNORE` | 무시 가능한 에러 | 아무것도 하지 않음 |

---

## 11. 데이터 흐름

### 11.1 노트 편집 흐름
```
클라이언트 A (에디터)
  ↓ (operation)
WebSocket /app/notes/123/edit
  ↓ (StompHandler: 권한 검증)
NoteWebSocketController
  ↓ (OT 변환)
OTService/OTEngine
  ↓ (Redis 저장)
모든 클라이언트
  ↓ (WebSocket broadcast)
/topic/notes/123/edit
  ↓
클라이언트 B, C (구독자들)
```

### 11.2 이미지 업로드 흐름
```
클라이언트
  ↓ (POST /api/notes/{noteId}/images/presigned-url)
S3Controller
  ↓
S3ServiceImpl (경로: notes/{noteId}/{UUID}.{ext})
  ↓
S3Util (presigned URL 생성)
  ↓
클라이언트 (직접 S3에 업로드)
```

---

## 12. 주의사항

### 12.1 JWT 토큰 형식
```
Authorization: Bearer {token}
```
- 모든 WebSocket CONNECT 요청에 포함 필수
- Header 이름: `Authorization`
- 형식: `Bearer {token}`

### 12.2 에러 응답 호환성
- 기존 CustomException 형식 유지
- 새로운 에러 코드는 NoteErrorCode에 정의
- action 필드는 선택사항 (미지원 클라이언트 대응)

### 12.3 동시 편집 충돌 처리
- `OptimisticLockingFailureException` 발생 시 409 Conflict
- 클라이언트는 RELOAD 액션으로 페이지 새로고침
- 충돌 감지는 노트 엔티티의 @Version 필드로 구현

---

## 13. 마이그레이션 체크리스트

- [ ] 노트 엔티티 DB 마이그레이션 (Flyway/Liquibase)
- [ ] Redis 초기화 (기존 WebSocket 데이터와 충돌 여부 확인)
- [ ] 모니터링 대시보드 (Prometheus/Grafana) 설정
- [ ] 클라이언트에 WebSocket 프로토콜 문서 전달
- [ ] 로드 테스트 (동시 사용자 100+ 동시 편집)

---

## 참고 자료

- JWT 토큰 검증: `JwtProvider.getAuthentication(token)`
- 노트 권한 검증: `WorkspaceMemberRepository.existsByWorkspaceIdAndMemberId()`
- OT 엔진: `OTService`, `OTEngine` (새 클래스)
- Redis 명령: `NoteRedisService` (새 클래스)
