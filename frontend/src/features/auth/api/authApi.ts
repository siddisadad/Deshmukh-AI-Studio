import { http } from '../../../shared/api/httpClient';
import type { MeResponse, TokenResponse } from '../../../shared/api/types';

export const authApi = {
  register: (body: { email: string; password: string; displayName: string }) =>
    http.post<TokenResponse>('/auth/register', body).then((r) => r.data),
  login: (body: { email: string; password: string }) =>
    http.post<TokenResponse>('/auth/login', body).then((r) => r.data),
  logout: (refreshToken?: string | null) =>
    http.post('/auth/logout', refreshToken ? { refreshToken } : {}).then(() => undefined),
  forgotPassword: (email: string) =>
    http.post('/auth/forgot-password', { email }).then(() => undefined),
  me: () => http.get<MeResponse>('/me').then((r) => r.data),
  updateProfile: (body: { displayName?: string; theme?: string }) =>
    http.patch<MeResponse>('/me', body).then((r) => r.data),
};
