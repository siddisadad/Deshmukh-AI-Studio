import {
  AppBar,
  Box,
  Button,
  Container,
  Stack,
  Toolbar,
  Typography,
} from '@mui/material';
import { Link as RouterLink, Outlet, useNavigate } from 'react-router-dom';
import { authApi } from '../../features/auth/api/authApi';
import { useAuthStore } from '../../features/auth/store/authStore';

export function AppShell() {
  const navigate = useNavigate();
  const user = useAuthStore((s) => s.user);
  const organization = useAuthStore((s) => s.organization);
  const refreshToken = useAuthStore((s) => s.refreshToken);
  const clearSession = useAuthStore((s) => s.clearSession);

  async function logout() {
    try {
      await authApi.logout(refreshToken);
    } catch {
      // ignore network errors on logout
    }
    clearSession();
    navigate('/login');
  }

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'background.default' }}>
      <AppBar position="sticky" color="transparent" elevation={0} sx={{ borderBottom: 1, borderColor: 'divider' }}>
        <Toolbar sx={{ gap: 1 }}>
          <Typography
            variant="h6"
            component={RouterLink}
            to="/dashboard"
            sx={{ fontWeight: 700, textDecoration: 'none', color: 'inherit', mr: 2 }}
          >
            AI Studio
          </Typography>
          <Button component={RouterLink} to="/dashboard" color="inherit">
            Dashboard
          </Button>
          <Button component={RouterLink} to="/projects" color="inherit" data-testid="nav-projects">
            Projects
          </Button>
          <Box sx={{ flexGrow: 1 }} />
          <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
            <Typography variant="body2" color="text.secondary">
              {organization?.name}
            </Typography>
            <Button onClick={() => navigate('/settings/billing')}>Billing</Button>
            <Button onClick={() => navigate('/settings/plugins')}>Plugins</Button>
            <Button onClick={() => navigate('/settings/profile')}>{user?.displayName}</Button>
            <Button variant="outlined" onClick={() => void logout()} data-testid="logout-button">
              Log out
            </Button>
          </Stack>
        </Toolbar>
      </AppBar>
      <Container maxWidth="lg" sx={{ py: 4 }}>
        <Outlet />
      </Container>
    </Box>
  );
}
