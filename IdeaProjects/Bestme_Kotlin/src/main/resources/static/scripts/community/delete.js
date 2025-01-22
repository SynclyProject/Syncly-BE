// static/scripts/community/delete.js

async function deletePost(boardId, password) {
    refresh();
    try {
        const requestDTO = { boardId, password };

        const response = await fetch("http://localhost:8080/api/community/delete", {
            method: "DELETE",
            headers: {
                "Content-Type": "application/json",
                Authorization: localStorage.getItem("Authorization")
            },
            body: JSON.stringify(requestDTO)
        });

        const result = await response.json();

        if (response.ok) {
            console.log("게시물 삭제 성공:", result.data);
            const { nickname, subject } = result.data;

            // 성공 모달 표시
            const modalObj = new ModalObj();
            modalObj.createModal("성공", "게시물이 성공적으로 삭제되었습니다.", [{
                title: "확인",
                onclick: () => {
                    window.location.href = `/community`;}
            }]);

        } else {
            console.error("게시물 작성 실패:", result.message);
            const modalObj = new ModalObj();
            modalObj.createModal("실패", "게시물 삭제에 실패했습니다.", [{
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