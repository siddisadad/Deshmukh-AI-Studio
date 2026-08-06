import { Alert, CircularProgress, Stack, Typography } from '@mui/material';
import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { ApiError } from '../../../shared/api/types';
import { authApi } from '../api/authApi';
import { useAuthStore } from '../store/authStore';
import { AuthCard } from './LoginPage';

export function SsoCallbackPage() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const setSession = useAuthStore((s) => s.setSession);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const provider = params.get('provider');
    const code = params.get('code');
    const state = params.get('state');
    if (!provider || !code || !state) {
      setError('Missing SSO callback parameters');
      return;
    }

    let cancelled = false;
    void (async () => {
      try {
        const redirectUri = `${window.location.origin}/auth/sso/callback`;
        const data = await authApi.completeSso({ provider, code, state, redirectUri });
        if (cancelled) {
          return;
        }
        setSession(data);
        navigate('/dashboard', { replace: true });
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : 'SSO sign-in failed');
        }
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [navigate, params, setSession]);

  return (
    <AuthCard title="Signing you in" subtitle="Completing SSO authentication">
      <Stack spacing={2} sx={{ alignItems: 'center', py: 2 }}>
        {error ? (
          <Alert severity="error" sx={{ width: '100%' }}>
            {error}
          </Alert>
        ) : (
          <>
            <CircularProgress size={28} />
            <Typography variant="body2" color="text.secondary">
              Please wait…
            </Typography>
          </>
        )}
      </Stack>
    </AuthCard>
  );
}
