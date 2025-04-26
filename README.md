**Syncly-BE**

**main**

CD 적용

**develop**

CI 적용

**feature**

각자 개발

**Release**

Develop 브랜치에서 생성됨 개발이 완료되어 출시를 위해 준비하는 브랜치

**Hotfix**

Production에 배포 된 버전에서 발생한 버그를 수정하는 브랜치 핫픽스 된 부분은 개발 과정에서도 반영이 되야 하므로 Develop 브랜치에도 같이 merge하는 것

---

**브랜치 네이밍 규칙**

feat/#이슈번호

**📋 Commit Message Convention**

| **Gitmoji** | **Tag** | **Description** |
| --- | --- | --- |
| ✨ | `feat:` | 새로운 기능 추가 |
| 🐛 | `fix:` | 버그 수정 |
| 📝 | `docs:` | 문서 추가, 수정, 삭제 |
| ✅ | `test:` | 테스트 코드 추가, 수정, 삭제 |
| 💄 | `style:` | 코드 형식 변경 |
| ♻️ | `refactor:` | 코드 리팩토링 |
| ⚡️ | `perf:` | 성능 개선 |
| 💚 | `ci:` | CI 관련 설정 수정 |
| 🚀 | `chore:` | 기타 변경사항 |
