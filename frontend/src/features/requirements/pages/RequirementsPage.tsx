import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Divider,
  FormControl,
  InputLabel,
  List,
  ListItemButton,
  ListItemText,
  MenuItem,
  Paper,
  Select,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { useEffect, useState, type FormEvent } from 'react';
import { Link as RouterLink, useParams } from 'react-router-dom';
import { ApiError } from '../../../shared/api/types';
import { EmptyState } from '../../../shared/ui/EmptyState';
import { projectsApi, type Project } from '../../projects/api/projectsApi';
import { requirementsApi, type Requirement } from '../api/requirementsApi';

export function RequirementsPage() {
  const { projectId } = useParams();
  const [project, setProject] = useState<Project | null>(null);
  const [requirements, setRequirements] = useState<Requirement[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [aiLoading, setAiLoading] = useState<string | null>(null);

  const selected = requirements.find((r) => r.id === selectedId) || null;

  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [priority, setPriority] = useState('MEDIUM');
  const [status, setStatus] = useState('DRAFT');
  const [improvedDescription, setImprovedDescription] = useState('');
  const [userStories, setUserStories] = useState('');
  const [acceptanceCriteria, setAcceptanceCriteria] = useState('');
  const [newTitle, setNewTitle] = useState('');

  async function load() {
    if (!projectId) return;
    setLoading(true);
    setError(null);
    try {
      const [p, list] = await Promise.all([
        projectsApi.getProject(projectId),
        requirementsApi.list(projectId),
      ]);
      setProject(p);
      setRequirements(list);
      if (list.length && !selectedId) {
        setSelectedId(list[0].id);
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to load requirements');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [projectId]);

  useEffect(() => {
    if (!selected) {
      setTitle('');
      setDescription('');
      setPriority('MEDIUM');
      setStatus('DRAFT');
      setImprovedDescription('');
      setUserStories('');
      setAcceptanceCriteria('');
      return;
    }
    setTitle(selected.title);
    setDescription(selected.description || '');
    setPriority(selected.priority);
    setStatus(selected.status);
    setImprovedDescription(selected.improvedDescription || '');
    setUserStories(selected.userStories || '');
    setAcceptanceCriteria(selected.acceptanceCriteria || '');
  }, [selected]);

  async function onCreate(e: FormEvent) {
    e.preventDefault();
    if (!projectId || !newTitle.trim()) return;
    setSaving(true);
    setError(null);
    try {
      const created = await requirementsApi.create(projectId, { title: newTitle.trim(), description: '' });
      setNewTitle('');
      setRequirements((prev) => [...prev, created]);
      setSelectedId(created.id);
      setMessage('Requirement created');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Create failed');
    } finally {
      setSaving(false);
    }
  }

  async function onSave(e: FormEvent) {
    e.preventDefault();
    if (!selected) return;
    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      const updated = await requirementsApi.update(selected.id, {
        title,
        description,
        priority,
        status,
        improvedDescription,
        userStories,
        acceptanceCriteria,
      });
      setRequirements((prev) => prev.map((r) => (r.id === updated.id ? updated : r)));
      setMessage('Requirement saved');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Save failed');
    } finally {
      setSaving(false);
    }
  }

  async function runAi(action: 'improve' | 'userStories' | 'acceptanceCriteria') {
    if (!selected) return;
    setAiLoading(action);
    setError(null);
    setMessage(null);
    try {
      const result =
        action === 'improve'
          ? await requirementsApi.improve(selected.id)
          : action === 'userStories'
            ? await requirementsApi.userStories(selected.id)
            : await requirementsApi.acceptanceCriteria(selected.id);
      setRequirements((prev) => prev.map((r) => (r.id === result.requirement.id ? result.requirement : r)));
      setMessage(`AI ${action} completed via ${result.provider}`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'AI action failed');
    } finally {
      setAiLoading(null);
    }
  }

  async function onDelete() {
    if (!selected) return;
    const confirmed = window.confirm(`Delete requirement “${selected.title}”? This cannot be undone.`);
    if (!confirmed) return;
    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      const deletedId = selected.id;
      await requirementsApi.remove(deletedId);
      const remaining = requirements.filter((r) => r.id !== deletedId);
      setRequirements(remaining);
      setSelectedId(remaining[0]?.id ?? null);
      setMessage('Requirement deleted');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Delete failed');
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

  return (
    <Stack spacing={3}>
      <Stack direction={{ xs: 'column', sm: 'row' }} sx={{ justifyContent: 'space-between', gap: 2 }}>
        <Box>
          <Typography variant="overline" color="primary">
            {project?.projectKey} · Requirements
          </Typography>
          <Typography variant="h4">{project?.name}</Typography>
        </Box>
        <Button component={RouterLink} to={`/projects/${projectId}`} variant="outlined">
          Project overview
        </Button>
      </Stack>

      {error && <Alert severity="error">{error}</Alert>}
      {message && <Alert severity="success">{message}</Alert>}

      <Box
        sx={{
          display: 'grid',
          gap: 2,
          gridTemplateColumns: { xs: '1fr', md: '340px 1fr' },
          alignItems: 'start',
        }}
      >
        <Paper variant="outlined" sx={{ p: 2 }}>
          <Box component="form" onSubmit={onCreate} sx={{ mb: 2 }}>
            <Stack spacing={1}>
              <TextField
                size="small"
                label="New requirement"
                value={newTitle}
                onChange={(e) => setNewTitle(e.target.value)}
                slotProps={{ htmlInput: { 'data-testid': 'requirement-title-input' } }}
              />
              <Button
                type="submit"
                variant="contained"
                disabled={saving || !newTitle.trim()}
                data-testid="requirement-add-button"
              >
                Add
              </Button>
            </Stack>
          </Box>
          <List dense>
            {requirements.map((req) => (
              <ListItemButton
                key={req.id}
                selected={req.id === selectedId}
                onClick={() => setSelectedId(req.id)}
                data-testid={`requirement-item-${req.id}`}
              >
                <ListItemText
                  primary={<span data-testid="requirement-item-title">{req.title}</span>}
                  secondary={`${req.priority} · ${req.status}`}
                />
              </ListItemButton>
            ))}
            {requirements.length === 0 && (
              <Box sx={{ px: 1, pb: 1 }}>
                <EmptyState
                  title="Capture the first requirement"
                  description="Write what should be built, then use BA AI to improve wording, stories, and acceptance criteria."
                />
              </Box>
            )}
          </List>
        </Paper>

        <Paper variant="outlined" sx={{ p: 3 }}>
          {!selected ? (
            <EmptyState
              title="Select a requirement"
              description="Pick one from the list, or add a title on the left to create your first draft."
            />
          ) : (
            <Box component="form" onSubmit={onSave}>
              <Stack spacing={2}>
                <TextField label="Title" value={title} onChange={(e) => setTitle(e.target.value)} required />
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                  <FormControl fullWidth>
                    <InputLabel id="priority-label">Priority</InputLabel>
                    <Select
                      labelId="priority-label"
                      label="Priority"
                      value={priority}
                      onChange={(e) => setPriority(e.target.value)}
                    >
                      {['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'].map((v) => (
                        <MenuItem key={v} value={v}>
                          {v}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                  <FormControl fullWidth>
                    <InputLabel id="status-label">Status</InputLabel>
                    <Select
                      labelId="status-label"
                      label="Status"
                      value={status}
                      onChange={(e) => setStatus(e.target.value)}
                    >
                      {['DRAFT', 'READY', 'IN_PROGRESS', 'DONE', 'DEPRECATED'].map((v) => (
                        <MenuItem key={v} value={v}>
                          {v}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                </Stack>
                <TextField
                  label="Description"
                  multiline
                  minRows={5}
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                />

                <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap' }} useFlexGap>
                  <Button
                    variant="outlined"
                    disabled={!!aiLoading}
                    onClick={() => void runAi('improve')}
                  >
                    {aiLoading === 'improve' ? 'Improving…' : 'Improve'}
                  </Button>
                  <Button
                    variant="outlined"
                    disabled={!!aiLoading}
                    onClick={() => void runAi('userStories')}
                  >
                    {aiLoading === 'userStories' ? 'Generating…' : 'User stories'}
                  </Button>
                  <Button
                    variant="outlined"
                    disabled={!!aiLoading}
                    onClick={() => void runAi('acceptanceCriteria')}
                  >
                    {aiLoading === 'acceptanceCriteria' ? 'Generating…' : 'Acceptance criteria'}
                  </Button>
                </Stack>

                <Divider />
                <Typography variant="subtitle2" color="text.secondary">
                  AI-generated — review before use
                </Typography>
                <TextField
                  label="Improved description"
                  multiline
                  minRows={4}
                  value={improvedDescription}
                  onChange={(e) => setImprovedDescription(e.target.value)}
                />
                <TextField
                  label="User stories"
                  multiline
                  minRows={4}
                  value={userStories}
                  onChange={(e) => setUserStories(e.target.value)}
                />
                <TextField
                  label="Acceptance criteria"
                  multiline
                  minRows={4}
                  value={acceptanceCriteria}
                  onChange={(e) => setAcceptanceCriteria(e.target.value)}
                />
                <Stack direction="row" spacing={1} sx={{ justifyContent: 'space-between' }}>
                  <Button
                    color="error"
                    onClick={() => void onDelete()}
                    disabled={saving || !!aiLoading}
                    data-testid="requirement-delete-button"
                  >
                    Delete
                  </Button>
                  <Button type="submit" variant="contained" disabled={saving} data-testid="requirement-save-submit">
                    Save requirement
                  </Button>
                </Stack>
              </Stack>
            </Box>
          )}
        </Paper>
      </Box>
    </Stack>
  );
}
