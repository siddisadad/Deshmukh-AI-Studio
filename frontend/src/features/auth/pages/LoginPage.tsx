import {
  Alert,
  Box,
  Button,
  Divider,
  Link as MuiLink,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { useEffect, useState, type FormEvent, type ReactNode } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { ApiError } from '../../../shared/api/types';
import { MAIN_CONTENT_ID, SkipToContent } from '../../../shared/ui/SkipToContent';
import { authApi, type SsoProvider } from '../api/authApi';
import { useAuthStore } from '../store/authStore';

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const setSession = useAuthStore((s) => s.setSession);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [ssoLoading, setSsoLoading] = useState(false);
  const [providers, setProviders] = useState<SsoProvider[]>([]);
  const resetSuccess = Boolean((location.state as { resetSuccess?: boolean } | null)?.resetSuccess);
  const orgSlug = new URLSearchParams(location.search).get('org');

  useEffect(() => {
    void authApi
      .listSsoProviders(orgSlug ? { organizationSlug: orgSlug } : undefined)
      .then(setProviders)
      .catch(() => setProviders([]));
  }, [orgSlug]);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const data = await authApi.login({ email, password });
      setSession(data);
      navigate('/dashboard');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Login failed');
    } finally {
      setLoading(false);
    }
  }

  async function onSso(providerId: string) {
    setSsoLoading(true);
    setError(null);
    try {
      const redirectUri = `${window.location.origin}/auth/sso/callback`;
      const started = await authApi.startSso({
        provider: providerId,
        redirectUri,
        loginHint: email || undefined,
      });
      window.location.assign(started.authorizationUrl);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'SSO start failed');
      setSsoLoading(false);
    }
  }

  return (
    <AuthCard title="Sign in" subtitle="Continue to your engineering workspace">
      <Box component="form" onSubmit={onSubmit}>
        <Stack spacing={2}>
          {resetSuccess && <Alert severity="success">Password updated. Sign in with your new password.</Alert>}
          {error && <Alert severity="error">{error}</Alert>}
          <TextField
            label="Email"
            type="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            slotProps={{ htmlInput: { 'data-testid': 'login-email' } }}
          />
          <TextField
            label="Password"
            type="password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            slotProps={{ htmlInput: { 'data-testid': 'login-password' } }}
          />
          <Button
            type="submit"
            variant="contained"
            size="large"
            disabled={loading || ssoLoading}
            data-testid="login-submit"
          >
            {loading ? 'Signing in…' : 'Sign in'}
          </Button>

          {providers.length > 0 && (
            <>
              <Divider>or</Divider>
              {providers.map((provider) => (
                <Button
                  key={provider.id}
                  variant="outlined"
                  size="large"
                  disabled={loading || ssoLoading}
                  onClick={() => void onSso(provider.id)}
                  data-testid={`login-sso-${provider.id}`}
                >
                  {ssoLoading ? 'Redirecting…' : provider.displayName}
                </Button>
              ))}
            </>
          )}

          <Stack direction="row" sx={{ justifyContent: 'space-between' }}>
            <MuiLink component={Link} to="/forgot-password" underline="hover">
              Forgot password?
            </MuiLink>
            <MuiLink component={Link} to="/register" underline="hover">
              Create account
            </MuiLink>
          </Stack>
        </Stack>
      </Box>
    </AuthCard>
  );
}

export function AuthCard({
  title,
  subtitle,
  children,
}: {
  title: string;
  subtitle: string;
  children: ReactNode;
}) {
  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'grid',
        placeItems: 'center',
        px: 2,
        background: (t) =>
          t.palette.mode === 'light'
            ? 'radial-gradient(circle at top left, #CCFBF1 0%, #F1F5F9 45%, #E2E8F0 100%)'
            : 'radial-gradient(circle at top left, #134E4A 0%, #0B1220 50%, #020617 100%)',
      }}
    >
      <SkipToContent />
      <Paper
        component="main"
        id={MAIN_CONTENT_ID}
        aria-label="Authentication"
        sx={{ width: '100%', maxWidth: 420, p: 4 }}
        elevation={0}
        variant="outlined"
      >
        <Stack spacing={1} sx={{ mb: 3 }}>
          <Typography
            component={Link}
            to="/"
            variant="overline"
            color="primary"
            sx={{ fontWeight: 700, textDecoration: 'none', width: 'fit-content' }}
          >
            Deshmukh Technology
          </Typography>
          <Typography variant="h4">{title}</Typography>
          <Typography color="text.secondary">{subtitle}</Typography>
        </Stack>
        {children}
      </Paper>
    </Box>
  );
}