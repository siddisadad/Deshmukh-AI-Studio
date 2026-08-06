import { Alert, Box, Button, CircularProgress, Paper, Stack, Typography } from '@mui/material';
import { useEffect, useState } from 'react';
import { Link as RouterLink, useParams } from 'react-router-dom';
import { ApiError } from '../../../shared/api/types';
import { projectsApi, type Project } from '../api/projectsApi';

export function ProjectOverviewPage() {
  const { projectId } = useParams();
  const [project, setProject] = useState<Project | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!projectId) return;
    setLoading(true);
    projectsApi
      .getProject(projectId)
      .then(setProject)
      .catch((err) => setError(err instanceof ApiError ? err.message : 'Failed to load project'))
      .finally(() => setLoading(false));
  }, [projectId]);

  if (loading) {
    return (
      <Box sx={{ display: 'grid', placeItems: 'center', py: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error || !project) {
    return <Alert severity="error">{error || 'Project not found'}</Alert>;
  }

  return (
    <Stack spacing={3}>
      <Stack direction={{ xs: 'column', sm: 'row' }} sx={{ justifyContent: 'space-between', gap: 2 }}>
        <Box>
          <Typography variant="overline" color="primary">
            {project.projectKey} · {project.status}
          </Typography>
          <Typography variant="h4">{project.name}</Typography>
          <Typography color="text.secondary" sx={{ mt: 1 }}>
            {project.description || 'No description yet.'}
          </Typography>
        </Box>
        <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap' }}>
          <Button component={RouterLink} to={`/projects/${project.id}/requirements`} variant="contained">
            Requirements
          </Button>
          <Button component={RouterLink} to={`/projects/${project.id}/tasks`} variant="contained" color="secondary">
            Tasks
          </Button>
          <Button component={RouterLink} to={`/projects/${project.id}/chat`} variant="contained">
            AI Chat
          </Button>
          <Button component={RouterLink} to={`/projects/${project.id}/settings`} variant="outlined">
            Settings
          </Button>
        </Stack>
      </Stack>

      <Paper variant="outlined" sx={{ p: 3 }}>
        <Typography variant="h6" gutterBottom>
          Workspace
        </Typography>
        <Typography color="text.secondary">
          Requirements, Kanban, and AI chat with four role assistants are available. Documents come next. Your role:{' '}
          {project.role}.
        </Typography>
      </Paper>
    </Stack>
  );
}
