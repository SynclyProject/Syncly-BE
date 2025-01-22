// static/scripts/community/write.js

async function writeSubmit() {

    // 폼 데이터 수집
    const subject = document.getElementById("subject").value;
    const content = document.getElementById("content").value;
    const category = "BASIC"; // 카테고리 기본값 설정
    const fileInput = document.getElementById("file");
    const infoCheckbox = document.getElementById("info");

    // 개인정보 동의 여부 확인
    if (!infoCheckbox.checked) {
        alert("개인정보 수집 및 이용에 동의해 주세요.");
        return;
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
        subject: subject,
        content: content,
        category: category
    };

    // FormData 생성
    const formData = new FormData();
    formData.append("to", new Blob([JSON.stringify(requestDTO)], { type: "application/json" }));

    if (fileInput.files.length > 0) {
        formData.append("file", fileInput.files[0]);
    }

    refresh();
    try {
        const response = await fetch("http://localhost:8080/api/community/write", {
            method: "POST",
            headers: {
                Authorization: localStorage.getItem("Authorization") // 토큰 추가
            },
            body: formData
        });

        const result = await response.json();

        if (response.ok) {
            console.log("게시물 작성 성공:", result);

            const post = result.data;
            if (post && post.boardId) {
                // 성공 모달 표시
                const modalObj = new ModalObj();
                modalObj.createModal("성공", "게시물이 성공적으로 작성되었습니다.", [{
                            title: "확인",
                            onclick: () => {
                                window.location.href = `/community/detail/${post.boardId}`;}
                        }]);
            } else {
                const modalObj = new ModalObj();
                modalObj.createModal("경고", "게시물 생성은 성공했으나 리디렉션할 수 없습니다.", [{
                            title: "확인",
                            onclick: () => {
                                modalObj.delete();}
                        }]);
            }
        } else {
            console.error("게시물 작성 실패:", result.message);
            const modalObj = new ModalObj();
            modalObj.createModal("실패", "게시물 작성에 실패했습니다.", [{
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