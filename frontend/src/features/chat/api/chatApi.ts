import axios from 'axios';
import { http, refreshAccessToken } from '../../../shared/api/httpClient';
import { useAuthStore } from '../../auth/store/authStore';
import { ApiError } from '../../../shared/api/types';

export interface Assistant {
  role: string;
  pluginId?: string;
  name: string;
  capabilities: string[];
  limitations: string[];
  tools?: string[];
}

export interface ChatMessage {
  id: string;
  sender: 'USER' | 'ASSISTANT' | 'SYSTEM';
  content: string;
  createdAt: string;
}

export interface ConversationSummary {
  id: string;
  projectId: string;
  assistantRole: string;
  title: string | null;
  createdAt: string;
  updatedAt: string;
  messageCount: number;
  shareEnabled: boolean;
  shareExpiresAt: string | null;
  visibility: 'PROJECT' | 'PRIVATE';
}

export interface ConversationShareResult {
  shareEnabled: boolean;
  shareUrl: string;
  token: string;
  expiresAt: string;
}

export interface SharedConversation {
  assistantRole: string;
  title: string | null;
  expiresAt: string;
  messages: ChatMessage[];
}

export interface Conversation {
  id: string;
  projectId: string;
  assistantRole: string;
  title: string | null;
  messages: ChatMessage[];
}

export interface ChatResponse {
  userMessage: ChatMessage;
  assistantMessage: ChatMessage;
  provider: string;
  model: string;
}

export interface StreamUsage {
  inputTokens?: number;
  outputTokens?: number;
  streamChars: number;
  deltaCount: number;
}

export interface StreamDoneResult {
  assistantMessage: ChatMessage;
  provider: string;
  model: string;
  usage?: StreamUsage;
}

export interface StreamHandlers {
  onUser?: (message: ChatMessage) => void;
  onDelta?: (text: string) => void;
  onDone?: (result: StreamDoneResult) => void;
  onError?: (message: string) => void;
}

export interface StreamOptions {
  signal?: AbortSignal;
  maxRetries?: number;
  onReconnecting?: (attempt: number, reason: 'network' | 'auth' | 'recovering') => void;
}

const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';
const DEFAULT_MAX_RETRIES = 2;
const RETRY_BASE_DELAY_MS = 800;
const RECOVERY_POLL_INTERVAL_MS = 1000;
const RECOVERY_POLL_MAX_MS = 30_000;

function isRetriableHttpStatus(status: number): boolean {
  return status === 401 || status === 502 || status === 503 || status === 429;
}

function isAbortError(err: unknown): boolean {
  return err instanceof DOMException && err.name === 'AbortError';
}

function sleep(ms: number, signal?: AbortSignal): Promise<void> {
  return new Promise((resolve, reject) => {
    if (signal?.aborted) {
      reject(new DOMException('Aborted', 'AbortError'));
      return;
    }
    const id = window.setTimeout(() => resolve(), ms);
    signal?.addEventListener(
      'abort',
      () => {
        window.clearTimeout(id);
        reject(new DOMException('Aborted', 'AbortError'));
      },
      { once: true },
    );
  });
}

async function readStreamError(response: Response): Promise<string> {
  let message = `Stream failed (${response.status})`;
  try {
    const body = (await response.json()) as { message?: string };
    if (body.message) message = body.message;
  } catch {
    // ignore
  }
  return message;
}

function parseStreamUsage(parsed: Record<string, unknown>): StreamUsage | undefined {
  const raw = parsed.usage as Record<string, unknown> | undefined;
  if (!raw || typeof raw !== 'object') {
    return undefined;
  }
  const streamChars = Number(raw.streamChars);
  const deltaCount = Number(raw.deltaCount);
  if (!Number.isFinite(streamChars) || !Number.isFinite(deltaCount)) {
    return undefined;
  }
  const usage: StreamUsage = { streamChars, deltaCount };
  if (raw.inputTokens != null && Number.isFinite(Number(raw.inputTokens))) {
    usage.inputTokens = Number(raw.inputTokens);
  }
  if (raw.outputTokens != null && Number.isFinite(Number(raw.outputTokens))) {
    usage.outputTokens = Number(raw.outputTokens);
  }
  return usage;
}

/** Batches rapid SSE deltas to one RAF flush for smoother rendering. */
export function createDeltaBatcher(onFlush: (text: string) => void): (delta: string) => void {
  let pending = '';
  let scheduled = false;
  return (delta: string) => {
    pending += delta;
    if (!scheduled) {
      scheduled = true;
      requestAnimationFrame(() => {
        scheduled = false;
        if (pending) {
          onFlush(pending);
          pending = '';
        }
      });
    }
  };
}

