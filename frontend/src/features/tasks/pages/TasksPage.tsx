import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { Link as RouterLink, useParams } from 'react-router-dom';
import { ApiError } from '../../../shared/api/types';
import { EmptyState } from '../../../shared/ui/EmptyState';
import { projectsApi, type Project } from '../../projects/api/projectsApi';
import { tasksApi, type Task, type TaskStatus } from '../api/tasksApi';

const COLUMNS: { status: TaskStatus; title: string }[] = [
  { status: 'TODO', title: 'To Do' },
  { status: 'IN_PROGRESS', title: 'In Progress' },
  { status: 'REVIEW', title: 'Review' },
  { status: 'DONE', title: 'Done' },
];

export function TasksPage() {
  const { projectId } = useParams();
  const [project, setProject] = useState<Project | null>(null);
  const [tasks, setTasks] = useState<Task[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [createOpen, setCreateOpen] = useState(false);
  const [selected, setSelected] = useState<Task | null>(null);
  const [newTitle, setNewTitle] = useState('');
  const [newPriority, setNewPriority] = useState('MEDIUM');
  const [saving, setSaving] = useState(false);

  async function load() {
    if (!projectId) return;
    setLoading(true);
    setError(null);
    try {
      const [p, list] = await Promise.all([projectsApi.getProject(projectId), tasksApi.list(projectId)]);
      setProject(p);
      setTasks(list);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to load tasks');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
  }, [projectId]);

  const byStatus = useMemo(() => {
    const map: Record<TaskStatus, Task[]> = {
      TODO: [],
      IN_PROGRESS: [],
      REVIEW: [],
      DONE: [],
    };
    for (const task of tasks) {
      map[task.status]?.push(task);
    }
    for (const key of Object.keys(map) as TaskStatus[]) {
      map[key].sort((a, b) => a.sortOrder - b.sortOrder);
    }
    return map;
  }, [tasks]);

  async function onCreate(e: FormEvent) {
    e.preventDefault();
    if (!projectId || !newTitle.trim()) return;
    setSaving(true);
    setError(null);
    try {
      const created = await tasksApi.create(projectId, {
        title: newTitle.trim(),
        priority: newPriority,
        status: 'TODO',
      });
      setTasks((prev) => [...prev, created]);
      setNewTitle('');
      setCreateOpen(false);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Create failed');
    } finally {
      setSaving(false);
    }
  }

  async function moveTask(task: Task, status: TaskStatus) {
    setError(null);
    const previous = tasks;
    setTasks((prev) => prev.map((t) => (t.id === task.id ? { ...t, status } : t)));
    try {
      const updated = await tasksApi.update(task.id, { status });
      setTasks((prev) => prev.map((t) => (t.id === updated.id ? updated : t)));
      if (selected?.id === updated.id) setSelected(updated);
    } catch (err) {
      setTasks(previous);
      setError(err instanceof ApiError ? err.message : 'Move failed');
    }
  }

  async function saveSelected(e: FormEvent) {
    e.preventDefault();
    if (!selected) return;
    setSaving(true);
    setError(null);
    try {
      const updated = await tasksApi.update(selected.id, {
        title: selected.title,
        description: selected.description || '',
        priority: selected.priority,
        status: selected.status,
      });
      setTasks((prev) => prev.map((t) => (t.id === updated.id ? updated : t)));
      setSelected(updated);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Save failed');
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
            {project?.projectKey} · Tasks
          </Typography>
          <Typography variant="h4">{project?.name}</Typography>
        </Box>
        <Stack direction="row" spacing={1}>
          <Button component={RouterLink} to={`/projects/${projectId}`} variant="outlined">
            Overview
          </Button>
          <Button variant="contained" onClick={() => setCreateOpen(true)}>
            New task
          </Button>
        </Stack>
      </Stack>

      {error && <Alert severity="error">{error}</Alert>}

      {tasks.length === 0 && (
        <EmptyState
          title="Board is empty"
          description="Break work into tasks and move them across To Do → Done as the team delivers."
          actionLabel="New task"
          onAction={() => setCreateOpen(true)}
        />
      )}

      <Box
        sx={{
          display: 'grid',
          gap: 2,
          gridTemplateColumns: { xs: '1fr', md: 'repeat(4, minmax(0, 1fr))' },
          alignItems: 'start',
        }}
      >
        {COLUMNS.map((column) => (
          <Paper key={column.status} variant="outlined" sx={{ p: 1.5, minHeight: 320 }}>
            <Typography variant="subtitle2" sx={{ mb: 1.5, px: 0.5 }}>
              {column.title} · {byStatus[column.status].length}
            </Typography>
            <Stack spacing={1}>
              {byStatus[column.status].map((task) => (
                <Paper
                  key={task.id}
                  variant="outlined"
                  sx={{ p: 1.5, cursor: 'pointer', bgcolor: 'background.default' }}
                  onClick={() => setSelected(task)}
                >
                  <Typography variant="subtitle2">{task.title}</Typography>
                  <Stack direction="row" spacing={0.5} sx={{ mt: 1, flexWrap: 'wrap' }}>
                    <Chip size="small" label={task.priority} />
                    {task.labels.map((label) => (
                      <Chip
                        key={label.id}
                        size="small"
                        label={label.name}
                        sx={{ bgcolor: label.color, color: '#fff' }}
                      />
                    ))}
                  </Stack>
                  <FormControl size="small" fullWidth sx={{ mt: 1.5 }} onClick={(e) => e.stopPropagation()}>
                    <Select
                      value={task.status}
                      onChange={(e) => void moveTask(task, e.target.value as TaskStatus)}
                    >
                      {COLUMNS.map((c) => (
                        <MenuItem key={c.status} value={c.status}>
                          Move to {c.title}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                </Paper>
              ))}
            </Stack>
          </Paper>
        ))}
      </Box>

      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} fullWidth maxWidth="sm">
        <Box component="form" onSubmit={onCreate}>
          <DialogTitle>New task</DialogTitle>
          <DialogContent>
            <Stack spacing={2} sx={{ mt: 1 }}>
              <TextField label="Title" required value={newTitle} onChange={(e) => setNewTitle(e.target.value)} />
              <FormControl fullWidth>
                <InputLabel id="new-priority">Priority</InputLabel>
                <Select
                  labelId="new-priority"
                  label="Priority"
                  value={newPriority}
                  onChange={(e) => setNewPriority(e.target.value)}
                >
                  {['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'].map((p) => (
                    <MenuItem key={p} value={p}>
                      {p}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Stack>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setCreateOpen(false)}>Cancel</Button>
            <Button type="submit" variant="contained" disabled={saving}>
              Create
            </Button>
          </DialogActions>
        </Box>
      </Dialog>

      <Dialog open={!!selected} onClose={() => setSelected(null)} fullWidth maxWidth="sm">
        {selected && (
          <Box component="form" onSubmit={saveSelected}>
            <DialogTitle>Edit task</DialogTitle>
            <DialogContent>
              <Stack spacing={2} sx={{ mt: 1 }}>
                <TextField
                  label="Title"
                  value={selected.title}
                  onChange={(e) => setSelected({ ...selected, title: e.target.value })}
                  required
                />
                <TextField
                  label="Description"
                  multiline
                  minRows={4}
                  value={selected.description || ''}
                  onChange={(e) => setSelected({ ...selected, description: e.target.value })}
                />
                <FormControl fullWidth>
                  <InputLabel id="edit-priority">Priority</InputLabel>
                  <Select
                    labelId="edit-priority"
                    label="Priority"
                    value={selected.priority}
                    onChange={(e) => setSelected({ ...selected, priority: e.target.value })}
                  >
                    {['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'].map((p) => (
                      <MenuItem key={p} value={p}>
                        {p}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
                <FormControl fullWidth>
                  <InputLabel id="edit-status">Status</InputLabel>
                  <Select
                    labelId="edit-status"
                    label="Status"
                    value={selected.status}
                    onChange={(e) => setSelected({ ...selected, status: e.target.value as TaskStatus })}
                  >
                    {COLUMNS.map((c) => (
                      <MenuItem key={c.status} value={c.status}>
                        {c.title}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Stack>
            </DialogContent>
            <DialogActions>
              <Button onClick={() => setSelected(null)}>Close</Button>
              <Button type="submit" variant="contained" disabled={saving}>
                Save
              </Button>
            </DialogActions>
          </Box>
        )}
      </Dialog>
    </Stack>
  );
}
