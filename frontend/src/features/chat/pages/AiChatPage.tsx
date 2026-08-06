import {
  Alert,
  Box,
  Button,
  CircularProgress,
  IconButton,
  List,
  ListItemButton,
  ListItemText,
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
import {
  chatApi,
  type Assistant,
  type ChatMessage,
  type ConversationSummary,
} from '../api/chatApi';

export function AiChatPage() {
  const { projectId } = useParams();
  const [project, setProject] = useState<Project | null>(null);
  const [assistants, setAssistants] = useState<Assistant[]>([]);
  const [role, setRole] = useState('BUSINESS_ANALYST');
  const [threads, setThreads] = useState<ConversationSummary[]>([]);
  const [activeThreadId, setActiveThreadId] = useState<string | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [streamingContent, setStreamingContent] = useState('');
  const [input, setInput] = useState('');
  const [provider, setProvider] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const bottomRef = useRef<HTMLDivElement | null>(null);

  const selected = assistants.find((a) => a.role === role);
  const activeThread = threads.find((t) => t.id === activeThreadId) || null;

  async function loadThreads(nextRole: string, preferId?: string | null) {
    if (!projectId) return;
    const listed = await chatApi.listConversations(projectId, nextRole);
    setThreads(listed);
    const nextId = (preferId && listed.some((t) => t.id === preferId) ? preferId : listed[0]?.id) || null;
    setActiveThreadId(nextId);
    if (nextId) {
      const conversation = await chatApi.getConversation(nextId);
      setMessages(conversation.messages);
    } else {
      setMessages([]);
    }
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
        await loadThreads(initialRole);
      })
      .catch((err) => setError(err instanceof ApiError ? err.message : 'Failed to load chat'))
      .finally(() => setLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
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
      await loadThreads(value);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to load conversations');
    }
  }

  async function onSelectThread(threadId: string) {
    if (sending || threadId === activeThreadId) return;
    setError(null);
    setStreamingContent('');
    setActiveThreadId(threadId);
    try {
      const conversation = await chatApi.getConversation(threadId);
      setMessages(conversation.messages);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to load thread');
    }
  }

  async function onNewThread() {
    if (!projectId || sending) return;
    setError(null);
    try {
      const created = await chatApi.createConversation(projectId, { assistantRole: role });
      setThreads((prev) => [created, ...prev]);
      setActiveThreadId(created.id);
      setMessages([]);
      setStreamingContent('');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to create thread');
    }
  }

  async function onDeleteThread(threadId: string) {
    if (sending) return;
    setError(null);
    try {
      await chatApi.deleteConversation(threadId);
      const remaining = threads.filter((t) => t.id !== threadId);
      setThreads(remaining);
      if (activeThreadId === threadId) {
        const next = remaining[0]?.id || null;
        setActiveThreadId(next);
        if (next) {
          const conversation = await chatApi.getConversation(next);
          setMessages(conversation.messages);
        } else {
          setMessages([]);
        }
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to delete thread');
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
      let conversationId = activeThreadId;
      if (!conversationId) {
        const created = await chatApi.createConversation(projectId, { assistantRole: role });
        conversationId = created.id;
        setThreads((prev) => [created, ...prev]);
        setActiveThreadId(created.id);
      }
      await chatApi.streamMessage(conversationId, content, {
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
          setThreads((prev) => {
            const updated = prev.map((t) =>
              t.id === conversationId
                ? {
                    ...t,
                    title: t.title?.startsWith('New ') || t.title?.endsWith(' thread')
                      ? content.length > 60
                        ? `${content.slice(0, 57)}…`
                        : content
                      : t.title,
                    messageCount: t.messageCount + 2,
                    updatedAt: new Date().toISOString(),
                  }
                : t,
            );
            return [...updated].sort((a, b) => b.updatedAt.localeCompare(a.updatedAt));
          });
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

      <Box
        sx={{
          display: 'grid',
          gap: 2,
          gridTemplateColumns: { xs: '1fr', md: '260px 1fr' },
          flex: 1,
          minHeight: 0,
        }}
      >
        <Paper variant="outlined" sx={{ p: 1.5, display: 'flex', flexDirection: 'column', minHeight: 280 }}>
          <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center', mb: 1, px: 0.5 }}>
            <Typography variant="subtitle2">Threads</Typography>
            <Button size="small" onClick={() => void onNewThread()} disabled={sending}>
              New
            </Button>
          </Stack>
          <List dense sx={{ overflowY: 'auto', flex: 1 }}>
            {threads.map((thread) => (
              <ListItemButton
                key={thread.id}
                selected={thread.id === activeThreadId}
                onClick={() => void onSelectThread(thread.id)}
                disabled={sending}
                sx={{ borderRadius: 1, alignItems: 'flex-start' }}
              >
                <ListItemText
                  primary={
                    <Typography noWrap sx={{ fontSize: 14 }}>
                      {thread.title || 'Untitled thread'}
                    </Typography>
                  }
                  secondary={
                    <Typography component="span" variant="caption" color="text.secondary">
                      {thread.messageCount} messages · {new Date(thread.updatedAt).toLocaleString()}
                    </Typography>
                  }
                />
                <IconButton
                  size="small"
                  aria-label="Delete thread"
                  onClick={(ev) => {
                    ev.stopPropagation();
                    void onDeleteThread(thread.id);
                  }}
                  disabled={sending}
                >
                  ×
                </IconButton>
              </ListItemButton>
            ))}
            {threads.length === 0 && (
              <Typography variant="body2" color="text.secondary" sx={{ px: 1, py: 2 }}>
                No threads yet for this assistant.
              </Typography>
            )}
          </List>
        </Paper>

        <Stack spacing={2} sx={{ minHeight: 0 }}>
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
                  title={activeThread ? `Continue “${activeThread.title || 'thread'}”` : `Start with the ${selected?.name || 'assistant'}`}
                  description="Each thread keeps its own history. Switch assistants or open a new thread anytime — shared project context still applies."
                  actionLabel={activeThread ? undefined : 'New thread'}
                  onAction={activeThread ? undefined : () => void onNewThread()}
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
              Multi-thread · streaming SSE · {provider || 'Provider: mock (default)'}
            </Typography>
          </Box>
        </Stack>
      </Box>
    </Stack>
  );
}
