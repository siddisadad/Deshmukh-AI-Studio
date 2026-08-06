import { http } from '../../../shared/api/httpClient';

export interface KnowledgeStatus {
  enabled: boolean;
  embeddingProvider: string;
  indexedChunks: number;
}

export interface KnowledgeHit {
  id: string;
  sourceType: string;
  sourceId: string;
  title: string;
  content: string;
  score: number;
}

export interface KnowledgeSearchResult {
  query: string;
  embeddingProvider: string;
  indexedChunks: number;
  hits: KnowledgeHit[];
}

export interface KnowledgeReindexResult {
  chunkCount: number;
  embeddingProvider: string;
  enabled: boolean;
}

export const knowledgeApi = {
  status: (projectId: string) =>
    http.get<KnowledgeStatus>(`/projects/${projectId}/knowledge`).then((r) => r.data),
  reindex: (projectId: string) =>
    http.post<KnowledgeReindexResult>(`/projects/${projectId}/knowledge/reindex`).then((r) => r.data),
  search: (projectId: string, q: string, limit = 8) =>
    http
      .get<KnowledgeSearchResult>(`/projects/${projectId}/knowledge/search`, { params: { q, limit } })
      .then((r) => r.data),
};
