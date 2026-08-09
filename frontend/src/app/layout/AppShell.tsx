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
import { MAIN_CONTENT_ID, SkipToContent } from '../../shared/ui/SkipToContent';

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
      <SkipToContent />
      <AppBar
        position="sticky"
        color="transparent"
        elevation={0}
        component="header"
        sx={{ borderBottom: 1, borderColor: 'divider' }}
      >
        <Toolbar sx={{ gap: 1 }} component="nav" aria-label="Primary">
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
            <Typography variant="body2" color="text.secondary" aria-label="Current organization">
              {organization?.name}
            </Typography>
            <Button onClick={() => navigate('/settings/members')} aria-label="Organization members">
              Members
            </Button>
            <Button
              onClick={() => navigate('/settings/billing')}
              aria-label="Billing and plans"
              data-testid="nav-billing"
            >
              Billing
            </Button>
            <Button
              onClick={() => navigate('/settings/ai-routing')}
              aria-label="AI routing policy"
              data-testid="nav-ai-routing"
            >
              AI routing
            </Button>
            <Button
              onClick={() => navigate('/settings/slo')}
              aria-label="SLO targets"
              data-testid="nav-slo"
            >
              SLO
            </Button>
            <Button
              onClick={() => navigate('/settings/sso')}
              aria-label="SSO and identity providers"
              data-testid="nav-sso"
            >
              SSO
            </Button>
            <Button
              onClick={() => navigate('/settings/dlp')}
              aria-label="DLP and SIEM export"
              data-testid="nav-dlp"
            >
              DLP
            </Button>
            <Button
              onClick={() => navigate('/settings/git')}
              aria-label="Git host credentials"
              data-testid="nav-git"
            >
              Git
            </Button>
            <Button
              onClick={() => navigate('/settings/plugins')}
              aria-label="Plugins and assistants"
              data-testid="nav-plugins"
            >
              Plugins
            </Button>
            <Button
              onClick={() => navigate('/settings/profile')}
              aria-label={`Profile settings for ${user?.displayName ?? 'user'}`}
            >
              {user?.displayName}
            </Button>
            <Button variant="outlined" onClick={() => void logout()} data-testid="logout-button">
              Log out
            </Button>
          </Stack>
        </Toolbar>
      </AppBar>
      <Container
        maxWidth="lg"
        sx={{ py: 4 }}
        component="main"
        id={MAIN_CONTENT_ID}
        aria-label="Main content"
      >
        <Outlet />
      </Container>
    </Box>
  );
}
