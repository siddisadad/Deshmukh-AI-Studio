import { Box, Button, Stack, Typography } from '@mui/material';
import type { ReactNode } from 'react';

interface EmptyStateProps {
  title: string;
  description: string;
  actionLabel?: string;
  onAction?: () => void;
  secondary?: ReactNode;
}

export function EmptyState({ title, description, actionLabel, onAction, secondary }: EmptyStateProps) {
  return (
    <Box
      sx={{
        border: '1px dashed',
        borderColor: 'divider',
        borderRadius: 2,
        px: 3,
        py: 5,
        textAlign: 'center',
        bgcolor: 'action.hover',
      }}
    >
      <Stack spacing={1.5} sx={{ alignItems: 'center' }}>
        <Typography variant="h6">{title}</Typography>
        <Typography color="text.secondary" sx={{ maxWidth: 480 }}>
          {description}
        </Typography>
        {actionLabel && onAction && (
          <Button variant="contained" onClick={onAction} sx={{ mt: 1 }}>
            {actionLabel}
          </Button>
        )}
        {secondary}
      </Stack>
    </Box>
  );
}
