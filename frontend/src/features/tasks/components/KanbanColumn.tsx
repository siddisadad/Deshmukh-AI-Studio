import { Paper, Stack, Typography } from '@mui/material';
import { useDroppable } from '@dnd-kit/core';
import type { ReactNode } from 'react';
import type { TaskStatus } from '../api/tasksApi';

interface Props {
  status: TaskStatus;
  title: string;
  count: number;
  children: ReactNode;
}

export function KanbanColumn({ status, title, count, children }: Props) {
  const { setNodeRef, isOver } = useDroppable({ id: status });

  return (
    <Paper
      ref={setNodeRef}
      variant="outlined"
      sx={{
        p: 1.5,
        minHeight: 320,
        outline: isOver ? '2px solid' : 'none',
        outlineColor: 'primary.main',
        bgcolor: isOver ? 'action.hover' : undefined,
      }}
      data-testid={`task-column-${status}`}
    >
      <Typography variant="subtitle2" sx={{ mb: 1.5, px: 0.5 }}>
        {title} · {count}
      </Typography>
      <Stack spacing={1}>{children}</Stack>
    </Paper>
  );
}
