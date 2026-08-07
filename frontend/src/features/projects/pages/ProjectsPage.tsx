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
import { EmptyState } from '../../../shared/ui/EmptyState';
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
          <Button variant="contained" onClick={() => setCreateOpen(true)} data-testid="new-project-button">
            New project
          </Button>
        </Stack>
      </Stack>

      {error && <Alert severity="error">{error}</Alert>}
      {loading ? (
        <Box sx={{ display: 'grid', placeItems: 'center', py: 6 }}>
          <CircularProgress />
        </Box>
      ) : projects.length === 0 ? (
        <EmptyState
          title={status === 'ACTIVE' ? 'No active projects' : 'Nothing in this filter'}
          description={
            status === 'ACTIVE'
              ? 'Start with a project so requirements, tasks, documents, and AI assistants share one workspace context.'
              : 'Try another filter, or create a new project.'
          }
          actionLabel="New project"
          onAction={() => setCreateOpen(true)}
        />
      ) : (
        <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' } }}>
          {projects.map((project) => (
            <Card key={project.id} variant="outlined" data-testid={`project-card-${project.projectKey}`}>
              <CardActionArea onClick={() => navigate(`/projects/${project.id}`)}>
                <CardContent>
                  <Typography variant="overline" color="primary">
                    {project.projectKey} · {project.status}
                  </Typography>
                  <Typography variant="h6" data-testid="project-card-name">
                    {project.name}
                  </Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                    {project.description || 'No description'}
                  </Typography>
                </CardContent>
              </CardActionArea>
            </Card>
          ))}
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
