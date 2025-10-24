# feat/#125: 노트 기능 API 및 WebSocket 가이드

> 🎯 **프론트엔드 개발자용** 통합 가이드입니다. 백엔드가 제공하는 API와 WebSocket 프로토콜을 이해하고 구현할 수 있도록 작성되었습니다.

---

## 📋 목차
1. [WebSocket 연결](#1-websocket-연결)
2. [실시간 노트 편집](#2-실시간-노트-편집)
3. [에러 처리](#3-에러-처리)
4. [이미지 업로드](#4-이미지-업로드)
5. [예제 코드](#5-예제-코드)

---

## 1. WebSocket 연결

### 1.1 연결 엔드포인트
```
ws://localhost:8080/ws/note
또는
wss://your-domain.com/ws/note (HTTPS)
```

### 1.2 연결 방법 (STOMP)

#### Step 1: 라이브러리 설치
```bash
npm install stompjs
```

#### Step 2: 연결 코드
```javascript
import { Client } from '@stomp/stompjs';

const client = new Client({
  brokerURL: 'ws://localhost:8080/ws/note',
  connectHeaders: {
    'Authorization': `Bearer ${jwtToken}`  // ⭐ 필수!
  },
  onConnect: () => {
    console.log('WebSocket 연결 성공');
  },
  onDisconnect: () => {
    console.log('WebSocket 연결 해제');
  },
  onStompError: (error) => {
    console.error('STOMP 에러:', error);
  }
});

client.activate();
```

### 1.3 JWT 토큰
```
Authorization 헤더 형식:
  Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

⚠️ **토큰이 없으면 연결이 거부됩니다!**

---

## 2. 실시간 노트 편집

### 2.1 메시지 구조

#### 발행 (Publish - 클라이언트 → 서버)
```javascript
client.publish({
  destination: '/app/notes/{noteId}/edit',
  body: JSON.stringify({
    operation: {
      type: 'insert',      // 'insert' | 'delete'
      position: 5,         // 커서 위치
      content: 'hello',    // 삽입할 텍스트
      revision: 10         // 현재 리비전
    }
  })
});
```

#### 구독 (Subscribe - 서버 → 클라이언트)
```javascript
client.subscribe('/topic/notes/{noteId}/edit', (message) => {
  const data = JSON.parse(message.body);
  console.log('원격 변경:', data.operation);
  // UI 업데이트
});
```

### 2.2 Operation 타입

| Type | 설명 | 필수 필드 |
|------|------|---------|
| `insert` | 텍스트 삽입 | position, content, revision |
| `delete` | 텍스트 삭제 | position, length, revision |

### 2.3 예제: 텍스트 삽입
```javascript
// 5번 위치에 "hello" 삽입
client.publish({
  destination: '/app/notes/123/edit',
  body: JSON.stringify({
    operation: {
      type: 'insert',
      position: 5,
      content: 'hello',
      revision: 10
    }
  })
});
```

### 2.4 예제: 텍스트 삭제
```javascript
// 5번 위치부터 3글자 삭제
client.publish({
  destination: '/app/notes/123/edit',
  body: JSON.stringify({
    operation: {
      type: 'delete',
      position: 5,
      length: 3,
      revision: 10
    }
  })
});
```

---

## 3. 에러 처리

### 3.1 에러 응답 구조
```json
{
  "code": "Note409_2",
  "message": "동시 편집 충돌이 발생했습니다",
  "data": {
    "code": "Note409_2",
    "timestamp": "2025-10-25T10:30:45",
    "path": "/ws/notes/123/edit",
    "action": "RELOAD"
  }
}
```

### 3.2 에러 구독
```javascript
client.subscribe('/user/queue/errors', (message) => {
  const error = JSON.parse(message.body);

  switch(error.data.action) {
    case 'RELOAD':
      // 페이지 새로고침 (동시 편집 충돌)
      location.reload();
      break;

    case 'RETRY':
      // 재시도 (Redis 오류 등)
      setTimeout(() => {
        retryLastOperation();
      }, 1000);
      break;

    case 'RECONNECT':
      // WebSocket 재연결
      client.deactivate().then(() => {
        setTimeout(() => client.activate(), 2000);
      });
      break;

    case 'REDIRECT_LOGIN':
      // 로그인 페이지로 이동
      window.location.href = '/login';
      break;

    default:
      console.error('알 수 없는 에러:', error.message);
  }
});
```

### 3.3 일반적인 에러 코드

| Code | 설명 | Action |
|------|------|--------|
| `Note404` | 노트를 찾을 수 없음 | 페이지 이동 |
| `Note403` | 접근 권한 없음 | REDIRECT_LOGIN |
| `Note409_1` | Revision 불일치 | RELOAD |
| `Note409_2` | 동시 편집 충돌 | RELOAD |
| `Note500_3` | Redis 연결 오류 | RETRY |

---

## 4. 이미지 업로드

### 4.1 Presigned URL 요청
```javascript
const response = await fetch('/api/notes/123/images/presigned-url', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${jwtToken}`
  },
  body: JSON.stringify({
    noteId: 123,
    fileName: 'screenshot.png',
    mimeType: 'image/png'  // FileMimeType enum 값
  })
});

const { data } = await response.json();
const presignedUrl = data.presignedUrl;
```

### 4.2 S3 직접 업로드
```javascript
// 1단계: Presigned URL 획득
const presignedUrl = await getPresignedUrl(noteId, fileName, mimeType);

// 2단계: S3에 직접 업로드
const uploadResponse = await fetch(presignedUrl, {
  method: 'PUT',
  headers: {
    'Content-Type': mimeType
  },
  body: file  // File 객체
});

if (uploadResponse.ok) {
  console.log('업로드 성공');
  // 3단계: 에디터에 이미지 삽입
  const imageUrl = presignedUrl.split('?')[0];  // 쿼리 제거
  insertImageToNote(imageUrl);
}
```

### 4.3 지원하는 MIME 타입
```
image/jpeg
image/png
image/gif
image/webp
```

---

## 5. 예제 코드

### 5.1 완전한 WebSocket 클라이언트

```javascript
import { Client } from '@stomp/stompjs';

class NoteCollaborativeEditor {
  constructor(noteId, jwtToken) {
    this.noteId = noteId;
    this.jwtToken = jwtToken;
    this.revision = 0;
    this.client = null;
    this.editQueue = [];  // 전송 대기 중인 작업
  }

  // 연결 초기화
  connect() {
    this.client = new Client({
      brokerURL: 'ws://localhost:8080/ws/note',
      connectHeaders: {
        'Authorization': `Bearer ${this.jwtToken}`
      },
      onConnect: () => this.onConnected(),
      onDisconnect: () => this.onDisconnected(),
      onStompError: (error) => this.onError(error)
    });

    this.client.activate();
  }

  onConnected() {
    console.log('✅ WebSocket 연결 성공');

    // 서버에서 변경사항 수신
    this.client.subscribe(
      `/topic/notes/${this.noteId}/edit`,
      (message) => this.handleRemoteEdit(message)
    );

    // 에러 메시지 수신
    this.client.subscribe('/user/queue/errors', (message) => {
      this.handleError(JSON.parse(message.body));
    });

    // 대기 중인 작업 전송
    this.flushEditQueue();
  }

  onDisconnected() {
    console.log('❌ WebSocket 연결 해제');
  }

  // 로컬 변경사항 전송
  sendEdit(type, position, content, length = null) {
    const operation = {
      type,           // 'insert' | 'delete'
      position,
      revision: this.revision
    };

    if (type === 'insert') {
      operation.content = content;
    } else if (type === 'delete') {
      operation.length = length;
    }

    if (this.client.connected) {
      this.client.publish({
        destination: `/app/notes/${this.noteId}/edit`,
        body: JSON.stringify({ operation })
      });
    } else {
      // 연결 대기 중이면 큐에 저장
      this.editQueue.push(operation);
    }
  }

  // 원격 변경사항 처리
  handleRemoteEdit(message) {
    const data = JSON.parse(message.body);
    const operation = data.operation;

    console.log('📩 원격 편집 수신:', operation);

    // 로컬 에디터에 적용
    this.applyOperation(operation);

    // Revision 업데이트
    if (operation.revision !== undefined) {
      this.revision = operation.revision + 1;
    }
  }

  // Operation 적용 (OT 변환 로직은 별도로 구현)
  applyOperation(operation) {
    const editor = document.getElementById('note-editor');
    const text = editor.value;

    if (operation.type === 'insert') {
      const newText =
        text.slice(0, operation.position) +
        operation.content +
        text.slice(operation.position);
      editor.value = newText;
    } else if (operation.type === 'delete') {
      const newText =
        text.slice(0, operation.position) +
        text.slice(operation.position + operation.length);
      editor.value = newText;
    }

    // 이벤트 발생
    editor.dispatchEvent(new Event('change'));
  }

  // 에러 처리
  handleError(error) {
    const action = error.data?.action || 'ERROR';

    switch (action) {
      case 'RELOAD':
        alert('다른 사용자에 의해 노트가 변경되었습니다. 페이지를 새로고침합니다.');
        location.reload();
        break;

      case 'RETRY':
        console.warn('일시적 오류입니다. 재시도합니다...');
        setTimeout(() => this.flushEditQueue(), 2000);
        break;

      case 'RECONNECT':
        console.warn('WebSocket 재연결 시도 중...');
        this.client.deactivate().then(() => {
          setTimeout(() => this.client.activate(), 2000);
        });
        break;

      case 'REDIRECT_LOGIN':
        window.location.href = '/login';
        break;

      default:
        console.error('에러:', error.message);
    }
  }

  // 대기 중인 작업 일괄 전송
  flushEditQueue() {
    while (this.editQueue.length > 0 && this.client.connected) {
      const operation = this.editQueue.shift();
      this.client.publish({
        destination: `/app/notes/${this.noteId}/edit`,
        body: JSON.stringify({ operation })
      });
    }
  }

  // 연결 종료
  disconnect() {
    this.client?.deactivate();
  }
}

// 사용 예
const editor = new NoteCollaborativeEditor(123, jwtToken);
editor.connect();

document.getElementById('note-editor').addEventListener('input', (e) => {
  const text = e.target.value;
  const position = e.target.selectionStart;

  // 텍스트 삽입 감지
  editor.sendEdit('insert', position, 'a');
});
```

### 5.2 에디터 UI 통합
```html
<div id="note-container">
  <textarea
    id="note-editor"
    placeholder="노트를 작성하세요..."
    rows="20"
    cols="80"
  ></textarea>

  <!-- 접속자 목록 -->
  <div id="online-users">
    <h4>온라인 사용자</h4>
    <ul id="user-list"></ul>
  </div>

  <!-- 에러 메시지 -->
  <div id="error-message" style="display: none; color: red;"></div>
</div>

<script>
  const jwtToken = localStorage.getItem('token');
  const noteId = new URLSearchParams(window.location.search).get('noteId');

  const editor = new NoteCollaborativeEditor(noteId, jwtToken);
  editor.connect();
</script>
```

---

## 6. 체크리스트

### 필수 구현 항목

- [ ] WebSocket 클라이언트 라이브러리 설치 (@stomp/stompjs)
- [ ] JWT 토큰 포함 WebSocket 연결 구현
- [ ] Operation 메시지 발행/구독 구현
- [ ] 에러 처리 구현 (action별 대응)
- [ ] UI에 실시간 변경사항 반영
- [ ] 이미지 업로드 (Presigned URL) 구현
- [ ] 동시 편집 충돌 처리 (page reload)
- [ ] 연결 상태 표시 (온/오프라인)

### 권장 사항

- [ ] 자동 저장 로직 (30초마다 수동 저장)
- [ ] Operation 큐잉 (오프라인 모드 지원)
- [ ] Cursor 위치 공유 (다른 사용자 커서 표시)
- [ ] 사용자 활동 상태 (typing indicator)
- [ ] 변경 이력 (undo/redo)

---

## 7. 문제 해결

### Q: WebSocket 연결이 실패합니다
**A:**
- JWT 토큰이 유효한지 확인하세요
- Authorization 헤더 형식: `Bearer {token}`
- 브라우저 콘솔에서 에러 메시지 확인

### Q: "NOTE_ACCESS_DENIED" 에러가 발생합니다
**A:**
- 해당 노트가 속한 워크스페이스의 멤버인지 확인하세요
- 워크스페이스에 초대되어 있는지 확인하세요

### Q: 다른 사용자의 변경사항이 보이지 않습니다
**A:**
- `/topic/notes/{noteId}/edit` 구독이 제대로 되었는지 확인
- 네트워크 탭에서 메시지 수신 여부 확인
- 서버 로그에서 broadcast 여부 확인

### Q: 이미지 업로드가 실패합니다
**A:**
- MIME 타입이 지원되는 형식인지 확인
- CORS 설정 확인
- S3 버킷 권한 확인

---

## 8. API 명세

### 이미지 업로드 Presigned URL
```
POST /api/notes/{noteId}/images/presigned-url
Content-Type: application/json
Authorization: Bearer {token}

요청:
{
  "noteId": 123,
  "fileName": "screenshot.png",
  "mimeType": "image/png"
}

응답:
{
  "code": "200",
  "message": "성공",
  "data": {
    "presignedUrl": "https://s3.amazonaws.com/bucket/notes/123/uuid.png?..."
  }
}
```

---

## 📞 문제 발생 시

> 백엔드 팀에 다음 정보와 함께 이슈 생성하세요:
> - 에러 메시지
> - 브라우저 콘솔 로그
> - 서버 로그 (stacktrace)
> - 재현 방법 (Step by step)

---

**마지막 업데이트**: 2025-10-25
**백엔드 브랜치**: feat/#125
