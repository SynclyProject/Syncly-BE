document.addEventListener("DOMContentLoaded", function() {
    refresh();
    const accessToken = localStorage.getItem('Authorization')

    //URL에서 값 가져오기
    const params = new URLSearchParams(window.location.search);

    const inputData = {
        lightestSkinColor: params.get('lightestSkinColor'),
        darkestSkinColor: params.get('darkestSkinColor'),
        lipColor: params.get('lipColor'),
        pupilColor: params.get('pupilColor'),
        irisColor: params.get('irisColor'),
        hairColor: params.get('hairColor'),
    };


    // 사용자 정보 가져오기
    fetch('http://localhost:8080/api/results', {
        method: 'POST',
        headers: {
            "Content-Type": "application/json",
            'Authorization': accessToken
        },
        body: JSON.stringify(inputData)
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        return response.json();
    })
    .then(data => {
        console.log(data);
        const resultColorElement = document.getElementById("result-color");
        const descriptionElement = document.getElementById("result-desc");
        if (data.data.colorId === 1) {
            resultColorElement.innerHTML = "봄 웜톤";
            resultColorElement.style.color = "#f65882"; // 밝은 주황색 계열
            descriptionElement.innerHTML = data.message;
        } else if (data.data.colorId === 2) {
            resultColorElement.innerHTML = "여름 쿨톤";
            resultColorElement.style.color = "#358ccd"; // 밝은 파란색 계열
            descriptionElement.innerHTML = data.message;
        } else if (data.data.colorId === 3) {
            resultColorElement.innerHTML = "가을 웜톤";
            resultColorElement.style.color = "#c54603"; // 따뜻한 갈색 계열
            descriptionElement.innerHTML = data.message;
        } else if (data.data.colorId === 4) {
            resultColorElement.innerHTML = "겨울 쿨톤";
            resultColorElement.style.color = "#081b63"; // 진한 파란색 계열
            descriptionElement.innerHTML = data.message;
        } else {
            document.getElementById("r1").innerHTML = "입력된 색상코드가";
            resultColorElement.innerHTML = `퍼스널 컬러를 진단하기 적합하지 않습니다`;
            document.getElementById("r2").innerHTML = "다시 진단해주세요";
            descriptionElement.innerHTML = data.message;
        }

    })
    .catch((error) => {
        console.error("Error fetching result data:", error);
    });


    // '내 진단 내역 확인하기' 버튼 클릭 이벤트
    document.getElementById("result-history").addEventListener("click", function () {
        location.href = "/member"; // 진단 내역 페이지로 이동
    });

// '스타일 가이드 보러가기' 버튼 클릭 이벤트
    document.getElementById("result-guide").addEventListener("click", function () {
        location.href = "/style/guide"; // 스타일 가이드 페이지로 이동
    });

});


