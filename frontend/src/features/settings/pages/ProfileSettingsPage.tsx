import {
  Alert,
  Button,
  FormControl,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { useEffect, useState, type FormEvent } from 'react';
import { ApiError } from '../../../shared/api/types';
import { authApi } from '../../auth/api/authApi';
import { useAuthStore } from '../../auth/store/authStore';

export function ProfileSettingsPage() {
  const setSession = useAuthStore((s) => s.setSession);
  const accessToken = useAuthStore((s) => s.accessToken);
  const refreshToken = useAuthStore((s) => s.refreshToken);
  const organization = useAuthStore((s) => s.organization);
  const [displayName, setDisplayName] = useState('');
  const [theme, setTheme] = useState<'LIGHT' | 'DARK' | 'SYSTEM'>('SYSTEM');
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    void authApi.me().then((me) => {
      setDisplayName(me.displayName);
      setTheme(me.theme);
      if (accessToken && refreshToken && organization) {
        setSession({
          user: {
            id: me.id,
            email: me.email,
            displayName: me.displayName,
            theme: me.theme,
          },
          organization,
          accessToken,
          refreshToken,
        });
      }
    });
  }, [accessToken, refreshToken, organization, setSession]);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setMessage(null);
    try {
      const me = await authApi.updateProfile({ displayName, theme });
      if (accessToken && refreshToken && organization) {
        setSession({
          user: {
            id: me.id,
            email: me.email,
            displayName: me.displayName,
            theme: me.theme,
          },
          organization,
          accessToken,
          refreshToken,
        });
      }
      setMessage('Profile updated');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Update failed');
    }
  }

  return (
    <Stack spacing={3} sx={{ maxWidth: 520 }}>
      <Typography variant="h4">Profile</Typography>
      <Paper component="form" onSubmit={onSubmit} variant="outlined" sx={{ p: 3 }}>
        <Stack spacing={2}>
          {error && <Alert severity="error">{error}</Alert>}
          {message && <Alert severity="success">{message}</Alert>}
          <TextField label="Display name" value={displayName} onChange={(e) => setDisplayName(e.target.value)} />
          <FormControl>
            <InputLabel id="theme-label">Theme</InputLabel>
            <Select
              labelId="theme-label"
              label="Theme"
              value={theme}
              onChange={(e) => setTheme(e.target.value as 'LIGHT' | 'DARK' | 'SYSTEM')}
            >
              <MenuItem value="SYSTEM">System</MenuItem>
              <MenuItem value="LIGHT">Light</MenuItem>
              <MenuItem value="DARK">Dark</MenuItem>
            </Select>
          </FormControl>
          <Button type="submit" variant="contained">
            Save
          </Button>
        </Stack>
      </Paper>
    </Stack>
  );
}
