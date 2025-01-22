HTMLElement.prototype.show = function () {
    this.classList.add('-visible');
    return this;
}

HTMLElement.prototype.hide = function () {
    this.classList.remove('-visible');
    return this;
}

// 브라우저 종료 시 사라지는 sessionStorage 값을 이용하여 localStorage 관리하기.
// 페이지 이동이나 새로고침에는 sessionStorage 값은 사라지지 않음.
// 서버 구동 시간을 받아와서 sessionStorage 에 저장 - 이 후 값을 비교하여 서버 재시작됐는지 확인하여 토큰 삭제.
function checkRestart() {
    fetch('/api/isRestartedServer')
        .then(response => response.text())  // 서버의 재시작 시간 받기
        .then(serverStartTime => {
            const storedTime = sessionStorage.getItem('serverStartTime');
            // 서버 시작 시간이 저장된 시간보다 더 늦으면 localStorage 삭제
            if (serverStartTime !== storedTime) {
                deleteCookie('refresh');
                localStorage.removeItem('Authorization');  // localStorage 삭제
                sessionStorage.setItem('serverStartTime', serverStartTime);  // 최신 서버 시작 시간 저장
            }
            setLoginBox();
        })
        .catch(error => console.error('Error fetching server start time:', error));
}


// 쿠키 다루기
function getCookie(name) {
    let matches = document.cookie.match(new RegExp(
        "(?:^|; )" + name.replace(/([\.$?*|{}\(\)\[\]\\\/\+^])/g, '\\$1') + "=([^;]*)"
    ));
    return matches ? decodeURIComponent(matches[1]) : undefined;
}

function setCookie(name, value, options = {}) {

    options = {
        path: '/', // 경로 지정
        ...options // 아규먼트로 옵션을 넘겨줬을경우 전개연산자로 추가 갱신
    };

    if (options.expires instanceof Date) {
        options.expires = options.expires.toUTCString(); // 생 Date 객체라면 형식에 맞게 인코딩
    }

    let updatedCookie = encodeURIComponent(name) + "=" + encodeURIComponent(value);

    for (let optionKey in options) {
        updatedCookie += "; " + optionKey;
        let optionValue = options[optionKey];
        if (optionValue !== true) { // 밸류가 없다면
            updatedCookie += "=" + optionValue;
        }
    }

    document.cookie = updatedCookie; // 새로 갱신
}


function deleteCookie(name) { // 해당 쿠키 요소만 삭제
    setCookie(name, "", {
        'max-age': -1
    })
}

// Authorization 이 있으면 로그인을 했다. - true
// Authorization 이 없으면 로그인을 하지 않았다. - false
function checkLogin() {
    return !(localStorage.getItem('Authorization') === null)
}

function setLoginBox() {
    const loginBox = document.getElementById('header').querySelector('.header-box.login');
    loginBox.innerHTML = '';
    if (!checkLogin()) {
        const userBox = new DOMParser().parseFromString(`
        <div class="user-box">
            <button class="login" type="button" onclick="location.href = '/login'">Log in</button>
            <button class="sign-up" type="button" onclick="location.href = '/join'">Sign up</button>
        </div>
        `, 'text/html').querySelector('.user-box');
        loginBox.append(userBox);
    } else {
        const userBox = new DOMParser().parseFromString(`
        <div class="user-box">
            <button class="logout" type="button" onclick="logout()">Log out</button>
        </div>
        `, 'text/html').querySelector('.user-box');
        loginBox.append(userBox);
    }
}

function logout() {
    const logoutModal = new ModalObj();
    logoutModal.createModal('알림', '로그아웃 하시겠습니까?', [{
        title: '확인',
        onclick: () => {
            localStorage.removeItem('Authorization');

            fetch('/api/logout')
                .then(response => response.json())
                .then(data => {
                    if (data.success === true) {
                        logoutModal.createModal('알림', '로그아웃에 성공하였습니다.', [{
                            title: '확인',
                            onclick: () => {
                                logoutModal.delete();
                                location.href = '/'
                            }
                        }]);
                    }
                    if (data.success === false) {
                        const resetModal = new ModalObj();
                        resetModal.createSimpleModal('알림', data.message);
                    }
                }).catch(error => console.error('Error:', error));
        }
    }, {
        title: '취소',
        onclick: () => logoutModal.delete()
    }])
}

// 정상적인 형식이면 true 를 반환
function checkBirth(input) {

    const month = parseInt(input.substring(2, 4));
    const day = parseInt(input.substring(4, 6));

    if (month === 0 || month > 12) {
        return false;
    } else if (day === 0 || day > 31) {
        return false;
    }
    return true;
}

// refresh 토큰을 이용하여 새로운 토큰 배치
function refresh() {
    fetch('/api/refresh', {}).then(response => response.json())
        .then(data => {
            if (data.success === true) {
                const Authorization = data.data;
                localStorage.setItem("Authorization", Authorization);
            }
            if (data.success === false) {
                localStorage.removeItem("Authorization");
                const refreshModal = new ModalObj();
                refreshModal.createModal('알림', '로그인 기간이 만료되었습니다.', [{
                    title: '확인',
                    onclick : () => location.href = "/login"
                }])
            }
        })
        .catch(error => console.error('Error:', error));
}

async function getLoginInfo() {
    const token = localStorage.getItem("Authorization");

    try {
        const response = await fetch('/api/loginUser', {
            headers: {
                Authorization: token
            }
        });

        const data = await response.json();  // JSON 응답을 바로 받아옴

        if (data.success === true) {
            return data.data;  // data.data 를 반환
        } else {
            console.log("로그인 유저 정보 호출 실패");
            return null;  // 실패 시 null 반환
        }
    } catch (error) {
        console.error('Error:', error);
        return null;  // 에러 발생 시 null 반환
    }
}

// getLoginInfo 함수의 사용 예시입니다.

// 1.
// getLoginInfo().then(userInfo => console.log(userInfo));
// userInfo 에는 id, nickname, gender, birth 의 값이 있으며
// 로그인 중인 사용자의 정보가 js 의 log 에 나타납니다.

// 2.
// getLoginInfo().then(userInfo => alert(userInfo.nickname));
// 로그인 중인 사용자의 nickname 이 알림창에 표시됩니다.

checkRestart();