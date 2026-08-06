import {
  Alert,
  Box,
  Button,
  Card,
  CardActionArea,
  CardContent,
  CircularProgress,
  Stack,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
} from '@mui/material';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ApiError } from '../../../shared/api/types';
import { useAuthStore } from '../../auth/store/authStore';
import { projectsApi, type Project } from '../api/projectsApi';
import { CreateProjectDialog } from '../components/CreateProjectDialog';

export function ProjectsPage() {
  const navigate = useNavigate();
  const organization = useAuthStore((s) => s.organization);
  const [status, setStatus] = useState<'ACTIVE' | 'ARCHIVED' | 'ALL'>('ACTIVE');
  const [projects, setProjects] = useState<Project[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [createOpen, setCreateOpen] = useState(false);

  async function load(nextStatus = status) {
    if (!organization) return;
    setLoading(true);
    setError(null);
    try {
      setProjects(await projectsApi.listProjects(organization.id, nextStatus));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to load projects');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [organization?.id, status]);

  return (
    <Stack spacing={3}>
      <Stack direction={{ xs: 'column', sm: 'row' }} sx={{ justifyContent: 'space-between', gap: 2 }}>
        <Typography variant="h4">Projects</Typography>
        <Stack direction="row" spacing={1}>
          <ToggleButtonGroup
            exclusive
            size="small"
            value={status}
            onChange={(_, value) => value && setStatus(value)}
          >
            <ToggleButton value="ACTIVE">Active</ToggleButton>
            <ToggleButton value="ARCHIVED">Archived</ToggleButton>
            <ToggleButton value="ALL">All</ToggleButton>
          </ToggleButtonGroup>
          <Button variant="contained" onClick={() => setCreateOpen(true)}>
            New project
          </Button>
        </Stack>
      </Stack>

      {error && <Alert severity="error">{error}</Alert>}
      {loading ? (
        <Box sx={{ display: 'grid', placeItems: 'center', py: 6 }}>
          <CircularProgress />
        </Box>
      ) : (
        <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' } }}>
          {projects.map((project) => (
            <Card key={project.id} variant="outlined">
              <CardActionArea onClick={() => navigate(`/projects/${project.id}`)}>
                <CardContent>
                  <Typography variant="overline" color="primary">
                    {project.projectKey} · {project.status}
                  </Typography>
                  <Typography variant="h6">{project.name}</Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                    {project.description || 'No description'}
                  </Typography>
                </CardContent>
              </CardActionArea>
            </Card>
          ))}
          {projects.length === 0 && <Alert severity="info">No projects in this filter.</Alert>}
        </Box>
      )}

      {organization && (
        <CreateProjectDialog
          open={createOpen}
          orgId={organization.id}
          onClose={() => setCreateOpen(false)}
          onCreated={(id) => navigate(`/projects/${id}`)}
        />
      )}
    </Stack>
  );
}
