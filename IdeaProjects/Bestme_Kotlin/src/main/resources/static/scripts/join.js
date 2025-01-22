const joinForm = document.getElementById('join-form');

const passwordLabel= joinForm.querySelector('.password > .text-label');
const passwordCheckLabel = joinForm.querySelector('.password-check > .text-label');
const validChecks =  joinForm.querySelectorAll('div.valid-check');

if (checkLogin()) {
    alert('이미 로그인했습니다.');
    location.href = "/";
}

passwordLabel.querySelector('button').onclick = () => {
    if (passwordLabel.querySelector('button > i').classList.contains('fa-eye')) {
        passwordLabel.querySelector('input').removeAttribute('type');
        passwordLabel.querySelector('input').setAttribute('type', 'password');
        passwordLabel.querySelector('button > i').classList.remove('fa-eye');
        passwordLabel.querySelector('button > i').classList.add('fa-eye-slash');
    } else {
        passwordLabel.querySelector('input').removeAttribute('type');
        passwordLabel.querySelector('input').setAttribute('type', 'text');
        passwordLabel.querySelector('button > i').classList.remove('fa-eye-slash');
        passwordLabel.querySelector('button > i').classList.add('fa-eye');
    }
}

passwordCheckLabel.querySelector('button').onclick = () => {
    if (passwordCheckLabel.querySelector('button > i').classList.contains('fa-eye')) {
        passwordCheckLabel.querySelector('input').removeAttribute('type');
        passwordCheckLabel.querySelector('input').setAttribute('type', 'password');
        passwordCheckLabel.querySelector('button > i').classList.remove('fa-eye');
        passwordCheckLabel.querySelector('button > i').classList.add('fa-eye-slash');
    } else {
        passwordCheckLabel.querySelector('input').removeAttribute('type');
        passwordCheckLabel.querySelector('input').setAttribute('type', 'text');
        passwordCheckLabel.querySelector('button > i').classList.remove('fa-eye-slash');
        passwordCheckLabel.querySelector('button > i').classList.add('fa-eye');
    }
}

for (const validCheck of validChecks) {
    validCheck.querySelector('input').onblur = () => {
        validCheck.classList.remove('-invalid');
    }
}

joinForm.onsubmit = (e) => {
    e.preventDefault();

    let validCount = 0;

    const eVC = joinForm.querySelector('.email.valid-check');
    const nVC = joinForm.querySelector('.nickname.valid-check');
    const pwVC = joinForm.querySelector('.password.valid-check');
    const pwCheckVC = joinForm.querySelector('.password-check.valid-check');
    const bVC = joinForm.querySelector('.birth.valid-check');
    const gVC = joinForm.querySelector('.gender.valid-check');

    const eInput = eVC.querySelector('input');
    const nInput = nVC.querySelector('input');
    const pwInput = pwVC.querySelector('input');
    const pwCheckInput = pwCheckVC.querySelector('input');
    const bInput = bVC.querySelector('input');

    const eRegex = new RegExp(eInput.dataset.regex);
    const nRegex = new RegExp(nInput.dataset.regex);
    const pwRegex = new RegExp(pwInput.dataset.regex);
    const bRegex = new RegExp(bInput.dataset.regex);

    const gs = joinForm.querySelectorAll('[name="gender"]');
    let gender = '';

    for (const g of gs) {
        if (g.checked) {
            gender = g.value;
        }
    }

    eVC.classList.remove('-invalid');
    nVC.classList.remove('-invalid');
    pwVC.classList.remove('-invalid');
    pwCheckVC.classList.remove('-invalid');
    bVC.classList.remove('-invalid');
    gVC.classList.remove('-invalid');


    if (eInput.value === '' || !eRegex.test(eInput.value)) {
        eVC.classList.add('-invalid');
        eVC.querySelector('.warning').innerText = '잘못된 이메일 형식입니다.';
        validCount++;
    }

    if (nInput.value === '' || !nRegex.test(nInput.value)) {
        nVC.classList.add('-invalid');
        nVC.querySelector('.warning').innerText = '닉네임은 숫자, 영소문자, 영대문자, 한글, -, _ 로 이루어진 2 ~ 8자입니다.';
        validCount++;
    }

    if (pwInput.value === '' || !pwRegex.test(pwInput.value)) {
        pwVC.classList.add('-invalid');
        pwVC.querySelector('.warning').innerText = '비밀번호는 8 ~ 20자리 이상의 영어, 숫자, 특수문자의 조합입니다.';
        validCount++;
    }

    if (pwInput.value !== pwCheckInput.value) {
        pwCheckVC.classList.add('-invalid');
        pwCheckVC.querySelector('.warning').innerText = '입력하신 비밀번호와 일치하지 않습니다.';
        validCount++;
    }

    if (bInput.value === '' || !bRegex.test(bInput.value) || !checkBirth(bInput.value)) {
        bVC.classList.add('-invalid');
        bVC.querySelector('.warning').innerText = '잘못된 생년월일 형식입니다.';
        validCount++;
    }

    if (gender === '') {
        gVC.classList.add('-invalid');
        gVC.querySelector('.warning').innerText = '성별을 선택해주세요.';
        validCount++;
    }

    let requestObject = {
        email : joinForm.email.value,
        nickname : joinForm.nickname.value,
        password : joinForm.password.value,
        gender : gender,
        birth : joinForm.birth.value,
    };

    if (validCount > 0) {
        return;
    }

    const joinModal = new ModalObj();
    joinModal.createModal('알림', '입력하신 정보로 회원가입하시겠습니까?', [{
        title : '확인',
        onclick : () => {
            fetch('/api/join', {
                method : 'POST',
                headers : {
                    'Content-Type': 'application/json'
                },
                body : JSON.stringify(requestObject)
            }).then(response => response.json())
                .then(data => {
                    if (data.success === true) {
                        joinModal.createModal('알림', data.message, [{
                            title : '확인',
                            onclick : () => location.href = '/login'
                        }]);
                    }
                    if (data.success === false) {
                        joinModal.createSimpleModal('알림', data.message);
                    }
                })
                .catch(error => console.error('Error:', error));
        }
    }, {
        title : '취소',
        onclick : () => joinModal.delete()
    }]);
}