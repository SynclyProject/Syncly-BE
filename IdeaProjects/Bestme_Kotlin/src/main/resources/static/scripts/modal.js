// 모달 관련
class ModalObj {
    cover = null;

    constructor() {
        if (this.cover === null) {
            const cover = document.createElement('div');
            cover.setAttribute('id', 'cover');
            document.body.prepend(cover);
            this.cover = cover;
        }
    }

    createModal(title, content, buttonOptions) {
        this.cover.innerHTML = '';

        const modal = new DOMParser().parseFromString(`
        <div class="modal">
            <div class="modal-head">${title}</div>
            <div class="modal-main">${content}</div>
            <div class="button-container"></div>
        </div>
        `, 'text/html').querySelector('.modal');

        const buttonContainer = modal.querySelector('.button-container');
        for (const buttonOption of buttonOptions) {
            const button = this.createButton(buttonOption.title, buttonOption.onclick);
            buttonContainer.append(button);
        }

        this.cover.append(modal);
    }

    createSimpleModal(title, content) {
        this.cover.innerHTML = '';

        const simpleModal = new DOMParser().parseFromString(`
        <div class="modal">
            <div class="modal-head">${title}</div>
            <div class="modal-main">${content}</div>
            <div class="button-container">
                <button type="button">닫기</button>
            </div>
        </div>
        `, 'text/html').querySelector('.modal');

        simpleModal.querySelector('button').onclick = () => {
            this.delete();
        }

        this.cover.append(simpleModal);
    }

    createButton(text, onclick) {
        const button = document.createElement('button');
        button.innerText = text;
        if (typeof onclick === 'function') {
            button.onclick = () => {
                onclick();
            }
        }
        return button;
    }

    delete() {
        this.cover.remove();
    }
}