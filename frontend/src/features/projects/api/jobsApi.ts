import { http } from '../../../shared/api/httpClient';

export interface BackgroundJob {
  id: string;
  projectId: string;
  jobType: string;
  status: string;
  payload: string;
  result: string;
  errorMessage?: string | null;
  attempts: number;
  startedAt?: string | null;
  finishedAt?: string | null;
  createdAt: string;
  updatedAt: string;
}

export const jobsApi = {
  list: (projectId: string, limit = 20) =>
    http.get<BackgroundJob[]>(`/projects/${projectId}/jobs`, { params: { limit } }).then((r) => r.data),
  get: (jobId: string) => http.get<BackgroundJob>(`/jobs/${jobId}`).then((r) => r.data),
  reindexAsync: (projectId: string) =>
    http.post<BackgroundJob>(`/projects/${projectId}/knowledge/reindex/async`).then((r) => r.data),
};
