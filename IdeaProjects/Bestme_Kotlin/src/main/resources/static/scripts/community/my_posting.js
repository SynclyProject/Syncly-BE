// static/scripts/community/my_posting.js
document.addEventListener("DOMContentLoaded", function() {
    console.log("사용자 페이지 - 나의 게시물 페이지 스크립트")

    // const accessToken = localStorage.getItem('Authorization')
    // const buttonBody = document.getElementById("buttons");

    // 버튼 HTML 동적 생성
    // if (accessToken) {
    //     console.log("accessToken: " + accessToken.toString())
    //     buttonBody.innerHTML = `<button type="button" class="green" onclick="location.href = '/community/write'">글 작성</button>`;
    // }

    // 게시물 목록 API 호출
    fetch('http://localhost:8080/api/member/my_boards', {
        method: 'GET',
        headers: {
            Authorization: localStorage.getItem("Authorization")
        },
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(data => {
            console.log(`게시물 목록 데이터: ${data}`);
            console.log(data);

            // 게시물 데이터 추출
            const posts = data.data.content;
            const tableBody = document.getElementById("board-list");

            // 현재 날짜 계산
            const today = new Date();
            const todayDate = `${today.getFullYear()}-${(today.getMonth() + 1).toString().padStart(2, '0')}-${today.getDate().toString().padStart(2, '0')}`;

            // 게시물 데이터를 HTML 테이블에 추가
            posts.forEach((post) => {
                const newRow = document.createElement("tr");

                // 날짜 포맷 (YYYY-MM-DD)
                const createdAtDate = new Date(post.createdAt);
                const createDate =
                    `${createdAtDate.getFullYear()}-${(createdAtDate.getMonth() + 1).toString().padStart(2, '0')}-${createdAtDate.getDate().toString().padStart(2, '0')}`;

                // 시간 포맷 (12시간제, AM/PM)
                const hours24 = createdAtDate.getHours();
                const minutes = createdAtDate.getMinutes().toString().padStart(2, '0');
                const period = hours24 >= 12 ? 'PM' : 'AM';
                const hours12 = hours24 % 12 || 12; // 0시는 12로 변경

                const createTime = `${hours12}:${minutes} ${period}`;

                // icon_new.gif 표시 여부 결정
                const isNew = createDate === todayDate;
                const newIconHtml = isNew ? '<img src="/imgs/community/icon_new.gif" alt="NEW">' : '';

                // links 배열에서 href 추출
                const hrefLink = post.links.find(link => link.rel === 'self')?.href || '#';

                newRow.innerHTML = `
                    <td>${post.boardId}</td>
                    <td class="left">
                        <a href="${hrefLink}">${post.subject}</a>&nbsp;
                        ${newIconHtml}
                    </td>
                    <td>${createDate}</td>
                    <td>${createTime}</td>
                `;

                tableBody.appendChild(newRow);
            });

            // 데이터 총 갯수 id
            // const boardCount = document.getElementById("boards-count");
            // boardCount.innerHTML = `<div class="bold">총 <span class="txt_orange">${data.data.page.totalElements}</span>건</div>`;

            // 페이지 및 링크 데이터 추출
            const pages = data.data.page;
            const links = data.data.links;
            // 페이지 표시 id
            const pageNav = document.getElementById("page-nav");

            const currentPage = pages.number + 1; // 현재 페이지
            const totalPages = pages.totalPages; // 총 페이지 수
            const maxVisiblePages = 5; // 한 번에 표시할 페이지 번호 개수

            // 현재 페이지 그룹 계산 ( 서버에 가지고 있는 DTO 값을 추가해서 반환 - 나중에 )
            const currentGroup = Math.floor((currentPage - 1) / maxVisiblePages);
            const groupStartPage = currentGroup * maxVisiblePages + 1; // 현재 그룹의 첫 페이지 번호
            const groupEndPage = Math.min(groupStartPage + maxVisiblePages - 1, totalPages); // 현재 그룹의 마지막 페이지 번호

            // 네비게이션 HTML 생성
            // << 버튼 (firstPageOfPrevGroup 링크 사용)
            const firstPageOfPrevGroup = links.find(link => link.rel === "firstPageOfPrevGroup");
            let paginationHtml = firstPageOfPrevGroup
                ? `<a href="${firstPageOfPrevGroup.href}"><i class="fa-solid fa-angles-left"></i></a>`
                : '<a class="disabled"><i class="fa-solid fa-angles-left"></i></a>';

            // < 버튼 (prevPage 링크 사용)
            const prevPage = links.find(link => link.rel === "prevPage");
            paginationHtml += prevPage
                ? `<a href="${prevPage.href}"><i class="fa-solid fa-angle-left"></i></a>`
                : '<a class="disabled"><i class="fa-solid fa-angle-left"></i></a>';

            // 페이지 번호
            for (let i = groupStartPage; i <= groupEndPage; i++) {
                paginationHtml += `<a href="/member/my_posting/${i}" class="page-btn ${i === currentPage ? "sel" : ""}">${i}</a>`;
            }

            // > 버튼 (nextPage 링크 사용)
            const nextPage = links.find(link => link.rel === "nextPage");
            paginationHtml += nextPage
                ? `<a href="${nextPage.href}"><i class="fa-solid fa-angle-right"></i></a>`
                : '<a class="disabled"><i class="fa-solid fa-angle-right"></i></a>';

            // >> 버튼 (lastPageOfNextGroup 링크 사용)
            const lastPageOfNextGroup = links.find(link => link.rel === "lastPageOfNextGroup");
            paginationHtml += lastPageOfNextGroup
                ? `<a href="${lastPageOfNextGroup.href}"><i class="fa-solid fa-angles-right"></i></a>`
                : '<a class="disabled"><i class="fa-solid fa-angles-right"></i></a>';

            // 페이지 네비게이션 HTML 삽입
            pageNav.innerHTML = paginationHtml;

        })
        .catch((error) => {
            console.error("Error fetching result data:", error);
            const tableBody = document.getElementById("board-list");
            tableBody.innerHTML = `<tr><td colspan="7">작성된 게시글이 없습니다.</td></tr>`;
        });
});



