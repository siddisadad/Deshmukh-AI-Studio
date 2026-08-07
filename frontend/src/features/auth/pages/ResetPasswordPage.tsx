import { Alert, Box, Button, Link as MuiLink, Stack, TextField } from '@mui/material';
import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { formatApiError } from '../../../shared/api/formatApiError';
import { authApi } from '../api/authApi';
import { useAuthStore } from '../store/authStore';
import { AuthCard } from './LoginPage';

export function ResetPasswordPage() {
  const navigate = useNavigate();
  const clearSession = useAuthStore((s) => s.clearSession);
  const [searchParams, setSearchParams] = useSearchParams();
  const tokenFromQuery = useMemo(() => searchParams.get('token')?.trim() || '', [searchParams]);

  const [token, setToken] = useState(tokenFromQuery);
  const [loadedFromLink, setLoadedFromLink] = useState(Boolean(tokenFromQuery));
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!tokenFromQuery) {
      return;
    }
    setToken(tokenFromQuery);
    setLoadedFromLink(true);
    // Drop token from the URL/history after capture so it is not left in the address bar.
    const next = new URLSearchParams(searchParams);
    next.delete('token');
    setSearchParams(next, { replace: true });
    // eslint-disable-next-line react-hooks/exhaustive-deps -- only react when a query token arrives
  }, [tokenFromQuery, setSearchParams]);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (password !== confirm) {
      setError('Passwords do not match');
      return;
    }
    if (password.length < 10 || !/[A-Za-z]/.test(password) || !/\d/.test(password)) {
      setError('Password must be at least 10 characters and include a letter and a number');
      return;
    }
    setLoading(true);
    try {
      await authApi.resetPassword({ token: token.trim(), newPassword: password });
      clearSession();
      navigate('/login', { replace: true, state: { resetSuccess: true } });
    } catch (err) {
      setError(formatApiError(err, 'Reset failed'));
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthCard title="Reset password" subtitle="Choose a new password for your account">
      <Box component="form" onSubmit={onSubmit}>
        <Stack spacing={2}>
          {error && <Alert severity="error">{error}</Alert>}
          <TextField
            label="Reset token"
            required
            value={token}
            onChange={(e) => setToken(e.target.value)}
            helperText={
              loadedFromLink
                ? 'Loaded from email link (removed from the address bar)'
                : 'Paste the token from your reset email / API logs'
            }
            slotProps={{ htmlInput: { 'data-testid': 'reset-token' } }}
          />
          <TextField
            label="New password"
            type="password"
            required
            helperText="At least 10 characters, with a letter and a number"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            slotProps={{ htmlInput: { 'data-testid': 'reset-password' } }}
          />
          <TextField
            label="Confirm password"
            type="password"
            required
            value={confirm}
            onChange={(e) => setConfirm(e.target.value)}
            slotProps={{ htmlInput: { 'data-testid': 'reset-password-confirm' } }}
          />
          <Button
            type="submit"
            variant="contained"
            size="large"
            disabled={loading || !token.trim()}
            data-testid="reset-submit"
          >
            {loading ? 'Updating…' : 'Update password'}
          </Button>
          <MuiLink component={Link} to="/login" underline="hover">
            Back to sign in
          </MuiLink>
        </Stack>
      </Box>
    </AuthCard>
  );
}
