const kakaoLoginBtn = document.getElementById('kakao-login-btn');

if (kakaoLoginBtn) {
    const clientId = kakaoLoginBtn.getAttribute('data-client-id');
    const redirectUri = kakaoLoginBtn.getAttribute('data-redirect-uri');

    const kakaoLoginUrl = `https://kauth.kakao.com/oauth/authorize?client_id=${clientId}&redirect_uri=${redirectUri}&response_type=code&scope=account_email`;

    kakaoLoginBtn.addEventListener('click', () => {
        console.log("[JS] : kakaoLoginUrl로 이동");
        window.location.href = kakaoLoginUrl;
    });
} else {
    console.error('kakao-login-btn 없음');
}
