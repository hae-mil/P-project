const BASE_URL = "http://localhost:8080"; // 백엔드 서버 주소

const request = async (endpoint, options = {}) => {
  try {
    const response = await fetch(`${BASE_URL}${endpoint}`, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...options.headers,
      },
    });
    
    if (!response.ok) {
      throw new Error(`HTTP Error: ${response.status}`);
    }
    
    // 응답이 없는 경우(DELETE 등)를 대비
    const text = await response.text();
    return text ? JSON.parse(text) : {};
  } catch (e) {
    console.error(`API Request Failed: ${endpoint}`, e);
    return { success: false, message: e.message }; // 에러 처리
  }
};

// =================================================================
// 1. 대시보드
// =================================================================
export const getDashboardSummary = () => request('/admin/dashboard/summary');

// =================================================================
// 2. 등록된 사업 관리 (정책)
// =================================================================
export const getPolicies = (keyword = '', category = '') => 
  request(`/admin/policies?keyword=${encodeURIComponent(keyword)}&category=${encodeURIComponent(category)}`);

export const getPolicyDetail = (policyId) => request(`/admin/policies/${policyId}`);

export const createPolicy = (data) => 
  request('/admin/policies', { method: 'POST', body: JSON.stringify(data) });

export const updatePolicy = (policyId, data) => 
  request(`/admin/policies/${policyId}`, { method: 'PUT', body: JSON.stringify(data) });

export const deletePolicy = (policyId) => 
  request(`/admin/policies/${policyId}`, { method: 'DELETE' });

// =================================================================
// 3. 변경 이력 리포트
// =================================================================
export const getChangeLogs = () => request('/admin/policy-change-logs');

export const getChangeLogDetail = (logId) => request(`/admin/policy-change-logs/${logId}`);

// AI 요약 검증/재생성 (리포트 페이지 기능)
export const checkAiSummary = (data) => 
  request('/admin/ai/summary/check', { method: 'POST', body: JSON.stringify(data) });

// =================================================================
// 4. 서버 관리
// =================================================================
export const getServerMetrics = () => request('/admin/server/metrics');

export const getServerLogs = () => request('/admin/server/logs');