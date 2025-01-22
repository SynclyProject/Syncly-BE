// static/scripts/community/modify.js
document.addEventListener("DOMContentLoaded", function () {
    console.log("커뮤니티 수정 페이지 스크립트")

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
            // const category = document.getElementById("category");
            const content = document.getElementById("content");
            const fileName = document.getElementById("file-name");

            // 제목 삽입
            subject.value = post.subject;
            // 카테고리 삽입
            // category.innerHTML = post.category;
            // 내용 삽입
            content.value = post.content;

            // 파일 정보 추가 UI
            if (post.imagename) {
                // 동적으로 파일 이름과 삭제 버튼을 추가합니다.
                fileName.innerHTML = `<span id="image-name">${post.imagename}</span><button id="delete-button" class="delete-button"> X </button>`;

                // 삭제 버튼 클릭 시 처리
                document.getElementById('delete-button').addEventListener('click', function () {
                    // 파일 정보 UI 삭제
                    fileName.innerHTML = '';

                    // 파일 입력 초기화
                    const fileInput = document.getElementById("file");
                    fileInput.value = ""; // 새 파일 입력 가능

                    // 기존 파일 데이터 제거
                    post.imagename = null;
                });
            }
        })
        .catch((error) => {
            console.error("Error fetching result data:", error);
            const errorMessage = document.createElement("p");
            errorMessage.textContent = "게시물 데이터를 불러오는 데 실패했습니다. 나중에 다시 시도해주세요.";
            main.appendChild(errorMessage);
        });
});

async function modifySubmit() {

    const boardId = main.getAttribute("data-board-id");

    // 폼 데이터 수집
    const subject = document.getElementById("subject").value;
    const content = document.getElementById("content").value;
    const category = "BASIC"; // 카테고리 기본값 설정
    const fileInput = document.getElementById("file");
    let fileName = null;

    // 이미지 파일 이름을 가져오는 안전한 방법
    const fileNameElement = document.getElementById("image-name");
    if (fileNameElement) {
        fileName = fileNameElement.textContent.trim();
    }

    if (subject === '') {
        alert('제목을 입력해 주세요.');
        return;
    }
    if (content === '') {
        alert('내용을 입력해 주세요.');
        return;
    }

    // `RequestWriteDTO` 데이터 준비
    const requestDTO = {
        boardId: boardId,
        subject: subject,
        content: content,
        category: category,
        imagename: fileName // 기존 파일이 있으면 이름 전달
    };

    // FormData 생성
    const formData = new FormData();
    formData.append("to", new Blob([JSON.stringify(requestDTO)], { type: "application/json" }));

    if (fileInput.files.length > 0) {
        formData.append("file", fileInput.files[0]);
    }
    refresh();
    try {
        const response = await fetch("http://localhost:8080/api/community/modify", {
            method: "PUT",
            headers: {
                Authorization: localStorage.getItem("Authorization")
            },
            body: formData
        });

        const result = await response.json();

        if (response.ok) {
            console.log("게시물 수정 성공:", result);

            const post = result.data;
            if (post && post.boardId) {
                // 성공 모달 표시
                const modalObj = new ModalObj();
                modalObj.createModal("성공", "게시물이 성공적으로 수정되었습니다.", [{
                    title: "확인",
                    onclick: () => {
                        window.location.href = `/community/detail/${post.boardId}`;}
                }]);
            } else {
                const modalObj = new ModalObj();
                modalObj.createModal("경고", "게시물 수정은 성공했으나 리디렉션할 수 없습니다.", [{
                    title: "확인",
                    onclick: () => {
                        modalObj.delete();}
                }]);
            }
        } else {
            console.error("게시물 작성 실패:", result.message);
            const modalObj = new ModalObj();
            modalObj.createModal("실패", "게시물 수정에 실패했습니다.", [{
                title: "확인",
                onclick: () => {
                    modalObj.delete();}
            }]);
        }
    } catch (error) {
        console.error("API 호출 중 오류 발생:", error);
        alert("네트워크 오류가 발생했습니다.");
    }
}

// 파일 input 요소와 업로드된 파일 이름을 표시할 input 요소
const fileInput = document.getElementById("file");
const uploadText = document.getElementById("upload_text");

// 파일 선택 시 실행되는 이벤트 리스너
fileInput.addEventListener("change", function() {
    const fileName = fileInput.files[0] ? fileInput.files[0].name : ''; // 파일 이름 가져오기

    // 파일이 선택되었으면 파일 이름을 표시
    if (fileName) {
        uploadText.value = fileName;
    } else {
        uploadText.value = ''; // 파일이 선택되지 않으면 빈 문자열로 초기화
    }
});