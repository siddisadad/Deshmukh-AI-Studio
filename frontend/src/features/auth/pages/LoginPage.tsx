import {
  Alert,
  Box,
  Button,
  Link as MuiLink,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { useState, type FormEvent, type ReactNode } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ApiError } from '../../../shared/api/types';
import { authApi } from '../api/authApi';
import { useAuthStore } from '../store/authStore';

export function LoginPage() {
  const navigate = useNavigate();
  const setSession = useAuthStore((s) => s.setSession);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

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

  return (
    <AuthCard title="Sign in" subtitle="Continue to your engineering workspace">
      <Box component="form" onSubmit={onSubmit}>
        <Stack spacing={2}>
          {error && <Alert severity="error">{error}</Alert>}
          <TextField label="Email" type="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
          <TextField
            label="Password"
            type="password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          <Button type="submit" variant="contained" size="large" disabled={loading}>
            {loading ? 'Signing in…' : 'Sign in'}
          </Button>
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
      <Paper sx={{ width: '100%', maxWidth: 420, p: 4 }} elevation={0} variant="outlined">
        <Stack spacing={1} sx={{ mb: 3 }}>
          <Typography variant="overline" color="primary" sx={{ fontWeight: 700 }}>
            AI Studio
          </Typography>
          <Typography variant="h4">{title}</Typography>
          <Typography color="text.secondary">{subtitle}</Typography>
        </Stack>
        {children}
      </Paper>
    </Box>
  );
}
