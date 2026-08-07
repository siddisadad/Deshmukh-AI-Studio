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
import { formatApiError } from '../../../shared/api/formatApiError';
import { authApi } from '../../auth/api/authApi';
import { useAuthStore } from '../../auth/store/authStore';

export function ProfileSettingsPage() {
  const setSession = useAuthStore((s) => s.setSession);
  const accessToken = useAuthStore((s) => s.accessToken);
  const refreshToken = useAuthStore((s) => s.refreshToken);
  const organization = useAuthStore((s) => s.organization);
  const [email, setEmail] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [theme, setTheme] = useState<'LIGHT' | 'DARK' | 'SYSTEM'>('SYSTEM');
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [passwordMessage, setPasswordMessage] = useState<string | null>(null);
  const [passwordError, setPasswordError] = useState<string | null>(null);
  const [passwordLoading, setPasswordLoading] = useState(false);

  useEffect(() => {
    void authApi.me().then((me) => {
      setEmail(me.email);
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
      setEmail(me.email);
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
      setError(formatApiError(err, 'Update failed'));
    }
  }

  async function onChangePassword(e: FormEvent) {
    e.preventDefault();
    setPasswordError(null);
    setPasswordMessage(null);
    if (newPassword !== confirmPassword) {
      setPasswordError('Passwords do not match');
      return;
    }
    if (newPassword.length < 10 || !/[A-Za-z]/.test(newPassword) || !/\d/.test(newPassword)) {
      setPasswordError('New password must be at least 10 characters and include a letter and a number');
      return;
    }
    setPasswordLoading(true);
    try {
      const tokens = await authApi.changePassword({ currentPassword, newPassword });
      setSession({
        user: tokens.user,
        organization: tokens.organization,
        accessToken: tokens.accessToken,
        refreshToken: tokens.refreshToken,
      });
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
      setPasswordMessage('Password updated. Other sessions have been signed out.');
    } catch (err) {
      setPasswordError(formatApiError(err, 'Password change failed'));
    } finally {
      setPasswordLoading(false);
    }
  }

  return (
    <Stack spacing={3} sx={{ maxWidth: 520 }}>
      <Typography variant="h4">Profile</Typography>
      <Paper component="form" onSubmit={onSubmit} variant="outlined" sx={{ p: 3 }}>
        <Stack spacing={2}>
          {error && <Alert severity="error">{error}</Alert>}
          {message && <Alert severity="success">{message}</Alert>}
          <TextField
            label="Email"
            value={email}
            disabled
            helperText="Email cannot be changed here"
            slotProps={{ htmlInput: { 'data-testid': 'profile-email' } }}
          />
          <TextField
            label="Display name"
            value={displayName}
            onChange={(e) => setDisplayName(e.target.value)}
            slotProps={{ htmlInput: { 'data-testid': 'profile-display-name' } }}
          />
          <FormControl>
            <InputLabel id="theme-label">Theme</InputLabel>
            <Select
              labelId="theme-label"
              label="Theme"
              value={theme}
              onChange={(e) => setTheme(e.target.value as 'LIGHT' | 'DARK' | 'SYSTEM')}
              data-testid="profile-theme"
            >
              <MenuItem value="SYSTEM">System</MenuItem>
              <MenuItem value="LIGHT">Light</MenuItem>
              <MenuItem value="DARK">Dark</MenuItem>
            </Select>
          </FormControl>
          <Button type="submit" variant="contained" data-testid="profile-save">
            Save
          </Button>
        </Stack>
      </Paper>

      <Paper component="form" onSubmit={onChangePassword} variant="outlined" sx={{ p: 3 }}>
        <Stack spacing={2}>
          <Typography variant="h6">Change password</Typography>
          {passwordError && <Alert severity="error">{passwordError}</Alert>}
          {passwordMessage && <Alert severity="success">{passwordMessage}</Alert>}
          <TextField
            label="Current password"
            type="password"
            required
            value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
            slotProps={{ htmlInput: { 'data-testid': 'profile-current-password' } }}
          />
          <TextField
            label="New password"
            type="password"
            required
            helperText="At least 10 characters, with a letter and a number"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            slotProps={{ htmlInput: { 'data-testid': 'profile-new-password' } }}
          />
          <TextField
            label="Confirm new password"
            type="password"
            required
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            slotProps={{ htmlInput: { 'data-testid': 'profile-confirm-password' } }}
          />
          <Button
            type="submit"
            variant="contained"
            disabled={passwordLoading}
            data-testid="profile-change-password"
          >
            {passwordLoading ? 'Updating…' : 'Update password'}
          </Button>
        </Stack>
      </Paper>
    </Stack>
  );
}
