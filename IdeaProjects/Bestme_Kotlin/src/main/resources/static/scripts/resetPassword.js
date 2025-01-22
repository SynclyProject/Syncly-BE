const identifyUserSection = document.getElementById('identify-user-section');
const resetPasswordSection = document.getElementById('reset-password-section');
const identifyUserForm = document.getElementById('identify-user-form');
const resetPasswordForm = document.getElementById('reset-password-form');

const passwordLabel= resetPasswordForm.querySelector('.password > .text-label');

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

identifyUserForm.onsubmit = (e) => {
    e.preventDefault();

    let validCount = 0;

    const eVC = identifyUserForm.querySelector('.email.valid-check');
    const bVC = identifyUserForm.querySelector('.birth.valid-check');

    const eRegex = new RegExp(identifyUserForm.querySelector('input[name="email"]').dataset.regex);
    const bRegex = new RegExp(identifyUserForm.querySelector('input[name="birth"]').dataset.regex);

    const email = identifyUserForm.email.value;
    const birth = identifyUserForm.birth.value;

    eVC.classList.remove('-invalid');
    bVC.classList.remove('-invalid');

     if (email === "" || !eRegex.test(email)) {
         eVC.classList.add('-invalid');
         eVC.querySelector('.warning').innerText = '잘못된 이메일 형식입니다.';
         validCount++;
     }

     if (birth === "" || !bRegex.test(birth) || !checkBirth(birth)) {
         bVC.classList.add('-invalid');
         bVC.querySelector('.warning').innerText = '잘못된 생년월일 형식입니다.';
         validCount++;
     }

    if (validCount > 0) {
        return;
    }

    let requestObject = {
        email : email,
        birth : birth
    };

    fetch('/api/identifyUser', {
        method : 'POST',
        headers : {
            'Content-Type' : 'application/json'
        },
        body : JSON.stringify(requestObject)
    }).then(response => response.json())
        .then(data => {
            if (data.success === true) {
                console.log(data.data);
                resetPasswordForm.querySelector('input[name="user_id"]').value = data.data;
                identifyUserSection.hide();
                resetPasswordSection.show();
            }
            if (data.success === false) {
                const resetModal = new ModalObj();
                resetModal.createSimpleModal('알림', data.message);
            }
        })
        .catch(error => console.error('Error:', error));
}


resetPasswordForm.onsubmit = (e) => {
    e.preventDefault();

    const pVC = resetPasswordForm.querySelector('.password.valid-check');
    const pRegex = new RegExp(resetPasswordForm.querySelector('input[name="password"]').dataset.regex);
    const userId = resetPasswordForm.user_id.value;
    const password = resetPasswordForm.password.value;

    pVC.classList.remove('-invalid');

    // if (password === "" || !pRegex.test(password)) {
    //     pVC.classList.add('-invalid');
    //     pVC.querySelector('.warning').innerText = '잘못된 비밀번호 형식입니다.';
    //     return false;
    // }

    let requestObject = {
        userId : userId,
        password : password
    };

    const resetModal = new ModalObj();
    resetModal.createModal('알림', '비밀번호를 재설정하시겠습니까?', [{
        title : '확인',
        onclick : () => {
            fetch('/api/resetPassword', {
                method : 'POST',
                headers : {
                    'Content-Type' : 'application/json'
                },
                body : JSON.stringify(requestObject)
            }).then(response => response.json())
                .then(data => {
                    if (data.success === true) {
                        resetModal.createModal('알림', data.message, [{
                            title : '확인',
                            onclick : () => {
                                resetModal.delete();
                                location.href = '/login';
                            }
                        }])
                    }
                    if (data.success === false) {
                        resetModal.createSimpleModal('알림', data.message);
                    }
                })
                .catch(error => console.error('Error:', error));
        }
    }, {
        title : '취소',
        onclick : () => resetModal.delete()
    }])
}