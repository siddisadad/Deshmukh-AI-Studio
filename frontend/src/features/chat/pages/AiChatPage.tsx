import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Paper,
  Stack,
  TextField,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
} from '@mui/material';
import { useEffect, useRef, useState, type FormEvent, type MouseEvent } from 'react';
import { Link as RouterLink, useParams } from 'react-router-dom';
import { ApiError } from '../../../shared/api/types';
import { EmptyState } from '../../../shared/ui/EmptyState';
import { projectsApi, type Project } from '../../projects/api/projectsApi';
import { chatApi, type Assistant, type ChatMessage } from '../api/chatApi';

export function AiChatPage() {
  const { projectId } = useParams();
  const [project, setProject] = useState<Project | null>(null);
  const [assistants, setAssistants] = useState<Assistant[]>([]);
  const [role, setRole] = useState('BUSINESS_ANALYST');
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [streamingContent, setStreamingContent] = useState('');
  const [input, setInput] = useState('');
  const [provider, setProvider] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const bottomRef = useRef<HTMLDivElement | null>(null);

  const selected = assistants.find((a) => a.role === role);

  async function loadConversation(nextRole = role) {
    if (!projectId) return;
    const conversation = await chatApi.getConversation(projectId, nextRole);
    setMessages(conversation.messages);
  }

  useEffect(() => {
    if (!projectId) return;
    setLoading(true);
    setError(null);
    Promise.all([projectsApi.getProject(projectId), chatApi.listAssistants()])
      .then(async ([p, list]) => {
        setProject(p);
        setAssistants(list);
        const initialRole = list[0]?.role || 'BUSINESS_ANALYST';
        setRole(initialRole);
        const conversation = await chatApi.getConversation(projectId, initialRole);
        setMessages(conversation.messages);
      })
      .catch((err) => setError(err instanceof ApiError ? err.message : 'Failed to load chat'))
      .finally(() => setLoading(false));
  }, [projectId]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, sending, streamingContent]);

  async function onRoleChange(_: MouseEvent<HTMLElement>, value: string | null) {
    if (!value || !projectId || sending) return;
    setRole(value);
    setError(null);
    setStreamingContent('');
    try {
      await loadConversation(value);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to load conversation');
    }
  }

  async function onSend(e: FormEvent) {
    e.preventDefault();
    if (!projectId || !input.trim() || sending) return;
    setSending(true);
    setError(null);
    setStreamingContent('');
    const content = input.trim();
    setInput('');
    try {
      await chatApi.streamMessage(projectId, role, content, {
        onUser: (userMessage) => {
          setMessages((prev) => [...prev, userMessage]);
        },
        onDelta: (text) => {
          setStreamingContent((prev) => prev + text);
        },
        onDone: ({ assistantMessage, provider: p, model }) => {
          setStreamingContent('');
          setMessages((prev) => [...prev, assistantMessage]);
          setProvider(`${p} / ${model}`);
        },
        onError: (message) => {
          setError(message);
        },
      });
    } catch (err) {
      setInput(content);
      setStreamingContent('');
      setError(err instanceof ApiError ? err.message : 'Failed to send message');
    } finally {
      setSending(false);
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
    <Stack spacing={2} sx={{ height: { md: 'calc(100vh - 160px)' } }}>
      <Stack direction={{ xs: 'column', sm: 'row' }} sx={{ justifyContent: 'space-between', gap: 2 }}>
        <Box>
          <Typography variant="overline" color="primary">
            {project?.projectKey} · AI Chat
          </Typography>
          <Typography variant="h4">{project?.name}</Typography>
        </Box>
        <Button component={RouterLink} to={`/projects/${projectId}`} variant="outlined">
          Overview
        </Button>
      </Stack>

      <ToggleButtonGroup exclusive value={role} onChange={onRoleChange} size="small" sx={{ flexWrap: 'wrap' }}>
        {assistants.map((assistant) => (
          <ToggleButton key={assistant.role} value={assistant.role} disabled={sending}>
            {assistant.name}
          </ToggleButton>
        ))}
      </ToggleButtonGroup>

      {selected && (
        <Typography variant="body2" color="text.secondary">
          Limitations: {selected.limitations.join(' · ')}
        </Typography>
      )}

      {error && <Alert severity="error">{error}</Alert>}

      <Paper
        variant="outlined"
        sx={{
          flex: 1,
          minHeight: 320,
          p: 2,
          overflowY: 'auto',
          bgcolor: 'background.default',
        }}
      >
        <Stack spacing={2}>
          {messages.length === 0 && !streamingContent && (
            <EmptyState
              title={`Start with the ${selected?.name || 'assistant'}`}
              description="Ask about requirements, design trade-offs, test ideas, or docs. Answers stream live using this project's shared context."
              secondary={
                <Typography variant="body2" color="text.secondary">
                  Try: “Summarize open requirements” or “Suggest acceptance criteria for the top item.”
                </Typography>
              }
            />
          )}
          {messages.map((message) => (
            <Box
              key={message.id}
              sx={{
                alignSelf: message.sender === 'USER' ? 'flex-end' : 'flex-start',
                maxWidth: '85%',
              }}
            >
              <Typography variant="caption" color="text.secondary">
                {message.sender === 'USER' ? 'You' : selected?.name || 'Assistant'} ·{' '}
                {new Date(message.createdAt).toLocaleTimeString()}
              </Typography>
              <Paper
                variant="outlined"
                sx={{
                  p: 1.5,
                  mt: 0.5,
                  whiteSpace: 'pre-wrap',
                  bgcolor: message.sender === 'USER' ? 'primary.main' : 'background.paper',
                  color: message.sender === 'USER' ? 'primary.contrastText' : 'text.primary',
                }}
              >
                {message.content}
              </Paper>
            </Box>
          ))}
          {streamingContent && (
            <Box sx={{ alignSelf: 'flex-start', maxWidth: '85%' }}>
              <Typography variant="caption" color="text.secondary">
                {selected?.name || 'Assistant'} · streaming
              </Typography>
              <Paper
                variant="outlined"
                sx={{
                  p: 1.5,
                  mt: 0.5,
                  whiteSpace: 'pre-wrap',
                  bgcolor: 'background.paper',
                }}
              >
                {streamingContent}
                <Box
                  component="span"
                  sx={{
                    display: 'inline-block',
                    width: 8,
                    height: 14,
                    ml: 0.5,
                    bgcolor: 'primary.main',
                    verticalAlign: 'text-bottom',
                    animation: 'pulse 1s ease-in-out infinite',
                    '@keyframes pulse': {
                      '0%, 100%': { opacity: 1 },
                      '50%': { opacity: 0.2 },
                    },
                  }}
                />
              </Paper>
            </Box>
          )}
          {sending && !streamingContent && <Typography color="text.secondary">Assistant is thinking…</Typography>}
          <div ref={bottomRef} />
        </Stack>
      </Paper>

      <Box component="form" onSubmit={onSend}>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
          <TextField
            fullWidth
            placeholder={`Ask the ${selected?.name || 'assistant'}…`}
            value={input}
            onChange={(e) => setInput(e.target.value)}
            disabled={sending}
          />
          <Button type="submit" variant="contained" disabled={sending || !input.trim()}>
            Send
          </Button>
        </Stack>
        <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
          Context: shared project · streaming SSE · {provider || 'Provider: mock (default)'}
        </Typography>
      </Box>
    </Stack>
  );
}
