import { http } from '../../../shared/api/httpClient';

export interface Document {
  id: string;
  projectId: string;
  title: string;
  docType: string;
  contentMd: string;
  createdAt: string;
  updatedAt: string;
}

export interface DocumentAiResponse {
  document: Document;
  assistantRole: string;
  provider: string;
  model: string;
  generatedText: string;
}

export const documentsApi = {
  list: (projectId: string) =>
    http.get<Document[]>(`/projects/${projectId}/documents`).then((r) => r.data),
  create: (projectId: string, body: { title: string; docType?: string; contentMd?: string }) =>
    http.post<Document>(`/projects/${projectId}/documents`, body).then((r) => r.data),
  update: (documentId: string, body: Partial<{ title: string; docType: string; contentMd: string }>) =>
    http.patch<Document>(`/documents/${documentId}`, body).then((r) => r.data),
  remove: (documentId: string) => http.delete(`/documents/${documentId}`).then(() => undefined),
  generate: (documentId: string, instructions?: string) =>
    http
      .post<DocumentAiResponse>(`/documents/${documentId}/ai/generate`, instructions ? { instructions } : {})
      .then((r) => r.data),
};
