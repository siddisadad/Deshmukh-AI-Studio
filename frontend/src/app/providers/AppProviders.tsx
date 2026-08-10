import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useEffect, useState, type ReactNode } from 'react';
import { bootstrapAuthSession } from '../../features/auth/bootstrapAuth';
import { useAuthStore } from '../../features/auth/store/authStore';
import { ThemeProvider } from './ThemeProvider';

export function AppProviders({ children }: { children: ReactNode }) {
  const [queryClient] = useState(() => new QueryClient());
  const hydrateRefreshToken = useAuthStore((s) => s.hydrateRefreshToken);
  const refreshToken = useAuthStore((s) => s.refreshToken);
  const accessToken = useAuthStore((s) => s.accessToken);
  const authStatus = useAuthStore((s) => s.authStatus);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    hydrateRefreshToken();
    setReady(true);
  }, [hydrateRefreshToken]);

  useEffect(() => {
    if (!ready) return;
    if (!refreshToken || accessToken || authStatus === 'authenticated') {
      if (!refreshToken && authStatus === 'unknown') {
        useAuthStore.getState().setAuthStatus('unauthenticated');
      }
      return;
    }
    void bootstrapAuthSession();
  }, [ready, refreshToken, accessToken, authStatus]);

  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider>{children}</ThemeProvider>
    </QueryClientProvider>
  );
}
