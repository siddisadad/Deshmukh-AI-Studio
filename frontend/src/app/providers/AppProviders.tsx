import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useEffect, useState, type ReactNode } from 'react';
import { authApi } from '../../features/auth/api/authApi';
import { useAuthStore } from '../../features/auth/store/authStore';
import { ThemeProvider } from './ThemeProvider';

export function AppProviders({ children }: { children: ReactNode }) {
  const [queryClient] = useState(() => new QueryClient());
  const hydrateRefreshToken = useAuthStore((s) => s.hydrateRefreshToken);
  const refreshToken = useAuthStore((s) => s.refreshToken);
  const accessToken = useAuthStore((s) => s.accessToken);
  const setSession = useAuthStore((s) => s.setSession);
  const clearSession = useAuthStore((s) => s.clearSession);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    hydrateRefreshToken();
    setReady(true);
  }, [hydrateRefreshToken]);

  useEffect(() => {
    if (!ready) return;
    let cancelled = false;
    async function bootstrap() {
      if (!refreshToken || accessToken) {
        return;
      }
      try {
        const { default: axios } = await import('axios');
        const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';
        const { data } = await axios.post(`${baseURL}/auth/refresh`, { refreshToken });
        if (!cancelled) {
          setSession({
            user: data.user,
            organization: data.organization,
            accessToken: data.accessToken,
            refreshToken: data.refreshToken,
          });
          await authApi.me().catch(() => undefined);
        }
      } catch {
        if (!cancelled) clearSession();
      }
    }
    void bootstrap();
    return () => {
      cancelled = true;
    };
  }, [ready, refreshToken, accessToken, setSession, clearSession]);

  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider>{children}</ThemeProvider>
    </QueryClientProvider>
  );
}
