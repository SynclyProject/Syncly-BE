import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// 커스텀 메트릭 정의
const errorRate = new Rate('errors');

// 재귀 방식만 테스트하는 시나리오
export const options = {
  stages: [
    { duration: '30s', target: 50 },
    { duration: '1m', target: 100 },
    { duration: '2m', target: 100 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'],
    errors: ['rate<0.1'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'https://api.syncly-io.com';
const LOGIN_EMAIL = __ENV.LOGIN_EMAIL || '1026hzz2@gmail.com';
const LOGIN_PASSWORD = __ENV.LOGIN_PASSWORD || 'Khj86284803!';

const WORKSPACE_ID = 48;
const FOLDER_ID = 60;

function login() {
  const loginPayload = JSON.stringify({
    email: LOGIN_EMAIL,
    password: LOGIN_PASSWORD,
  });

  const loginParams = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  const loginRes = http.post(`${BASE_URL}/api/auth/login`, loginPayload, loginParams);

  check(loginRes, {
    '로그인 성공': (r) => r.status === 200,
  });

  if (loginRes.status !== 200) {
    console.error(`로그인 실패: ${loginRes.status} - ${loginRes.body}`);
    errorRate.add(1);
    return null;
  }

  // ✅ 실제 응답 구조에 맞게 수정
  const token =
    loginRes.json('result') ||
    loginRes.json('accessToken') ||
    loginRes.json('token');

  if (!token) {
    console.error('토큰을 찾을 수 없습니다.');
    errorRate.add(1);
    return null;
  }

  return token;
}

export default function () {
  const token = login();

  if (!token) {
    sleep(1);
    return;
  }

  const params = {
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
  };

  const res = http.get(
    `${BASE_URL}/api/workspaces/${WORKSPACE_ID}/folders/${FOLDER_ID}/path-recursive`,
    params
  );

  const success = check(res, {
    '상태 200': (r) => r.status === 200,
    '응답 시간 < 500ms': (r) => r.timings.duration < 500,
    '응답 시간 < 200ms': (r) => r.timings.duration < 200,
    'path 존재': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.result && body.result.path && Array.isArray(body.result.path);
      } catch (e) {
        return false;
      }
    },
  });

  if (!success) {
    errorRate.add(1);
  }

  sleep(Math.random() * 1 + 0.5);
}

export function setup() {
  console.log('='.repeat(60));
  console.log('재귀 방식 단독 성능 테스트');
  console.log(`BASE_URL: ${BASE_URL}`);
  console.log(`WORKSPACE_ID: ${WORKSPACE_ID}`);
  console.log(`FOLDER_ID: ${FOLDER_ID}`);
  console.log('='.repeat(60));
}

export function teardown(data) {
  console.log('='.repeat(60));
  console.log('재귀 방식 테스트 종료');
  console.log('='.repeat(60));
}
