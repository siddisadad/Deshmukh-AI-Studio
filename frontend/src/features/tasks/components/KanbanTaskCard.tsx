import { Chip, FormControl, MenuItem, Paper, Select, Stack, Typography } from '@mui/material';
import { useDraggable } from '@dnd-kit/core';
import { CSS } from '@dnd-kit/utilities';
import type { Task, TaskStatus } from '../api/tasksApi';

const COLUMNS: { status: TaskStatus; title: string }[] = [
  { status: 'TODO', title: 'To Do' },
  { status: 'IN_PROGRESS', title: 'In Progress' },
  { status: 'REVIEW', title: 'Review' },
  { status: 'DONE', title: 'Done' },
];

interface Props {
  task: Task;
  requirementTitle?: string;
  assigneeName?: string;
  onOpen: (task: Task) => void;
  onStatusChange: (task: Task, status: TaskStatus) => void;
}

export function KanbanTaskCard({ task, requirementTitle, assigneeName, onOpen, onStatusChange }: Props) {
  const { attributes, listeners, setNodeRef, transform, isDragging } = useDraggable({
    id: task.id,
    data: { status: task.status },
  });

  return (
    <Paper
      ref={setNodeRef}
      variant="outlined"
      {...listeners}
      {...attributes}
      sx={{
        p: 1.5,
        cursor: isDragging ? 'grabbing' : 'grab',
        bgcolor: 'background.default',
        opacity: isDragging ? 0.45 : 1,
        transform: CSS.Translate.toString(transform),
        touchAction: 'none',
      }}
      onClick={() => {
        if (!isDragging) {
          onOpen(task);
        }
      }}
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
            label={requirementTitle || 'Requirement'}
            data-testid={`task-requirement-chip-${task.id}`}
          />
        )}
        {task.assigneeId && (
          <Chip
            size="small"
            variant="outlined"
            label={assigneeName || 'Assignee'}
            data-testid={`task-assignee-chip-${task.id}`}
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
          onChange={(e) => onStatusChange(task, e.target.value as TaskStatus)}
          data-testid={`task-status-${task.id}`}
          // Avoid starting a drag when using the status menu.
          onPointerDown={(e) => e.stopPropagation()}
        >
          {COLUMNS.map((c) => (
            <MenuItem key={c.status} value={c.status}>
              Move to {c.title}
            </MenuItem>
          ))}
        </Select>
      </FormControl>
    </Paper>
  );
}
