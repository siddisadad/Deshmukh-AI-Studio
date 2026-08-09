import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Navigate } from 'react-router-dom';
import { ApiError } from '../../../shared/api/types';
import { contactInboxApi } from '../api/contactInboxApi';

export function ContactInboxSettingsPage() {
  const queryClient = useQueryClient();

  const accessQuery = useQuery({
    queryKey: ['contact-inbox-access'],
    queryFn: () => contactInboxApi.access(),
  });

  const listQuery = useQuery({
    queryKey: ['contact-inbox'],
    queryFn: () => contactInboxApi.list(),
    enabled: accessQuery.data?.canAccessInbox === true,
  });

  const markRead = useMutation({
    mutationFn: (id: string) => contactInboxApi.markRead(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['contact-inbox'] });
    },
  });

  if (accessQuery.isLoading) {
    return (
      <Box sx={{ display: 'grid', placeItems: 'center', py: 8 }}>
        <CircularProgress aria-label="Loading contact inbox access" />
      </Box>
    );
  }

  if (accessQuery.isError) {
    return (
      <Alert severity="error">
        {accessQuery.error instanceof ApiError
          ? accessQuery.error.message
          : 'Unable to check contact inbox access'}
      </Alert>
    );
  }

  if (!accessQuery.data?.canAccessInbox) {
    return <Navigate to="/dashboard" replace />;
  }

  const items = listQuery.data ?? [];
  const unread = items.filter((item) => !item.readAt).length;

  return (
    <Stack spacing={3}>
      <Box>
        <Typography variant="h4">Contact inbox</Typography>
        <Typography variant="body2" color="text.secondary">
          Official site inquiries for Deshmukh Technology
          {unread > 0 ? ` · ${unread} unread` : ''}
        </Typography>
      </Box>

      {listQuery.isError && (
        <Alert severity="error">
          {listQuery.error instanceof ApiError ? listQuery.error.message : 'Failed to load inquiries'}
        </Alert>
      )}

      {listQuery.isLoading ? (
        <CircularProgress aria-label="Loading inquiries" />
      ) : items.length === 0 ? (
        <Paper variant="outlined" sx={{ p: 3 }}>
          <Typography color="text.secondary">No contact inquiries yet.</Typography>
        </Paper>
      ) : (
        <Paper variant="outlined" sx={{ overflowX: 'auto' }}>
          <Table size="small" aria-label="Contact inquiries">
            <TableHead>
              <TableRow>
                <TableCell>Status</TableCell>
                <TableCell>From</TableCell>
                <TableCell>Topic</TableCell>
                <TableCell>Message</TableCell>
                <TableCell>Received</TableCell>
                <TableCell align="right">Action</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {items.map((item) => (
                <TableRow key={item.id} selected={!item.readAt}>
                  <TableCell>
                    <Chip
                      size="small"
                      label={item.readAt ? 'Read' : 'Unread'}
                      color={item.readAt ? 'default' : 'primary'}
                      variant={item.readAt ? 'outlined' : 'filled'}
                    />
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2" sx={{ fontWeight: 600 }}>
                      {item.name}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {item.email}
                    </Typography>
                  </TableCell>
                  <TableCell>{item.topic}</TableCell>
                  <TableCell sx={{ maxWidth: 320, whiteSpace: 'pre-wrap' }}>{item.message}</TableCell>
                  <TableCell>{new Date(item.createdAt).toLocaleString()}</TableCell>
                  <TableCell align="right">
                    {!item.readAt && (
                      <Button
                        size="small"
                        disabled={markRead.isPending}
                        onClick={() => markRead.mutate(item.id)}
                      >
                        Mark read
                      </Button>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Paper>
      )}
    </Stack>
  );
}
