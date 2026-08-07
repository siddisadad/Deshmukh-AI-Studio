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
  Select,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import {
  DndContext,
  DragOverlay,
  PointerSensor,
  closestCorners,
  useSensor,
  useSensors,
  type DragEndEvent,
  type DragStartEvent,
} from '@dnd-kit/core';
import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react';
import { Link as RouterLink, useParams } from 'react-router-dom';
import { ApiError } from '../../../shared/api/types';
import { EmptyState } from '../../../shared/ui/EmptyState';
import { organizationsApi, type OrgMember } from '../../projects/api/organizationsApi';
import { projectsApi, type Project } from '../../projects/api/projectsApi';
import { requirementsApi, type Requirement } from '../../requirements/api/requirementsApi';
import { tasksApi, type Label, type Task, type TaskStatus } from '../api/tasksApi';
import { AssigneeSelect } from '../components/AssigneeSelect';
import { KanbanColumn } from '../components/KanbanColumn';
import { KanbanTaskCard } from '../components/KanbanTaskCard';
import { LabelMultiSelect } from '../components/LabelMultiSelect';
import { RequirementSelect } from '../components/RequirementSelect';
import { applyTaskMove, resolveDropTarget, toReorderUpdates } from '../kanban/applyTaskMove';

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
  const [members, setMembers] = useState<OrgMember[]>([]);
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
  const [newAssigneeId, setNewAssigneeId] = useState('');
  const [editLabelIds, setEditLabelIds] = useState<string[]>([]);
  const [editRequirementId, setEditRequirementId] = useState('');
  const [editAssigneeId, setEditAssigneeId] = useState('');
  const [labelName, setLabelName] = useState('');
  const [labelColor, setLabelColor] = useState(LABEL_COLORS[0]);
  const [saving, setSaving] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);
  const [editError, setEditError] = useState<string | null>(null);
  const [labelsError, setLabelsError] = useState<string | null>(null);
  const [labelsMessage, setLabelsMessage] = useState<string | null>(null);
  const [activeDragId, setActiveDragId] = useState<string | null>(null);

  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 8 } }));

  const requirementTitleById = useMemo(() => {
    const map = new Map<string, string>();
    for (const req of requirements) {
      map.set(req.id, req.title);
    }
    return map;
  }, [requirements]);

  const memberNameById = useMemo(() => {
    const map = new Map<string, string>();
    for (const member of members) {
      map.set(member.userId, member.displayName);
    }
    return map;
  }, [members]);

  const load = useCallback(async () => {
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
      const orgMembers = await organizationsApi.listMembers(p.organizationId);
      setProject(p);
      setTasks(list);
      setLabels(projectLabels);
      setRequirements(projectRequirements);
      setMembers(orgMembers);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to load tasks');
    } finally {
      setLoading(false);
    }
  }, [projectId]);

  useEffect(() => {
    void load();
  }, [load]);

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
    setNewAssigneeId('');
    setCreateError(null);
    setCreateOpen(true);
  }

  function openEdit(task: Task) {
    setSelected(task);
    setEditLabelIds(task.labels.map((l) => l.id));
    setEditRequirementId(task.requirementId || '');
    setEditAssigneeId(task.assigneeId || '');
    setEditError(null);
  }

  function openLabels() {
    setLabelsError(null);
    setLabelsMessage(null);
    setLabelsOpen(true);
  }

  async function onCreate(e: FormEvent) {
    e.preventDefault();
    if (!projectId || !newTitle.trim()) return;
    setSaving(true);
    setCreateError(null);
    try {
      const created = await tasksApi.create(projectId, {
        title: newTitle.trim(),
        priority: newPriority,
        status: 'TODO',
        labelIds: newLabelIds,
        ...(newRequirementId ? { requirementId: newRequirementId } : {}),
        ...(newAssigneeId ? { assigneeId: newAssigneeId } : {}),
      });
      setTasks((prev) => [...prev, created]);
      setCreateOpen(false);
      setNewTitle('');
      setNewLabelIds([]);
      setNewRequirementId('');
      setNewAssigneeId('');
      setMessage('Task created');
    } catch (err) {
      setCreateError(err instanceof ApiError ? err.message : 'Create failed');
    } finally {
      setSaving(false);
    }
  }

  async function persistBoard(nextTasks: Task[], previous: Task[]) {
    if (!projectId) return;
    setError(null);
    setTasks(nextTasks);
    try {
      const updated = await tasksApi.reorder(projectId, toReorderUpdates(nextTasks));
      setTasks(updated);
      if (selected) {
        const refreshed = updated.find((t) => t.id === selected.id);
        if (refreshed) {
          setSelected(refreshed);
          setEditLabelIds(refreshed.labels.map((l) => l.id));
          setEditRequirementId(refreshed.requirementId || '');
          setEditAssigneeId(refreshed.assigneeId || '');
        }
      }
    } catch (err) {
      setTasks(previous);
      setError(err instanceof ApiError ? err.message : 'Move failed');
    }
  }

  async function moveTask(task: Task, status: TaskStatus) {
    if (task.status === status) return;
    const previous = tasks;
    const destIndex = tasks.filter((t) => t.status === status).length;
    const next = applyTaskMove(tasks, task.id, status, destIndex);
    await persistBoard(next, previous);
  }

  function onDragStart(event: DragStartEvent) {
    setActiveDragId(String(event.active.id));
  }

  async function onDragEnd(event: DragEndEvent) {
    setActiveDragId(null);
    const overId = event.over?.id;
    if (!overId) return;
    const target = resolveDropTarget(
      tasks,
      String(event.active.id),
      String(overId),
      COLUMNS.map((c) => c.status),
    );
    if (!target) return;
    const active = tasks.find((t) => t.id === event.active.id);
    if (!active) return;
    const previous = tasks;
    const next = applyTaskMove(tasks, active.id, target.status, target.index);
    const unchanged = next.every(
      (task) =>
        previous.find((p) => p.id === task.id)?.status === task.status &&
        previous.find((p) => p.id === task.id)?.sortOrder === task.sortOrder,
    );
    if (unchanged) return;
    await persistBoard(next, previous);
  }

  const activeDragTask = activeDragId ? tasks.find((t) => t.id === activeDragId) : null;

  async function saveSelected(e: FormEvent) {
    e.preventDefault();
    if (!selected) return;
    setSaving(true);
    setEditError(null);
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
        ...(editAssigneeId ? { assigneeId: editAssigneeId } : { clearAssigneeId: true }),
      });
      setTasks((prev) => prev.map((t) => (t.id === updated.id ? updated : t)));
      setSelected(updated);
      setEditLabelIds(updated.labels.map((l) => l.id));
      setEditRequirementId(updated.requirementId || '');
      setEditAssigneeId(updated.assigneeId || '');
      setMessage('Task saved');
    } catch (err) {
      setEditError(err instanceof ApiError ? err.message : 'Save failed');
    } finally {
      setSaving(false);
    }
  }

  async function onDeleteSelected() {
    if (!selected) return;
    const confirmed = window.confirm(`Delete task “${selected.title}”? This cannot be undone.`);
    if (!confirmed) return;
    setSaving(true);
    setEditError(null);
    setMessage(null);
    try {
      const deletedId = selected.id;
      await tasksApi.remove(deletedId);
      setTasks((prev) => prev.filter((t) => t.id !== deletedId));
      setSelected(null);
      setMessage('Task deleted');
    } catch (err) {
      setEditError(err instanceof ApiError ? err.message : 'Delete failed');
    } finally {
      setSaving(false);
    }
  }

  async function onCreateLabel(e: FormEvent) {
    e.preventDefault();
    if (!projectId || !labelName.trim()) return;
    setSaving(true);
    setLabelsError(null);
    setLabelsMessage(null);
    try {
      const created = await tasksApi.createLabel(projectId, {
        name: labelName.trim(),
        color: labelColor,
      });
      setLabels((prev) => [...prev, created]);
      setLabelName('');
      setLabelsMessage(`Label “${created.name}” created`);
    } catch (err) {
      setLabelsError(err instanceof ApiError ? err.message : 'Failed to create label');
    } finally {
      setSaving(false);
    }
  }

  async function onDeleteLabel(labelId: string) {
    const label = labels.find((l) => l.id === labelId);
    const confirmed = window.confirm(
      `Delete label “${label?.name || 'this label'}”? It will be removed from all tasks.`,
    );
    if (!confirmed) return;
    setLabelsError(null);
    setLabelsMessage(null);
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
      setLabelsMessage('Label deleted');
    } catch (err) {
      setLabelsError(err instanceof ApiError ? err.message : 'Failed to delete label');
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
          <Button variant="outlined" onClick={openLabels} data-testid="manage-labels-button">
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

      <Typography variant="body2" color="text.secondary">
        Drag cards between columns, or use Move to… for keyboard-friendly status changes.
      </Typography>

      <DndContext
        sensors={sensors}
        collisionDetection={closestCorners}
        onDragStart={onDragStart}
        onDragEnd={(event) => void onDragEnd(event)}
      >
        <Box
          sx={{
            display: 'grid',
            gap: 2,
            gridTemplateColumns: { xs: '1fr', md: 'repeat(4, minmax(0, 1fr))' },
            alignItems: 'start',
          }}
          data-testid="kanban-board"
        >
          {COLUMNS.map((column) => (
            <KanbanColumn
              key={column.status}
              status={column.status}
              title={column.title}
              count={byStatus[column.status].length}
            >
              {byStatus[column.status].map((task) => (
                <KanbanTaskCard
                  key={task.id}
                  task={task}
                  requirementTitle={
                    task.requirementId ? requirementTitleById.get(task.requirementId) : undefined
                  }
                  assigneeName={task.assigneeId ? memberNameById.get(task.assigneeId) : undefined}
                  onOpen={openEdit}
                  onStatusChange={(t, status) => void moveTask(t, status)}
                />
              ))}
            </KanbanColumn>
          ))}
        </Box>
        <DragOverlay>
          {activeDragTask ? (
            <KanbanTaskCard
              task={activeDragTask}
              requirementTitle={
                activeDragTask.requirementId
                  ? requirementTitleById.get(activeDragTask.requirementId)
                  : undefined
              }
              assigneeName={
                activeDragTask.assigneeId ? memberNameById.get(activeDragTask.assigneeId) : undefined
              }
              onOpen={() => undefined}
              onStatusChange={() => undefined}
            />
          ) : null}
        </DragOverlay>
      </DndContext>

      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} fullWidth maxWidth="sm">
        <Box component="form" onSubmit={onCreate}>
          <DialogTitle>New task</DialogTitle>
          <DialogContent>
            <Stack spacing={2} sx={{ mt: 1 }}>
              {createError && <Alert severity="error">{createError}</Alert>}
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
              <AssigneeSelect members={members} value={newAssigneeId} onChange={setNewAssigneeId} />
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
                {editError && <Alert severity="error">{editError}</Alert>}
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
                <AssigneeSelect
                  members={members}
                  value={editAssigneeId}
                  onChange={setEditAssigneeId}
                  testId="edit-assignee-select"
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
            {labelsError && <Alert severity="error">{labelsError}</Alert>}
            {labelsMessage && <Alert severity="success">{labelsMessage}</Alert>}
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