function triggerBlobDownload(blob: Blob, disposition: string | undefined, defaultFilename: string) {
  let filename = defaultFilename;
  if (disposition) {
    const match = /filename="([^"]+)"/.exec(disposition);
    if (match) filename = match[1];
  }
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  anchor.click();
  URL.revokeObjectURL(url);
}

export async function parseSseStream(
  response: Response,
  handlers: StreamHandlers,
  signal?: AbortSignal,
): Promise<void> {
  if (!response.body) {
    throw new ApiError({ status: 0, code: 'NETWORK_ERROR', message: 'No response body' });
  }
  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';
  let eventName = 'message';

  const abortReader = () => {
    reader.cancel().catch(() => undefined);
  };
  if (signal) {
    if (signal.aborted) {
      abortReader();
      throw new DOMException('Aborted', 'AbortError');
    }
    signal.addEventListener('abort', abortReader, { once: true });
  }

  try {
    while (true) {
      if (signal?.aborted) {
        throw new DOMException('Aborted', 'AbortError');
      }
      const { done, value } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });
      const parts = buffer.split('\n');
      buffer = parts.pop() || '';

      for (const rawLine of parts) {
        const line = rawLine.replace(/\r$/, '');
        if (!line) {
          eventName = 'message';
          continue;
        }
        if (line.startsWith('event:')) {
          eventName = line.slice(6).trim();
          continue;
        }
        if (!line.startsWith('data:')) continue;
        const data = line.slice(5).trim();
        if (!data) continue;
        try {
          const parsed = JSON.parse(data) as Record<string, unknown>;
          if (eventName === 'user') {
            handlers.onUser?.(parsed as unknown as ChatMessage);
          } else if (eventName === 'delta') {
            handlers.onDelta?.(String(parsed.text || ''));
          } else if (eventName === 'done') {
            const usage = parseStreamUsage(parsed);
            handlers.onDone?.({
              assistantMessage: parsed.assistantMessage as ChatMessage,
              provider: String(parsed.provider || ''),
              model: String(parsed.model || ''),
              ...(usage ? { usage } : {}),
            });
          } else if (eventName === 'error') {
            handlers.onError?.(String(parsed.message || 'Stream failed'));
          }
        } catch {
          // ignore malformed chunks
        }
      }
    }
  } finally {
    if (signal) {
      signal.removeEventListener('abort', abortReader);
    }
  }
}

async function pollConversationRecovery(
  conversationId: string,
  userMessageId: string,
  handlers: StreamHandlers,
  options?: StreamOptions,
): Promise<boolean> {
  const deadline = Date.now() + RECOVERY_POLL_MAX_MS;
  while (Date.now() < deadline) {
    if (options?.signal?.aborted) {
      throw new DOMException('Aborted', 'AbortError');
    }
    await sleep(RECOVERY_POLL_INTERVAL_MS, options?.signal);
    const conversation = await http
      .get<Conversation>(`/conversations/${conversationId}`)
      .then((r) => r.data);
    const userIdx = conversation.messages.findIndex((m) => m.id === userMessageId);
    if (userIdx >= 0 && userIdx + 1 < conversation.messages.length) {
      const assistant = conversation.messages[userIdx + 1];
      if (assistant.sender === 'ASSISTANT') {
        handlers.onDone?.({
          assistantMessage: assistant,
          provider: 'stream-recovery',
          model: 'stream-recovery',
        });
        return true;
      }
    }
  }
  return false;
}

