import { Alert, Box, CircularProgress, Paper, Stack, Typography } from '@mui/material';
import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { formatApiError } from '../../../shared/api/formatApiError';
import { chatApi, type ChatMessage } from '../api/chatApi';

export function SharedChatPage() {
  const { token: tokenParam } = useParams();
  const token = tokenParam?.trim() || '';
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [title, setTitle] = useState<string | null>(null);
  const [assistantRole, setAssistantRole] = useState<string | null>(null);
  const [expiresAt, setExpiresAt] = useState<string | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);

  useEffect(() => {
    if (!token) {
      setError('Missing share link token');
      setLoading(false);
      return;
    }
    let cancelled = false;
    setLoading(true);
    setError(null);
    chatApi
      .getSharedConversation(token)
      .then((data) => {
        if (cancelled) return;
        setTitle(data.title);
        setAssistantRole(data.assistantRole);
        setExpiresAt(data.expiresAt);
        setMessages(data.messages);
      })
      .catch((err) => {
        if (cancelled) return;
        setError(formatApiError(err, 'This shared conversation is unavailable or has expired'));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [token]);

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '50vh' }}>
        <CircularProgress aria-label="Loading shared conversation" />
      </Box>
    );
  }

  if (error) {
    return (
      <Box sx={{ maxWidth: 560, mx: 'auto', mt: 6, px: 2 }}>
        <Alert severity="error">{error}</Alert>
      </Box>
    );
  }

  return (
    <Box sx={{ maxWidth: 800, mx: 'auto', py: 3, px: 2 }}>
      <Stack spacing={2}>
        <Stack spacing={0.5}>
          <Typography variant="h5" component="h1">
            {title || 'Shared conversation'}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Read-only · {assistantRole?.replace(/_/g, ' ')} · link expires{' '}
            {expiresAt ? new Date(expiresAt).toLocaleString() : 'soon'}
          </Typography>
        </Stack>
        <Paper variant="outlined" sx={{ p: 2, bgcolor: 'background.default' }}>
          <Stack spacing={2}>
            {messages.map((message) => (
              <Box
                key={message.id}
                sx={{
                  alignSelf: message.sender === 'USER' ? 'flex-end' : 'flex-start',
                  maxWidth: '85%',
                  px: 1.5,
                  py: 1,
                  borderRadius: 2,
                  bgcolor: message.sender === 'USER' ? 'primary.main' : 'action.hover',
                  color: message.sender === 'USER' ? 'primary.contrastText' : 'text.primary',
                }}
              >
                <Typography variant="caption" component="div" sx={{ opacity: 0.8 }}>
                  {message.sender}
                </Typography>
                <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                  {message.content}
                </Typography>
              </Box>
            ))}
            {messages.length === 0 && (
              <Typography variant="body2" color="text.secondary">No messages in this thread.</Typography>
            )}
          </Stack>
        </Paper>
      </Stack>
    </Box>
  );
}
