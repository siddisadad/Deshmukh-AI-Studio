import { http } from '../../../shared/api/httpClient';

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

export interface Conversation {
  id: string | null;
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

export const chatApi = {
  listAssistants: () =>
    http.get<{ assistants: Assistant[] }>('/assistants').then((r) => r.data.assistants),
  getConversation: (projectId: string, role: string) =>
    http.get<Conversation>(`/projects/${projectId}/conversations/${role}`).then((r) => r.data),
  sendMessage: (projectId: string, role: string, content: string) =>
    http
      .post<ChatResponse>(`/projects/${projectId}/conversations/${role}/messages`, { content })
      .then((r) => r.data),
};
