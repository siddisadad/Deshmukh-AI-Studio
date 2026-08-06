import { Alert, Paper, Stack, Typography } from '@mui/material';
import { useAuthStore } from '../../auth/store/authStore';

export function DashboardPage() {
  const user = useAuthStore((s) => s.user);
  const organization = useAuthStore((s) => s.organization);

  return (
    <Stack spacing={3}>
      <div>
        <Typography variant="h4" gutterBottom>
          Dashboard
        </Typography>
        <Typography color="text.secondary">
          Welcome back, {user?.displayName}. Projects and AI assistants land in the next slices.
        </Typography>
      </div>
      <Paper variant="outlined" sx={{ p: 3 }}>
        <Typography variant="h6" gutterBottom>
          {organization?.name}
        </Typography>
        <Typography color="text.secondary" gutterBottom>
          Org slug: {organization?.slug}
        </Typography>
        <Alert severity="info" sx={{ mt: 2 }}>
          Auth foundations are live. Next up: project management, requirements, Kanban, and AI chat.
        </Alert>
      </Paper>
    </Stack>
  );
}