async function openStreamRequest(
  conversationId: string,
  content: string,
  signal?: AbortSignal,
): Promise<Response> {
  const token = useAuthStore.getState().accessToken;
  return fetch(`${baseURL}/conversations/${conversationId}/messages/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify({ content }),
    signal,
  });
}

export const chatApi = {
  listAssistants: () =>
    http.get<{ assistants: Assistant[] }>('/assistants').then((r) => r.data.assistants),
  listConversations: (
    projectId: string,
    options?: { assistantRole?: string; q?: string },
  ) =>
    http
      .get<ConversationSummary[]>(`/projects/${projectId}/conversations`, {
        params: {
          ...(options?.assistantRole ? { assistantRole: options.assistantRole } : {}),
          ...(options?.q ? { q: options.q } : {}),
        },
      })
      .then((r) => r.data),
  createConversation: (
    projectId: string,
    body: { assistantRole: string; title?: string; visibility?: 'PROJECT' | 'PRIVATE' },
  ) =>
    http.post<ConversationSummary>(`/projects/${projectId}/conversations`, body).then((r) => r.data),
  getConversation: (conversationId: string) =>
    http.get<Conversation>(`/conversations/${conversationId}`).then((r) => r.data),
  updateConversation: (conversationId: string, title: string) =>
    http.patch<ConversationSummary>(`/conversations/${conversationId}`, { title }).then((r) => r.data),
  deleteConversation: (conversationId: string) =>
    http.delete(`/conversations/${conversationId}`).then(() => undefined),
  enableShare: (conversationId: string) =>
    http.post<ConversationShareResult>(`/conversations/${conversationId}/share`).then((r) => r.data),
  revokeShare: (conversationId: string) =>
    http.delete(`/conversations/${conversationId}/share`).then(() => undefined),
  downloadExport: async (conversationId: string, format: 'json' | 'markdown' = 'markdown') => {
    const response = await http.get(`/conversations/${conversationId}/export`, {
      params: { format },
      responseType: 'blob',
    });
    triggerBlobDownload(
      response.data as Blob,
      response.headers['content-disposition'] as string | undefined,
      `conversation-${conversationId}.${format === 'json' ? 'json' : 'md'}`,
    );
  },
  downloadProjectExport: async (
    projectId: string,
    format: 'json' | 'markdown' = 'markdown',
    assistantRole?: string,
  ) => {
    const response = await http.get(`/projects/${projectId}/conversations/export`, {
      params: { format, assistantRole: assistantRole || undefined },
      responseType: 'blob',
    });
    triggerBlobDownload(
      response.data as Blob,
      response.headers['content-disposition'] as string | undefined,
      `project-${projectId}-threads.${format === 'json' ? 'json' : 'md'}`,
    );
  },
  getSharedConversation: (token: string) =>
    axios
      .get<SharedConversation>(`${baseURL}/shared/conversations/${encodeURIComponent(token)}`)
      .then((r) => r.data),
  sendMessage: (conversationId: string, content: string) =>
    http.post<ChatResponse>(`/conversations/${conversationId}/messages`, { content }).then((r) => r.data),
  streamMessage: async (
    conversationId: string,
    content: string,
    handlers: StreamHandlers,
    options?: StreamOptions,
  ) => {
    const maxRetries = options?.maxRetries ?? DEFAULT_MAX_RETRIES;
    let connectAttempt = 0;
    let userMessageId: string | null = null;

    while (true) {
      if (options?.signal?.aborted) {
        throw new DOMException('Aborted', 'AbortError');
      }

      try {
        let response = await openStreamRequest(conversationId, content, options?.signal);

        if (!response.ok && isRetriableHttpStatus(response.status) && connectAttempt < maxRetries) {
          if (response.status === 401) {
            const newToken = await refreshAccessToken();
            if (!newToken) {
              throw new ApiError({
                status: 401,
                code: 'UNAUTHORIZED',
                message: await readStreamError(response),
              });
            }
            options?.onReconnecting?.(connectAttempt + 1, 'auth');
          } else {
            options?.onReconnecting?.(connectAttempt + 1, 'network');
          }
          connectAttempt += 1;
          await sleep(RETRY_BASE_DELAY_MS * connectAttempt, options?.signal);
          continue;
        }

        if (!response.ok) {
          throw new ApiError({
            status: response.status,
            code: 'STREAM_ERROR',
            message: await readStreamError(response),
          });
        }

        connectAttempt = 0;
        userMessageId = null;

        await parseSseStream(
          response,
          {
            onUser: (message) => {
              userMessageId = message.id;
              handlers.onUser?.(message);
            },
            onDelta: handlers.onDelta ? createDeltaBatcher(handlers.onDelta) : undefined,
            onDone: handlers.onDone,
            onError: handlers.onError,
          },
          options?.signal,
        );
        return;
      } catch (err) {
        if (isAbortError(err)) {
          throw err;
        }

        const isNetwork =
          err instanceof TypeError ||
          (err instanceof ApiError && (err.status === 0 || isRetriableHttpStatus(err.status)));

        if (!userMessageId && isNetwork && connectAttempt < maxRetries) {
          connectAttempt += 1;
          options?.onReconnecting?.(connectAttempt, 'network');
          await sleep(RETRY_BASE_DELAY_MS * connectAttempt, options?.signal);
          continue;
        }

        if (userMessageId) {
          options?.onReconnecting?.(0, 'recovering');
          const recovered = await pollConversationRecovery(
            conversationId,
            userMessageId,
            handlers,
            options,
          );
          if (recovered) {
            return;
          }
        }

        throw err;
      }
    }
  },
};
