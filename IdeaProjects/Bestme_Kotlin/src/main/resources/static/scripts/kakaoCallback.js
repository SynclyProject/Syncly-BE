document.getElementById('goHomeBtn').addEventListener('click', async function () {
    // URL에서 token 파라미터를 가져옵니다.
    const urlParams = new URLSearchParams(window.location.search);
    const token = urlParams.get("token");

    if (token) {
        // 로컬 스토리지에 토큰을 저장합니다.
        localStorage.setItem("Authorization", token);
        console.log("토큰 저장 완료");

        window.location.href = "/";
    } else {
        console.error("토큰이 없습니다.");
    }
});
