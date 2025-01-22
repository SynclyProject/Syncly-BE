// static/scripts/community/file.js
// 지정 게시물 이미지 파일 Read API (바이너리 타입으로 반환 - 스웨거에 사용하려고 만듬 )
async function getImageFile(imagename) {
    try {
        const response = await fetch(`http://localhost:8080/api/community/image/${imagename}`, {
            method: 'GET',
        });

        // 이미지 데이터는 Blob으로 처리
        const imageBlob = await response.blob();  // blob을 사용해 이미지 데이터를 받아옴

        // URL.createObjectURL을 사용하여 Blob을 URL로 변환
        const imageUrl = URL.createObjectURL(imageBlob);

        return imageUrl;  // 생성된 URL을 반환
    } catch (error) {
        console.error('Error fetching image:', error);
        return null;  // 에러 발생 시 null 반환
    }
}

// 지정 게시물 이미지 파일 Read API (url 타입으로 반환)
/*
async function getImageFile(imagename) {
    try {
        const response = await fetch(`http://localhost:8080/api/community/image/${imagename}`, {
            method: 'GET',
        });

        if (!response.ok) {
            throw new Error('Network response was not ok');
        }

        const imageUrl = await response.json();  // 서버에서 반환한 URL을 JSON으로 파싱

        return imageUrl;  // 생성된 URL을 반환
    } catch (error) {
        console.error('Error fetching image:', error);
        return null;  // 에러 발생 시 null 반환
    }
}
 */