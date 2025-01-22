if (!checkLogin()) {
    alert("로그인한 회원만 이용이 가능한 페이지입니다.");
    location.href = '/login';  // 로그인 페이지로 이동
}

const deleteForm = document.getElementById('delete_form');
const passwordLabel = deleteForm.querySelector('label');

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

deleteForm.onsubmit = (e) => {
    e.preventDefault();

    if (deleteForm.password.value === "") {
        const deleteModal = new ModalObj();
        deleteModal.createSimpleModal('알림', '비밀번호를 입력해주세요.');
        return;
    }

    const requestObject = {
        password : deleteForm.password.value
    };
    refresh();
    const token = localStorage.getItem("Authorization");
    fetch('/api/user', {
        method: 'DELETE',
        headers: {
            'Content-Type': 'application/json',
            Authorization: token
        },
        body: JSON.stringify(requestObject)
    }).then(response => response.json())
        .then(data => {
            const deleteModal = new ModalObj();
            if (data.success === true) {
                localStorage.removeItem('Authorization');
                deleteModal.createModal('알림', data.message, [{
                    title: '확인',
                    onclick: () => {
                        location.href = '/';
                    }
                }]);
            }
            if (data.success === false) {
                deleteModal.createSimpleModal('알림', data.message);
            }
        })
        .catch(error => console.error('Error:', error));
}