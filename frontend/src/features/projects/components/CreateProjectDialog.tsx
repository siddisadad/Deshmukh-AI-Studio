import {
  Alert,
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Stack,
  TextField,
} from '@mui/material';
import { useState, type FormEvent } from 'react';
import { ApiError } from '../../../shared/api/types';
import { projectsApi } from '../api/projectsApi';

interface Props {
  open: boolean;
  orgId: string;
  onClose: () => void;
  onCreated: (projectId: string) => void;
}

export function CreateProjectDialog({ open, orgId, onClose, onCreated }: Props) {
  const [name, setName] = useState('');
  const [projectKey, setProjectKey] = useState('');
  const [description, setDescription] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const project = await projectsApi.createProject(orgId, {
        name,
        projectKey: projectKey.toUpperCase(),
        description: description || undefined,
      });
      setName('');
      setProjectKey('');
      setDescription('');
      onCreated(project.id);
      onClose();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to create project');
    } finally {
      setLoading(false);
    }
  }

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <Box component="form" onSubmit={onSubmit}>
        <DialogTitle>New project</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            {error && <Alert severity="error">{error}</Alert>}
            <TextField label="Name" required value={name} onChange={(e) => setName(e.target.value)} />
            <TextField
              label="Key"
              required
              helperText="2–10 uppercase letters/numbers, e.g. CP"
              value={projectKey}
              onChange={(e) => setProjectKey(e.target.value.toUpperCase())}
            />
            <TextField
              label="Description"
              multiline
              minRows={3}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose}>Cancel</Button>
          <Button type="submit" variant="contained" disabled={loading}>
            {loading ? 'Creating…' : 'Create'}
          </Button>
        </DialogActions>
      </Box>
    </Dialog>
  );
}
