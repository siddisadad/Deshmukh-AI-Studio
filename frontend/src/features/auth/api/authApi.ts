import { http } from '../../../shared/api/httpClient';
import type { MeResponse, TokenResponse } from '../../../shared/api/types';

export interface SsoProvider {
  id: string;
  displayName: string;
}

export interface SsoStartResponse {
  provider: string;
  authorizationUrl: string;
  state: string;
}

export const authApi = {
  register: (body: { email: string; password: string; displayName: string }) =>
    http.post<TokenResponse>('/auth/register', body).then((r) => r.data),
  login: (body: { email: string; password: string }) =>
    http.post<TokenResponse>('/auth/login', body).then((r) => r.data),
  logout: (refreshToken?: string | null) =>
    http.post('/auth/logout', refreshToken ? { refreshToken } : {}).then(() => undefined),
  forgotPassword: (email: string) =>
    http.post('/auth/forgot-password', { email }).then(() => undefined),
  resetPassword: (body: { token: string; newPassword: string }) =>
    http.post('/auth/reset-password', body).then(() => undefined),
  me: () => http.get<MeResponse>('/me').then((r) => r.data),
  updateProfile: (body: { displayName?: string; theme?: string }) =>
    http.patch<MeResponse>('/me', body).then((r) => r.data),
  changePassword: (body: { currentPassword: string; newPassword: string }) =>
    http.post<TokenResponse>('/me/password', body).then((r) => r.data),
  listSsoProviders: () => http.get<SsoProvider[]>('/auth/sso/providers').then((r) => r.data),
  startSso: (body: { provider: string; redirectUri: string; loginHint?: string }) =>
    http.post<SsoStartResponse>('/auth/sso/start', body).then((r) => r.data),
  completeSso: (body: { provider: string; code: string; state: string; redirectUri?: string }) =>
    http.post<TokenResponse>('/auth/sso/callback', body).then((r) => r.data),
};
