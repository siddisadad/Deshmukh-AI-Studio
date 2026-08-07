import { Alert, Box, Button, Link as MuiLink, Stack, TextField, Typography } from '@mui/material';
import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { ApiError } from '../../../shared/api/types';
import { authApi } from '../api/authApi';
import { AuthCard } from './LoginPage';

export function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [done, setDone] = useState(false);
  const [loading, setLoading] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      await authApi.forgotPassword(email);
      setDone(true);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Request failed');
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthCard title="Forgot password" subtitle="We’ll email a reset link if the account exists">
      {done ? (
        <Stack spacing={2}>
          <Alert severity="success">
            If that email is registered, a reset link was sent. In local dev the token is also printed in API logs.
          </Alert>
          <Button component={Link} to="/reset-password" variant="contained" data-testid="forgot-goto-reset">
            Continue to reset password
          </Button>
          <Typography variant="body2" color="text.secondary">
            <MuiLink component={Link} to="/login" underline="hover">
              Back to sign in
            </MuiLink>
          </Typography>
        </Stack>
      ) : (
        <Box component="form" onSubmit={onSubmit}>
          <Stack spacing={2}>
            {error && <Alert severity="error">{error}</Alert>}
            <TextField
              label="Email"
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              slotProps={{ htmlInput: { 'data-testid': 'forgot-email' } }}
            />
            <Button type="submit" variant="contained" size="large" disabled={loading} data-testid="forgot-submit">
              {loading ? 'Sending…' : 'Send reset link'}
            </Button>
            <Typography variant="body2" color="text.secondary">
              <MuiLink component={Link} to="/login" underline="hover">
                Back to sign in
              </MuiLink>
            </Typography>
          </Stack>
        </Box>
      )}
    </AuthCard>
  );
}
