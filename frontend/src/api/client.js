import axios from 'axios';

// 배포(Vercel)에서는 VITE_API_URL=https://<EC2주소> 로 주입, 로컬 dev에서는 '' (vite proxy 사용)
export const API_BASE = import.meta.env.VITE_API_URL || '';

export const api = axios.create({ baseURL: `${API_BASE}/api` });

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  const guestToken = localStorage.getItem('guestToken');
  if (guestToken) config.headers['X-Guest-Token'] = guestToken;
  const guestNickname = localStorage.getItem('guestNickname');
  if (guestNickname) config.headers['X-Guest-Nickname'] = encodeURIComponent(guestNickname);
  return config;
});

api.interceptors.response.use(
  (r) => r,
  (err) => {
    if (err.response?.status === 401 || err.response?.status === 403) {
      // 로그인 필요한 API인데 토큰 없음 → 로그인으로 (게스트 오픈방 조회는 허용되므로 조용히 패스)
    }
    return Promise.reject(err);
  }
);

export const fileUrl = (url) => {
  if (!url) return url;
  if (url.startsWith('http')) return url;
  return `${API_BASE}${url}`; // dev: vite proxy가 /uploads 처리, prod: EC2 절대경로
};
