// static/scripts/community/detail.js
document.addEventListener("DOMContentLoaded", function () {
    console.log("커뮤니티 디테일 페이지 스크립트")

    const main = document.getElementById("main");
    const boardId = main.getAttribute("data-board-id");

    // 게시물 목록 API 호출
    fetch(`http://localhost:8080/api/community/detail/${boardId}`, {
        method: 'GET',
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(data => {
            console.log(data);

            // 게시물 데이터 추출
            const post = data.data;
            // html 각 요소 준비
            const subject = document.getElementById("subject");
            const createdAt = document.getElementById("createdAt");
            const nickname = document.getElementById("nickname");
            const view = document.getElementById("view");
            // const category = document.getElementById("category");
            const content = document.getElementById("content");
            const image = document.getElementById("image-fIle");

            // 제목 삽입
            subject.innerHTML = post.subject;
            // 생성 날짜 삽입
            const createdAtDate = new Date(post.createdAt);
            const formattedCreatedAt = `${createdAtDate.getFullYear()}-${(createdAtDate.getMonth() + 1)
                .toString().padStart(2, '0')}-${createdAtDate.getDate().toString().padStart(2, '0')}`;
            createdAt.innerHTML = formattedCreatedAt;
            // 작성자 삽입
            nickname.innerHTML = post.nickname;
            // 조회 삽입
            view.innerHTML = post.view;
            // 카테고리 삽입
            // category.innerHTML = post.category;
            // 내용 삽입
            content.innerHTML = post.content;

            // 이미지 추가
            if (post.imagename) {
                // 이미지가 있으면 이미지를 비동기로 가져옴
                getImageFile(post.imagename).then(imageUrl => {
                    if (imageUrl) {
                        // 이미지 URL 또는 Blob을 img 태그의 src로 설정
                        image.innerHTML = `<img src="${imageUrl}" alt="게시물 이미지" />`;
                    } else {
                        image.innerHTML = "이미지 로드 실패.";
                    }
                });
            }

            // 버튼(수정, 삭제, 쓰기) 동적 표기 기능
            const accessToken = localStorage.getItem('Authorization');
            const buttonBody = document.getElementById("buttons");
            const apiUserId = post.userId;
            let buttonHTML = "";

            if (accessToken) {
                // getLoginInfo를 비동기적으로 처리
                getLoginInfo().then(userInfo => {
                    const tokenUserId = userInfo.id;
                    console.log("토큰 내 유저 id: "+ tokenUserId);
                    console.log("api 응답 내 유저 id: " + apiUserId);

                    if (apiUserId === tokenUserId && apiUserId !== 0 ) {
                        buttonHTML = `<button type="button" class="common" onclick="location.href='/community/modify/${boardId}'">수정</button>
<!--                            <button type="button" class="common" onclick="location.href='/community/delete/${boardId}'">삭제</button>-->
                            <button type="button" class="common" onclick="openDeleteModal('${post.subject}', '${post.nickname}', ${boardId})">삭제</button>
                            <button type="button" class="green" onclick="location.href='/community/write'">글 작성</button>
                `;
                    } else {
                        buttonHTML = `
                    <button type="button" class="green" onclick="location.href='/community/write'">글 작성</button>
                `;
                    }

                    buttonBody.innerHTML = buttonHTML;
                }).catch(error => {
                    console.error("로그인 정보 가져오기 실패:", error);
                });
            }

        })
        .catch((error) => {
            console.error("Error fetching result data:", error);
            alert("게시물 데이터를 불러오는 데 실패했습니다. 나중에 다시 시도해주세요.")
            location.href = "/community";
            // const errorMessage = document.createElement("p");
            // errorMessage.textContent = "게시물 데이터를 불러오는 데 실패했습니다. 나중에 다시 시도해주세요.";
            // main.appendChild(errorMessage);
        });
});


// 삭제 버튼 모달
function openDeleteModal(title, nickname, boardId) {
    const modalContent = `
        <div style="width: 100%; padding: 20px 0; font-size: 1.1rem;">
            <div style="padding: 10px 0;">다음 게시물을 삭제하시겠습니까?</div>
        
            <div style="padding: 10px 0;">
                <strong>제목:</strong> ${title}
            </div>
        
            <div style="padding: 10px 0;">
                <strong>작성자:</strong> ${nickname}
            </div>
        
            <div style="padding: 10px 0; display: flex; align-items: center;">
                <strong style="margin-right: 10px;">비밀번호:</strong>
                <input type="password" id="password" placeholder="비밀번호를 입력하세요" style="flex-grow: 1; padding: 0.5rem; font-size: 1rem;" />
            </div>
        </div>
    `;

    const modalObj = new ModalObj();
    const buttonOptions = [
        {
            title: '삭제',
            onclick: () => {
                const password = document.getElementById('password').value.trim();
                if (!password) {
                    alert('비밀번호를 입력해주세요.');
                    return;
                }
                deletePost(boardId, password);
                modalObj.delete(); // 모달 닫기
            }
        },
        {
            title: '취소',
            onclick: () => modalObj.delete() // 모달 닫기
        }
    ];

    modalObj.createModal('게시물 삭제', modalContent, buttonOptions);
}

