import { http } from '../../../shared/api/httpClient';
import type { BackgroundJob } from './jobsApi';

export interface ProjectGitLink {
  id: string | null;
  projectId: string;
  provider: string;
  repository: string;
  branch: string;
  enabled: boolean;
  webhookUrl: string;
  webhookSecret: string | null;
  lastSyncedAt: string | null;
  lastSyncStatus: string;
  lastSyncError: string | null;
  updatedAt: string | null;
}

export interface UpsertProjectGitLinkBody {
  provider?: string;
  repository: string;
  branch?: string;
  enabled?: boolean;
  regenerateWebhookSecret?: boolean;
}

export const gitLinkApi = {
  get: (projectId: string) =>
    http.get<ProjectGitLink>(`/projects/${projectId}/git-link`).then((r) => r.data),
  upsert: (projectId: string, body: UpsertProjectGitLinkBody) =>
    http.put<ProjectGitLink>(`/projects/${projectId}/git-link`, body).then((r) => r.data),
  syncNow: (projectId: string) =>
    http.post<ProjectGitLink>(`/projects/${projectId}/git-link/sync`).then((r) => r.data),
  syncAsync: (projectId: string) =>
    http.post<BackgroundJob>(`/projects/${projectId}/git-link/sync/async`).then((r) => r.data),
};
