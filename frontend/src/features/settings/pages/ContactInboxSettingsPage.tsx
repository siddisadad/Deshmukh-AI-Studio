import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  FormControlLabel,
  Paper,
  Stack,
  Switch,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Navigate } from 'react-router-dom';
import { ApiError } from '../../../shared/api/types';
import { buildInquiryReplyMailto, contactInboxApi } from '../api/contactInboxApi';

export function ContactInboxSettingsPage() {
  const queryClient = useQueryClient();
  const [unreadOnly, setUnreadOnly] = useState(false);

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
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['contact-inbox'] }),
        queryClient.invalidateQueries({ queryKey: ['contact-inbox-access'] }),
      ]);
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
  const visible = unreadOnly ? items.filter((item) => !item.readAt) : items;

  return (
    <Stack spacing={3}>
      <Box
        sx={{
          display: 'flex',
          flexWrap: 'wrap',
          gap: 2,
          alignItems: 'flex-end',
          justifyContent: 'space-between',
        }}
      >
        <Box>
          <Typography variant="h4">Contact inbox</Typography>
          <Typography variant="body2" color="text.secondary">
            Official site inquiries for Deshmukh Technology
            {unread > 0 ? ` · ${unread} unread` : ''}
          </Typography>
        </Box>
        <FormControlLabel
          control={
            <Switch
              checked={unreadOnly}
              onChange={(_, checked) => setUnreadOnly(checked)}
              slotProps={{ input: { 'aria-label': 'Show unread only' } }}
            />
          }
          label="Unread only"
        />
      </Box>

      {listQuery.isError && (
        <Alert severity="error">
          {listQuery.error instanceof ApiError ? listQuery.error.message : 'Failed to load inquiries'}
        </Alert>
      )}

      {listQuery.isLoading ? (
        <CircularProgress aria-label="Loading inquiries" />
      ) : visible.length === 0 ? (
        <Paper variant="outlined" sx={{ p: 3 }}>
          <Typography color="text.secondary">
            {unreadOnly ? 'No unread inquiries.' : 'No contact inquiries yet.'}
          </Typography>
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
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {visible.map((item) => (
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
                    <Stack direction="row" spacing={1} sx={{ justifyContent: 'flex-end' }}>
                      <Button
                        size="small"
                        component="a"
                        href={buildInquiryReplyMailto(item)}
                        data-testid={`contact-reply-${item.id}`}
                      >
                        Reply
                      </Button>
                      {!item.readAt && (
                        <Button
                          size="small"
                          disabled={markRead.isPending}
                          onClick={() => markRead.mutate(item.id)}
                        >
                          Mark read
                        </Button>
                      )}
                    </Stack>
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
