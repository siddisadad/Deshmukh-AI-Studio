import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ApiError } from '../../../shared/api/types';
import { projectsApi, type Project } from '../api/projectsApi';

export function ProjectSettingsPage() {
  const { projectId } = useParams();
  const navigate = useNavigate();
  const [project, setProject] = useState<Project | null>(null);
  const [name, setName] = useState('');
  const [projectKey, setProjectKey] = useState('');
  const [description, setDescription] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!projectId) return;
    setLoading(true);
    projectsApi
      .getProject(projectId)
      .then((p) => {
        setProject(p);
        setName(p.name);
        setProjectKey(p.projectKey);
        setDescription(p.description || '');
      })
      .catch((err) => setError(err instanceof ApiError ? err.message : 'Failed to load project'))
      .finally(() => setLoading(false));
  }, [projectId]);

  async function onSave(e: FormEvent) {
    e.preventDefault();
    if (!projectId) return;
    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      const updated = await projectsApi.updateProject(projectId, {
        name,
        projectKey,
        description,
      });
      setProject(updated);
      setMessage('Project updated');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Update failed');
    } finally {
      setSaving(false);
    }
  }

  async function onArchiveToggle() {
    if (!projectId || !project) return;
    setSaving(true);
    setError(null);
    try {
      const updated =
        project.status === 'ARCHIVED'
          ? await projectsApi.unarchiveProject(projectId)
          : await projectsApi.archiveProject(projectId);
      setProject(updated);
      setMessage(updated.status === 'ARCHIVED' ? 'Project archived' : 'Project restored');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Archive action failed');
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return (
      <Box sx={{ display: 'grid', placeItems: 'center', py: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (!project) {
    return <Alert severity="error">{error || 'Project not found'}</Alert>;
  }

  return (
    <Stack spacing={3} sx={{ maxWidth: 640 }}>
      <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h4">Project settings</Typography>
        <Button onClick={() => navigate(`/projects/${project.id}`)}>Back</Button>
      </Stack>

      <Paper component="form" onSubmit={onSave} variant="outlined" sx={{ p: 3 }}>
        <Stack spacing={2}>
          {error && <Alert severity="error">{error}</Alert>}
          {message && <Alert severity="success">{message}</Alert>}
          <TextField label="Name" value={name} onChange={(e) => setName(e.target.value)} required />
          <TextField label="Key" value={projectKey} onChange={(e) => setProjectKey(e.target.value.toUpperCase())} required />
          <TextField
            label="Description"
            multiline
            minRows={3}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />
          <Button type="submit" variant="contained" disabled={saving}>
            Save changes
          </Button>
        </Stack>
      </Paper>

      <Paper variant="outlined" sx={{ p: 3 }}>
        <Typography variant="h6" gutterBottom>
          Archive
        </Typography>
        <Typography color="text.secondary" sx={{ mb: 2 }}>
          Archived projects are hidden from the default dashboard.
        </Typography>
        <Button color={project.status === 'ARCHIVED' ? 'primary' : 'warning'} variant="outlined" onClick={() => void onArchiveToggle()} disabled={saving}>
          {project.status === 'ARCHIVED' ? 'Restore project' : 'Archive project'}
        </Button>
      </Paper>
    </Stack>
  );
}
