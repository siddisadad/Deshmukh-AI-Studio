import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios';
import { useAuthStore } from '../../features/auth/store/authStore';
import { ApiError, type ApiErrorBody, type TokenResponse } from './types';

const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';

export const http = axios.create({
  baseURL,
  headers: { 'Content-Type': 'application/json' },
});

let refreshPromise: Promise<string | null> | null = null;

async function refreshAccessToken(): Promise<string | null> {
  const { refreshToken, setSession, clearSession, user, organization } = useAuthStore.getState();
  if (!refreshToken) {
    clearSession();
    return null;
  }
  try {
    const { data } = await axios.post<TokenResponse>(`${baseURL}/auth/refresh`, { refreshToken });
    setSession({
      user: data.user,
      organization: data.organization,
      accessToken: data.accessToken,
      refreshToken: data.refreshToken,
    });
    return data.accessToken;
  } catch {
    clearSession();
    // keep references used for typing clarity
    void user;
    void organization;
    return null;
  }
}

http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = useAuthStore.getState().accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiErrorBody>) => {
    const original = error.config as InternalAxiosRequestConfig & { _retry?: boolean };
    if (error.response?.status === 401 && original && !original._retry && !original.url?.includes('/auth/')) {
      original._retry = true;
      refreshPromise ??= refreshAccessToken().finally(() => {
        refreshPromise = null;
      });
      const newToken = await refreshPromise;
      if (newToken) {
        original.headers.Authorization = `Bearer ${newToken}`;
        return http(original);
      }
    }
    if (error.response?.data) {
      throw new ApiError(error.response.data);
    }
    throw new ApiError({
      status: error.response?.status || 0,
      code: 'NETWORK_ERROR',
      message: error.message || 'Network error',
    });
  },
);
