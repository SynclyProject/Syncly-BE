if (!checkLogin()) {
    alert("로그인한 회원만 이용이 가능한 페이지입니다.");
    location.href = '/login';  // 로그인 페이지로 이동
}

const modifyForm = document.getElementById('modify_form');

const originPasswordLabel = modifyForm.querySelector('.origin-password > .text-label');
const newPasswordLabel = modifyForm.querySelector('.new-password > .text-label');
const newPasswordCheckLabel = modifyForm.querySelector('.new-password-check > .text-label');

originPasswordLabel.querySelector('button').onclick = () => {
    if (originPasswordLabel.querySelector('button > i').classList.contains('fa-eye')) {
        originPasswordLabel.querySelector('input').removeAttribute('type');
        originPasswordLabel.querySelector('input').setAttribute('type', 'password');
        originPasswordLabel.querySelector('button > i').classList.remove('fa-eye');
        originPasswordLabel.querySelector('button > i').classList.add('fa-eye-slash');
    } else {
        originPasswordLabel.querySelector('input').removeAttribute('type');
        originPasswordLabel.querySelector('input').setAttribute('type', 'text');
        originPasswordLabel.querySelector('button > i').classList.remove('fa-eye-slash');
        originPasswordLabel.querySelector('button > i').classList.add('fa-eye');
    }
}

newPasswordLabel.querySelector('button').onclick = () => {
    if (newPasswordLabel.querySelector('button > i').classList.contains('fa-eye')) {
        newPasswordLabel.querySelector('input').removeAttribute('type');
        newPasswordLabel.querySelector('input').setAttribute('type', 'password');
        newPasswordLabel.querySelector('button > i').classList.remove('fa-eye');
        newPasswordLabel.querySelector('button > i').classList.add('fa-eye-slash');
    } else {
        newPasswordLabel.querySelector('input').removeAttribute('type');
        newPasswordLabel.querySelector('input').setAttribute('type', 'text');
        newPasswordLabel.querySelector('button > i').classList.remove('fa-eye-slash');
        newPasswordLabel.querySelector('button > i').classList.add('fa-eye');
    }
}

newPasswordCheckLabel.querySelector('button').onclick = () => {
    if (newPasswordCheckLabel.querySelector('button > i').classList.contains('fa-eye')) {
        newPasswordCheckLabel.querySelector('input').removeAttribute('type');
        newPasswordCheckLabel.querySelector('input').setAttribute('type', 'password');
        newPasswordCheckLabel.querySelector('button > i').classList.remove('fa-eye');
        newPasswordCheckLabel.querySelector('button > i').classList.add('fa-eye-slash');
    } else {
        newPasswordCheckLabel.querySelector('input').removeAttribute('type');
        newPasswordCheckLabel.querySelector('input').setAttribute('type', 'text');
        newPasswordCheckLabel.querySelector('button > i').classList.remove('fa-eye-slash');
        newPasswordCheckLabel.querySelector('button > i').classList.add('fa-eye');
    }
}

modifyForm.onsubmit = (e) => {
    e.preventDefault();

    let requestObject = {};

    let validCount = 0;

    const opwVC = modifyForm.querySelector('.origin-password.valid-check');
    const nVC = modifyForm.querySelector('.nickname.valid-check');
    const bVC = modifyForm.querySelector('.birth.valid-check');
    const gVC = modifyForm.querySelector('.gender.valid-check');
    const npwVC = modifyForm.querySelector('.new-password.valid-check');
    const npwCheckVC = modifyForm.querySelector('.new-password-check.valid-check');

    const opwInput = opwVC.querySelector('input');
    const nInput = nVC.querySelector('input');
    const npwInput = npwVC.querySelector('input');
    const npwCheckInput = npwCheckVC.querySelector('input');
    const bInput = bVC.querySelector('input');

    const opwRegex = new RegExp(opwInput.dataset.regex);
    const nRegex = new RegExp(nInput.dataset.regex);
    const npwRegex = new RegExp(npwInput.dataset.regex);
    const bRegex = new RegExp(bInput.dataset.regex);

    const gs = modifyForm.querySelectorAll('[name="gender"]');
    let gender = 'U';

    for (const g of gs) {
        if (g.checked) {
            gender = g.value;
        }
    }

    opwVC.classList.remove('-invalid');
    nVC.classList.remove('-invalid');
    bVC.classList.remove('-invalid');
    gVC.classList.remove('-invalid');
    npwVC.classList.remove('-invalid');
    npwCheckVC.classList.remove('-invalid');

    if (opwInput.value === '' || !opwRegex.test(opwInput.value)) {
        opwVC.classList.add('-invalid');
        opwVC.querySelector('.warning').innerText = '잘못된 비밀번호 형식입니다.';
        validCount++;
    }

    if (nInput.value !== '') {
        if(!nRegex.test(nInput.value)) {
            nVC.classList.add('-invalid');
            nVC.querySelector('.warning').innerText = '닉네임은 숫자, 영소문자, 영대문자, 한글, -, _ 로 이루어진 2 ~ 8자입니다.';
            validCount++;
        }
    }

    if (npwInput.value !== '') {
        if(!npwRegex.test(npwInput.value)) {
            npwVC.classList.add('-invalid');
            npwVC.querySelector('.warning').innerText = '비밀번호는 8 ~ 20자리 이상의 영어, 숫자, 특수문자의 조합입니다.';
            validCount++;
        }
    }

    if (bInput.value !== '') {
        if (!bRegex.test(bInput.value) || !checkBirth(bInput.value)) {
            bVC.classList.add('-invalid');
            bVC.querySelector('.warning').innerText = '잘못된 생년월일 형식입니다.';
            validCount++;
        }
    }

    if (gender === '') {
        gVC.classList.add('-invalid');
        gVC.querySelector('.warning').innerText = '성별을 선택해주세요.';
        validCount++;
    }


    if (npwInput.value !== npwCheckInput.value) {
        npwCheckVC.classList.add('-invalid');
        npwCheckVC.querySelector('.warning').innerText = '입력하신 비밀번호와 일치하지 않습니다.';
        validCount++;
    }


    if (validCount > 0) {
        return;
    }

    requestObject.originPassword = opwInput.value;
    requestObject.gender = gender;
    requestObject.birth = bInput.value;
    requestObject.newPassword = npwInput.value;
    requestObject.nickname = nInput.value;

    refresh();
    const token = localStorage.getItem("Authorization");

    const modifyModal = new ModalObj();
    modifyModal.createModal('알림', '입력하신 정보로 수정하시겠습니까?', [{
        title : '확인',
        onclick : () => {
            fetch('/api/user', {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    Authorization: token
                },
                body: JSON.stringify(requestObject)
            }).then(response => response.json())
                .then(data => {
                    if (data.success === true) {
                        localStorage.setItem("Authorization", data.data);
                        modifyModal.createModal('알림', data.message, [{
                            title : '확인',
                            onclick : () => location.href = '/'
                        }]);
                    }
                    if (data.success === false) {
                        modifyModal.createSimpleModal('알림', data.message);
                    }
                })
        }
    }, {
        title: '취소',
        onclick : () => modifyModal.delete()
    }])
}