
document.addEventListener("DOMContentLoaded", function() {
    //로그인 여부 검증
    if (!checkLogin()) {
        alert("로그인한 회원만 이용이 가능한 페이지입니다.");
        location.href = '/login';  // 로그인 페이지로 이동
        return;
    }


    //닉네임 변경
    const nicknameSection = document.getElementById("personal-nickname");
    getLoginInfo().then(response => {
        const nickname = response.nickname;
        console.log(nickname);
        nicknameSection.innerHTML = nickname;
    }).catch(error => {
        console.error("Error:", error);
    });

    refresh();
    const accessToken = localStorage.getItem('Authorization')

    const colorSection = document.getElementById("personal-color-h");

    fetch('http://localhost:8080/api/results/color', {
        method: 'GET',
        headers: {
            'Authorization': accessToken
        },
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(data => {
            console.log(data);
            const fiveTon = document.getElementById("five-ton");

            if (data.data.colorId === 1) {
                colorSection.innerHTML = "봄 웜톤";
                colorSection.style.color = "#f65882"; // 밝은 주황색 계열
            } else if (data.data.colorId === 2) {
                colorSection.innerHTML = "여름 쿨톤";
                colorSection.style.color = "#358ccd"; // 밝은 파란색 계열
            } else if (data.data.colorId === 3) {
                colorSection.innerHTML = "가을 웜톤";
                colorSection.style.color = "#c54603"; // 따뜻한 갈색 계열
            } else if (data.data.colorId === 4) {
                colorSection.innerHTML = "겨울 쿨톤";
                colorSection.style.color = "#081b63"; // 진한 파란색 계열
            } else {
                colorSection.innerHTML = "진단을 내기에 결과가 충분하지 않습니다.";
                fiveTon.innerHTML = "진단을 여러번 더 진행해주세요";
            }
        })
        .catch((error) => {
            console.error("Error fetching result data:", error);
        });

    //전체 결과 띄우기
    const resultList = document.getElementById("content-personal-history");
    fetch("http://localhost:8080/api/results", {
        method: 'GET',
        headers: {
            'Authorization': accessToken
        }
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(data => {
            console.log(data);

            for(let i = 0; i<data.data.length; i++) {
                const li = document.createElement("li");
                li.classList.add("list-group-item", "d-flex", "mt-3");

                let p_color;

                if (data.data[i].colorId === 1) {p_color = "봄 웜톤"}
                else if (data.data[i].colorId === 2) {p_color = "여름 쿨톤"}
                else if (data.data[i].colorId === 3) {p_color = "가을 웜톤"}
                else if (data.data[i].colorId === 4) {p_color = "겨울 쿨톤"}
                else {p_color = "진단 불가"}

                li.innerHTML = `
                    <div class="result-one">
                        <div class="result-one-1">#${i+1} 진단 결과: ${p_color}</div>
                        <div class="result-one-2">가장 밝은 피부톤: ${data.data[i].lightestSkinColor} | 가장 어두운 피부톤: ${data.data[i].darkestSkinColor} | 립 컬러: ${data.data[i].lipColor}</div>
                        <div class="result-one-2">헤어 컬러: ${data.data[i].hairColor} | 동공 색상: ${data.data[i].pupilColor} | 홍채 색상: ${data.data[i].irisColor}</div>
                    </div>`;

                resultList.appendChild(li);
            }




        })
        .catch((error) => {
            console.error("Error fetching user data:", error);

            document.querySelector(".content-personal").style.display = 'none';
            document.querySelector(".content-personal-history").style.display = 'none';

            const li = document.createElement("li");
            li.classList.add("list-group-item", "d-flex", "mt-3", "custom-list-item");

            li.innerHTML = `
                    <h2>아직 진단받은 내역이 존재하지 않습니다.</h2><h3>퍼스널컬러 테스트 탭에서 진단을 시작해주세요.</h3>`;

            document.getElementById("content-row").appendChild(li);

        });

});



