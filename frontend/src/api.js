// 👇 실제 서버 주소
const BASE_URL = 'http://your-real-server-ip.com'; 

const request = async (endpoint, options = {}) => {
  try {
    const url = `${BASE_URL}${endpoint}`;
    console.log(`📡 요청 보냄: ${url}`);

    const response = await fetch(url, {
      headers: {
        'Content-Type': 'application/json',
      },
      ...options,
    });

    const data = await response.json();
    
    if (!response.ok) {
      throw new Error(data.message || '서버 통신 오류');
    }

    return data;
  } catch (error) {
    console.error('🚨 API 에러:', error);
    return { success: false, message: error.message || '네트워크 연결 실패' };
  }
};

// =================================================================
// 1. 로그인 API (username 사용)
// =================================================================
export const loginAPI = async (username, password) => {
  return request('/api/v1/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  });
};

// =================================================================
// 2. 회원가입 API
// =================================================================
export const signupAPI = async (userData) => {
  // userData 안에는 { username, password, name } 이 들어있어야 함
  return request('/api/v1/auth/signup', {
    method: 'POST',
    body: JSON.stringify(userData),
  });
};

// =================================================================
// 3. 로그아웃 API
// =================================================================
export const logoutAPI = async () => {
  return request('/api/v1/auth/logout', {
    method: 'POST',
  });
};

// =================================================================
// 4. 홈 화면 데이터
// =================================================================
export const getHomeSummaryAPI = async () => {
  return request('/api/v1/home/summary', {
    method: 'GET',
  });
};

// =================================================================
// 5. 일정 목록
// =================================================================
export const getSchedulesAPI = async (date) => {
  return request(`/api/v1/calendar/events?date=${date}`, {
    method: 'GET',
  });
};