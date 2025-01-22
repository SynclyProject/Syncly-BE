document.addEventListener("DOMContentLoaded", function() {
    event.preventDefault();

    const inputs = document.querySelectorAll('input[type="text"]');

    inputs.forEach(input => {
        // input에 focus되었을 때
        input.addEventListener("focus", function() {
            if (input.value === "") {
                input.value = "#";  // 입력 값이 비어 있으면 #을 추가
            }
        });

        // input에서 blur되었을 때
        input.addEventListener("blur", function() {
            if (input.value === "#") {
                input.value = "";  // #만 입력되어 있으면 지움
            }
        });

        // input에 keyup 이벤트를 추가하여 값이 # 뒤에 입력되는지 확인
        input.addEventListener("input", function() {
            if (input.value[0] !== "#") {
                input.value = "#" + input.value.replace(/^#/, "");  // #이 없다면 자동으로 추가
            }
        });
    });

    // 버튼에 이벤트 추가
    document.getElementById('test-btn').addEventListener('click', checkLoginAndRedirect);

});


function checkLoginAndRedirect(event) {
    // 폼이 제출되지 않도록 막음
    event.preventDefault();

    // 로그인 상태 체크
    if (!checkLogin()) {
        alert("로그인해주세요.");
        location.href = '/login';  // 로그인 페이지로 이동
        return;
    }

    // 폼 검증
    const form = document.querySelector("form");
    if (form.checkValidity()) {
        // 입력값이 모두 올바르면
        // Input 값 가져오기
        const lightestSkinColor = document.getElementById('lightestSkinColor').value;
        const darkestSkinColor = document.getElementById('darkestSkinColor').value;
        const lipColor = document.getElementById('lipColor').value;
        const hairColor = document.getElementById('hairColor').value;
        const pupilColor = document.getElementById('pupilColor').value;
        const irisColor = document.getElementById('irisColor').value;

        // 값들을 query string으로 변환
        const params = new URLSearchParams({
            lightestSkinColor,
            darkestSkinColor,
            lipColor,
            hairColor,
            pupilColor,
            irisColor
        });

        location.href = `/personal/result?${params.toString()}`;
    } else {
        // 입력값이 유효하지 않으면 경고 메시지
        alert("모든 입력값을 올바르게 입력해주세요.");
    }
}
