import { refreshAccessToken } from '../../shared/api/httpClient';
import { authApi } from './api/authApi';
import { useAuthStore } from './store/authStore';

export async function bootstrapAuthSession(): Promise<void> {
  const { refreshToken, accessToken, setAuthStatus } = useAuthStore.getState();
  if (!refreshToken) {
    setAuthStatus('unauthenticated');
    return;
  }
  if (accessToken) {
    setAuthStatus('authenticated');
    return;
  }
  const token = await refreshAccessToken();
  if (token) {
    await authApi.me().catch(() => undefined);
    setAuthStatus('authenticated');
  } else {
    setAuthStatus('unauthenticated');
  }
}
