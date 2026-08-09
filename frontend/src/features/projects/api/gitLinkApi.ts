import { http } from '../../../shared/api/httpClient';
import type { BackgroundJob } from './jobsApi';

export interface ProjectGitLink {
  id: string | null;
  projectId: string;
  provider: string;
  repository: string;
  branch: string;
  enabled: boolean;
  scheduledSyncEnabled: boolean;
  webhookUrl: string;
  webhookSecret: string | null;
  lastSyncedAt: string | null;
  lastSyncStatus: string;
  lastSyncError: string | null;
  scheduledSyncIntervalMinutes: number | null;
  pathIgnorePatterns: string[];
  pathIncludePatterns: string[];
  updatedAt: string | null;
}

export interface UpsertProjectGitLinkBody {
  provider?: string;
  repository: string;
  branch?: string;
  enabled?: boolean;
  scheduledSyncEnabled?: boolean;
  regenerateWebhookSecret?: boolean;
  scheduledSyncIntervalMinutes?: number;
  clearScheduledSyncInterval?: boolean;
  pathIgnorePatterns?: string[];
  clearPathIgnorePatterns?: boolean;
  pathIncludePatterns?: string[];
  clearPathIncludePatterns?: boolean;
}

export interface GitSyncRun {
  id: string;
  projectId: string;
  gitLinkId: string;
  source: string;
  status: string;
  fileCount: number;
  errorMessage: string | null;
  startedAt: string;
  finishedAt: string;
}

export const gitLinkApi = {
  get: (projectId: string) =>
    http.get<ProjectGitLink>(`/projects/${projectId}/git-link`).then((r) => r.data),
  upsert: (projectId: string, body: UpsertProjectGitLinkBody) =>
    http.put<ProjectGitLink>(`/projects/${projectId}/git-link`, body).then((r) => r.data),
  listSyncRuns: (projectId: string, limit = 10) =>
    http.get<GitSyncRun[]>(`/projects/${projectId}/git-link/sync-runs?limit=${limit}`).then((r) => r.data),
  syncNow: (projectId: string) =>
    http.post<ProjectGitLink>(`/projects/${projectId}/git-link/sync`).then((r) => r.data),
  syncAsync: (projectId: string) =>
    http.post<BackgroundJob>(`/projects/${projectId}/git-link/sync/async`).then((r) => r.data),
  testConnection: (projectId: string) =>
    http.post<{ ok: boolean; message: string; checks: { name: string; status: string; message: string }[] }>(
      `/projects/${projectId}/git-link/test`
    ).then((r) => r.data),
};
