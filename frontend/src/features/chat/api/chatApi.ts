import { http } from '../../../shared/api/httpClient';
import { useAuthStore } from '../../auth/store/authStore';
import { ApiError } from '../../../shared/api/types';

export interface Assistant {
  role: string;
  name: string;
  capabilities: string[];
  limitations: string[];
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

export interface StreamHandlers {
  onUser?: (message: ChatMessage) => void;
  onDelta?: (text: string) => void;
  onDone?: (result: { assistantMessage: ChatMessage; provider: string; model: string }) => void;
  onError?: (message: string) => void;
}

const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';

async function parseSseStream(response: Response, handlers: StreamHandlers): Promise<void> {
  if (!response.body) {
    throw new ApiError({ status: 0, code: 'NETWORK_ERROR', message: 'No response body' });
  }
  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';
  let eventName = 'message';

  while (true) {
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
          handlers.onDone?.(parsed as unknown as { assistantMessage: ChatMessage; provider: string; model: string });
        } else if (eventName === 'error') {
          handlers.onError?.(String(parsed.message || 'Stream failed'));
        }
      } catch {
        // ignore malformed chunks
      }
    }
  }
}

export const chatApi = {
  listAssistants: () =>
    http.get<{ assistants: Assistant[] }>('/assistants').then((r) => r.data.assistants),
  listConversations: (projectId: string, assistantRole?: string) =>
    http
      .get<ConversationSummary[]>(`/projects/${projectId}/conversations`, {
        params: assistantRole ? { assistantRole } : undefined,
      })
      .then((r) => r.data),
  createConversation: (projectId: string, body: { assistantRole: string; title?: string }) =>
    http.post<ConversationSummary>(`/projects/${projectId}/conversations`, body).then((r) => r.data),
  getConversation: (conversationId: string) =>
    http.get<Conversation>(`/conversations/${conversationId}`).then((r) => r.data),
  updateConversation: (conversationId: string, title: string) =>
    http.patch<ConversationSummary>(`/conversations/${conversationId}`, { title }).then((r) => r.data),
  deleteConversation: (conversationId: string) =>
    http.delete(`/conversations/${conversationId}`).then(() => undefined),
  sendMessage: (conversationId: string, content: string) =>
    http.post<ChatResponse>(`/conversations/${conversationId}/messages`, { content }).then((r) => r.data),
  streamMessage: async (conversationId: string, content: string, handlers: StreamHandlers) => {
    const token = useAuthStore.getState().accessToken;
    const response = await fetch(`${baseURL}/conversations/${conversationId}/messages/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify({ content }),
    });
    if (!response.ok) {
      let message = `Stream failed (${response.status})`;
      try {
        const body = (await response.json()) as { message?: string };
        if (body.message) message = body.message;
      } catch {
        // ignore
      }
      throw new ApiError({ status: response.status, code: 'STREAM_ERROR', message });
    }
    await parseSseStream(response, handlers);
  },
};
