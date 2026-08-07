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
import { requirementsApi, type Requirement } from '../../requirements/api/requirementsApi';
import { tasksApi, type Label, type Task, type TaskStatus } from '../api/tasksApi';
import { LabelMultiSelect } from '../components/LabelMultiSelect';
import { RequirementSelect } from '../components/RequirementSelect';

const COLUMNS: { status: TaskStatus; title: string }[] = [
  { status: 'TODO', title: 'To Do' },
  { status: 'IN_PROGRESS', title: 'In Progress' },
  { status: 'REVIEW', title: 'Review' },
  { status: 'DONE', title: 'Done' },
];

const LABEL_COLORS = ['#0D9488', '#2563EB', '#DC2626', '#CA8A04', '#7C3AED', '#475569'];

export function TasksPage() {
  const { projectId } = useParams();
  const [project, setProject] = useState<Project | null>(null);
  const [tasks, setTasks] = useState<Task[]>([]);
  const [labels, setLabels] = useState<Label[]>([]);
  const [requirements, setRequirements] = useState<Requirement[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [createOpen, setCreateOpen] = useState(false);
  const [labelsOpen, setLabelsOpen] = useState(false);
  const [selected, setSelected] = useState<Task | null>(null);
  const [newTitle, setNewTitle] = useState('');
  const [newPriority, setNewPriority] = useState('MEDIUM');
  const [newLabelIds, setNewLabelIds] = useState<string[]>([]);
  const [newRequirementId, setNewRequirementId] = useState('');
  const [editLabelIds, setEditLabelIds] = useState<string[]>([]);
  const [editRequirementId, setEditRequirementId] = useState('');
  const [labelName, setLabelName] = useState('');
  const [labelColor, setLabelColor] = useState(LABEL_COLORS[0]);
  const [saving, setSaving] = useState(false);

  const requirementTitleById = useMemo(() => {
    const map = new Map<string, string>();
    for (const req of requirements) {
      map.set(req.id, req.title);
    }
    return map;
  }, [requirements]);

  async function load() {
    if (!projectId) return;
    setLoading(true);
    setError(null);
    try {
      const [p, list, projectLabels, projectRequirements] = await Promise.all([
        projectsApi.getProject(projectId),
        tasksApi.list(projectId),
        tasksApi.listLabels(projectId),
        requirementsApi.list(projectId),
      ]);
      setProject(p);
      setTasks(list);
      setLabels(projectLabels);
      setRequirements(projectRequirements);
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

  function openCreate() {
    setNewTitle('');
    setNewPriority('MEDIUM');
    setNewLabelIds([]);
    setNewRequirementId('');
    setCreateOpen(true);
  }

  function openEdit(task: Task) {
    setSelected(task);
    setEditLabelIds(task.labels.map((l) => l.id));
    setEditRequirementId(task.requirementId || '');
  }

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
        labelIds: newLabelIds,
        ...(newRequirementId ? { requirementId: newRequirementId } : {}),
      });
      setTasks((prev) => [...prev, created]);
      setCreateOpen(false);
      setNewTitle('');
      setNewLabelIds([]);
      setNewRequirementId('');
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
      if (selected?.id === updated.id) {
        setSelected(updated);
        setEditLabelIds(updated.labels.map((l) => l.id));
        setEditRequirementId(updated.requirementId || '');
      }
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
    setMessage(null);
    try {
      const updated = await tasksApi.update(selected.id, {
        title: selected.title,
        description: selected.description || '',
        priority: selected.priority,
        status: selected.status,
        labelIds: editLabelIds,
        ...(editRequirementId
          ? { requirementId: editRequirementId }
          : { clearRequirementId: true }),
      });
      setTasks((prev) => prev.map((t) => (t.id === updated.id ? updated : t)));
      setSelected(updated);
      setEditLabelIds(updated.labels.map((l) => l.id));
      setEditRequirementId(updated.requirementId || '');
      setMessage('Task saved');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Save failed');
    } finally {
      setSaving(false);
    }
  }

  async function onDeleteSelected() {
    if (!selected) return;
    const confirmed = window.confirm(`Delete task “${selected.title}”? This cannot be undone.`);
    if (!confirmed) return;
    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      const deletedId = selected.id;
      await tasksApi.remove(deletedId);
      setTasks((prev) => prev.filter((t) => t.id !== deletedId));
      setSelected(null);
      setMessage('Task deleted');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Delete failed');
    } finally {
      setSaving(false);
    }
  }

  async function onCreateLabel(e: FormEvent) {
    e.preventDefault();
    if (!projectId || !labelName.trim()) return;
    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      const created = await tasksApi.createLabel(projectId, {
        name: labelName.trim(),
        color: labelColor,
      });
      setLabels((prev) => [...prev, created]);
      setLabelName('');
      setMessage(`Label “${created.name}” created`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to create label');
    } finally {
      setSaving(false);
    }
  }

  async function onDeleteLabel(labelId: string) {
    setError(null);
    setMessage(null);
    try {
      await tasksApi.deleteLabel(labelId);
      setLabels((prev) => prev.filter((l) => l.id !== labelId));
      setNewLabelIds((prev) => prev.filter((id) => id !== labelId));
      setEditLabelIds((prev) => prev.filter((id) => id !== labelId));
      setTasks((prev) =>
        prev.map((task) => ({
          ...task,
          labels: task.labels.filter((l) => l.id !== labelId),
        })),
      );
      if (selected) {
        setSelected({
          ...selected,
          labels: selected.labels.filter((l) => l.id !== labelId),
        });
      }
      setMessage('Label deleted');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to delete label');
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
        <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap' }}>
          <Button component={RouterLink} to={`/projects/${projectId}`} variant="outlined">
            Overview
          </Button>
          <Button variant="outlined" onClick={() => setLabelsOpen(true)} data-testid="manage-labels-button">
            Labels
          </Button>
          <Button variant="contained" onClick={openCreate} data-testid="new-task-button">
            New task
          </Button>
        </Stack>
      </Stack>

      {error && <Alert severity="error">{error}</Alert>}
      {message && <Alert severity="success">{message}</Alert>}

      {tasks.length === 0 && (
        <EmptyState
          title="Board is empty"
          description="Break work into tasks and move them across To Do → Done as the team delivers."
          actionLabel="New task"
          onAction={openCreate}
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
          <Paper
            key={column.status}
            variant="outlined"
            sx={{ p: 1.5, minHeight: 320 }}
            data-testid={`task-column-${column.status}`}
          >
            <Typography variant="subtitle2" sx={{ mb: 1.5, px: 0.5 }}>
              {column.title} · {byStatus[column.status].length}
            </Typography>
            <Stack spacing={1}>
              {byStatus[column.status].map((task) => (
                <Paper
                  key={task.id}
                  variant="outlined"
                  sx={{ p: 1.5, cursor: 'pointer', bgcolor: 'background.default' }}
                  onClick={() => openEdit(task)}
                  data-testid={`task-card-${task.id}`}
                >
                  <Typography variant="subtitle2" data-testid="task-card-title">
                    {task.title}
                  </Typography>
                  <Stack direction="row" spacing={0.5} sx={{ mt: 1, flexWrap: 'wrap' }}>
                    <Chip size="small" label={task.priority} />
                    {task.requirementId && (
                      <Chip
                        size="small"
                        variant="outlined"
                        label={requirementTitleById.get(task.requirementId) || 'Requirement'}
                        data-testid={`task-requirement-chip-${task.id}`}
                      />
                    )}
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
                      data-testid={`task-status-${task.id}`}
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
              <TextField
                label="Title"
                required
                value={newTitle}
                onChange={(e) => setNewTitle(e.target.value)}
                slotProps={{ htmlInput: { 'data-testid': 'task-title-input' } }}
              />
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
              <RequirementSelect
                requirements={requirements}
                value={newRequirementId}
                onChange={setNewRequirementId}
              />
              <LabelMultiSelect labels={labels} value={newLabelIds} onChange={setNewLabelIds} />
            </Stack>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setCreateOpen(false)}>Cancel</Button>
            <Button type="submit" variant="contained" disabled={saving} data-testid="task-create-submit">
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
                <RequirementSelect
                  requirements={requirements}
                  value={editRequirementId}
                  onChange={setEditRequirementId}
                  testId="edit-requirement-select"
                />
                <LabelMultiSelect
                  labels={labels}
                  value={editLabelIds}
                  onChange={setEditLabelIds}
                  testId="edit-label-multi-select"
                />
              </Stack>
            </DialogContent>
            <DialogActions sx={{ justifyContent: 'space-between' }}>
              <Button
                color="error"
                onClick={() => void onDeleteSelected()}
                disabled={saving}
                data-testid="task-delete-button"
              >
                Delete
              </Button>
              <Stack direction="row" spacing={1}>
                <Button onClick={() => setSelected(null)}>Close</Button>
                <Button type="submit" variant="contained" disabled={saving} data-testid="task-save-submit">
                  Save
                </Button>
              </Stack>
            </DialogActions>
          </Box>
        )}
      </Dialog>

      <Dialog open={labelsOpen} onClose={() => setLabelsOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>Project labels</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap' }} useFlexGap>
              {labels.map((label) => (
                <Chip
                  key={label.id}
                  label={label.name}
                  onDelete={() => void onDeleteLabel(label.id)}
                  sx={{ bgcolor: label.color, color: '#fff' }}
                  data-testid={`label-chip-${label.id}`}
                />
              ))}
              {labels.length === 0 && (
                <Typography variant="body2" color="text.secondary">
                  No labels yet. Create one to tag tasks on the board.
                </Typography>
              )}
            </Stack>
            <Box component="form" onSubmit={onCreateLabel}>
              <Stack spacing={2}>
                <TextField
                  label="Label name"
                  required
                  value={labelName}
                  onChange={(e) => setLabelName(e.target.value)}
                  slotProps={{ htmlInput: { 'data-testid': 'label-name-input' } }}
                />
                <FormControl fullWidth>
                  <InputLabel id="label-color">Color</InputLabel>
                  <Select
                    labelId="label-color"
                    label="Color"
                    value={labelColor}
                    onChange={(e) => setLabelColor(e.target.value)}
                    data-testid="label-color-select"
                  >
                    {LABEL_COLORS.map((color) => (
                      <MenuItem key={color} value={color}>
                        <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                          <Box sx={{ width: 16, height: 16, borderRadius: 0.5, bgcolor: color }} />
                          <span>{color}</span>
                        </Stack>
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
                <Button type="submit" variant="contained" disabled={saving} data-testid="label-create-submit">
                  Create label
                </Button>
              </Stack>
            </Box>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setLabelsOpen(false)}>Done</Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}
