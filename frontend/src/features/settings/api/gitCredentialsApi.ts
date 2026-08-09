import { http } from '../../../shared/api/httpClient';

export interface OrgGitCredentialEvent {
  id: string;
  provider: string;
  action: string;
  actorUserId: string | null;
  displayName: string | null;
  apiBaseUrl: string | null;
  createdAt: string;
}

export interface OrgGitCredential {
  id: string | null;
  provider: string;
  displayName: string;
  configured: boolean;
  apiBaseUrl: string | null;
  enabled: boolean;
  credentialSource: string;
  lastTestedAt: string | null;
  lastTestStatus: string | null;
  lastTestError: string | null;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface UpsertOrgGitCredentialBody {
  displayName: string;
  apiToken?: string;
  apiBaseUrl?: string;
  enabled?: boolean;
}

export interface GitConnectionCheck {
  name: string;
  status: string;
  message: string;
}

export interface GitConnectionTestResult {
  ok: boolean;
  message: string;
  checks: GitConnectionCheck[];
}

export interface OrgGitSyncOverviewItem {
  projectId: string;
  projectName: string;
  projectKey: string;
  linked: boolean;
  linkId: string | null;
  provider: string | null;
  repository: string | null;
  branch: string | null;
  enabled: boolean;
  scheduledSyncEnabled: boolean;
  lastSyncedAt: string | null;
  lastSyncStatus: string;
  lastSyncError: string | null;
  scheduledSyncIntervalMinutes: number | null;
}

export interface OrgGitSyncOverview {
  organizationId: string;
  totalProjects: number;
  linkedProjects: number;
  enabledLinks: number;
  failedLastSync: number;
  items: OrgGitSyncOverviewItem[];
}

export const gitCredentialsApi = {
  list: (orgId: string) =>
    http.get<OrgGitCredential[]>(`/organizations/${orgId}/git-credentials`).then((r) => r.data),
  listEvents: (orgId: string, limit = 50) =>
    http.get<OrgGitCredentialEvent[]>(`/organizations/${orgId}/git-credentials/events?limit=${limit}`).then((r) => r.data),
  upsert: (orgId: string, provider: string, body: UpsertOrgGitCredentialBody) =>
    http.put<OrgGitCredential>(`/organizations/${orgId}/git-credentials/${provider}`, body).then((r) => r.data),
  delete: (orgId: string, provider: string) =>
    http.delete(`/organizations/${orgId}/git-credentials/${provider}`).then(() => undefined),
  test: (orgId: string, provider: string, repository?: string, branch?: string) => {
    const params = new URLSearchParams();
    if (repository) params.set('repository', repository);
    if (branch) params.set('branch', branch);
    const query = params.toString();
    return http
      .post<GitConnectionTestResult>(
        `/organizations/${orgId}/git-credentials/${provider}/test${query ? `?${query}` : ''}`
      )
      .then((r) => r.data);
  },
  getSyncOverview: (
    orgId: string,
    filters?: {
      linked?: boolean;
      provider?: string;
      lastSyncStatus?: string;
    }
  ) => {
    const params = new URLSearchParams();
    if (filters?.linked !== undefined) params.set('linked', String(filters.linked));
    if (filters?.provider) params.set('provider', filters.provider);
    if (filters?.lastSyncStatus) params.set('lastSyncStatus', filters.lastSyncStatus);
    const query = params.toString();
    return http
      .get<OrgGitSyncOverview>(
        `/organizations/${orgId}/git-sync-overview${query ? `?${query}` : ''}`
      )
      .then((r) => r.data);
  },
};
