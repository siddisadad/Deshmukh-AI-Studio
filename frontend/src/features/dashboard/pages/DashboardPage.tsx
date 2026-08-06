import {
  Alert,
  Box,
  Button,
  Card,
  CardActionArea,
  CardContent,
  CircularProgress,
  Stack,
  Typography,
} from '@mui/material';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ApiError } from '../../../shared/api/types';
import { useAuthStore } from '../../auth/store/authStore';
import { projectsApi, type DashboardData } from '../../projects/api/projectsApi';
import { CreateProjectDialog } from '../../projects/components/CreateProjectDialog';

export function DashboardPage() {
  const navigate = useNavigate();
  const organization = useAuthStore((s) => s.organization);
  const user = useAuthStore((s) => s.user);
  const [data, setData] = useState<DashboardData | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [createOpen, setCreateOpen] = useState(false);

  async function load() {
    setLoading(true);
    setError(null);
    try {
      setData(await projectsApi.dashboard());
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to load dashboard');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
  }, []);

  return (
    <Stack spacing={3}>
      <Stack direction={{ xs: 'column', sm: 'row' }} sx={{ justifyContent: 'space-between', gap: 2 }}>
        <Box>
          <Typography variant="h4" gutterBottom>
            Dashboard
          </Typography>
          <Typography color="text.secondary">
            Welcome back, {user?.displayName}. Workspace: {organization?.name}
          </Typography>
        </Box>
        <Button variant="contained" onClick={() => setCreateOpen(true)} disabled={!organization}>
          New project
        </Button>
      </Stack>

      {error && <Alert severity="error">{error}</Alert>}
      {loading && (
        <Box sx={{ display: 'grid', placeItems: 'center', py: 6 }}>
          <CircularProgress />
        </Box>
      )}

      {!loading && data && (
        <>
          <Box
            sx={{
              display: 'grid',
              gap: 2,
              gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', md: '1fr 1fr 1fr' },
            }}
          >
            {data.projects.map((project) => (
              <Card key={project.id} variant="outlined">
                <CardActionArea onClick={() => navigate(`/projects/${project.id}`)}>
                  <CardContent>
                    <Typography variant="overline" color="primary">
                      {project.projectKey}
                    </Typography>
                    <Typography variant="h6">{project.name}</Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                      {project.requirementCount} requirements · {project.openTaskCount} open tasks
                    </Typography>
                  </CardContent>
                </CardActionArea>
              </Card>
            ))}
            {data.projects.length === 0 && (
              <Alert severity="info" sx={{ gridColumn: '1 / -1' }}>
                No active projects yet. Create your first project to start the SDLC workspace.
              </Alert>
            )}
          </Box>

          <Box>
            <Typography variant="h6" gutterBottom>
              Recent activity
            </Typography>
            {data.recentActivity.length === 0 ? (
              <Typography color="text.secondary">No activity yet.</Typography>
            ) : (
              <Stack spacing={1}>
                {data.recentActivity.map((item, idx) => (
                  <Typography key={`${item.action}-${item.createdAt}-${idx}`} variant="body2" color="text.secondary">
                    {item.action.replaceAll('_', ' ').toLowerCase()} · {new Date(item.createdAt).toLocaleString()}
                  </Typography>
                ))}
              </Stack>
            )}
          </Box>
        </>
      )}

      {organization && (
        <CreateProjectDialog
          open={createOpen}
          orgId={organization.id}
          onClose={() => setCreateOpen(false)}
          onCreated={(id) => {
            void load();
            navigate(`/projects/${id}`);
          }}
        />
      )}
    </Stack>
  );
}
