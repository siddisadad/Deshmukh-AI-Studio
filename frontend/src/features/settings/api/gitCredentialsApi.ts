import { http } from '../../../shared/api/httpClient';

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
  scheduledSyncLinks: number;
  manualSyncLinks: number;
  customSyncIntervalLinks: number;
  failedLastSync: number;
  items: OrgGitSyncOverviewItem[];
}

export interface OrgGitSyncOverviewFilters {
  linked?: boolean;
  enabled?: boolean;
  scheduledSyncEnabled?: boolean;
  customSyncInterval?: boolean;
  provider?: string;
  lastSyncStatus?: string;
}

export interface OrgGitSyncOverviewPresetCount {
  id: string;
  count: number;
}

export interface OrgGitSyncOverviewFilterCounts {
  presets: OrgGitSyncOverviewPresetCount[];
}

export interface OrgGitSyncBulkActionsSummary {
  organizationId: string;
  filteredItems: number;
  retryFailedTargeted: number;
  retryFailedPendingSkipped: number;
  enableScheduledTargeted: number;
  disableScheduledTargeted: number;
  clearIntervalTargeted: number;
  setIntervalTargeted: number;
}

function buildOverviewFilterParams(filters?: OrgGitSyncOverviewFilters) {
  const params = new URLSearchParams();
  if (filters?.linked !== undefined) params.set('linked', String(filters.linked));
  if (filters?.enabled !== undefined) params.set('enabled', String(filters.enabled));
  if (filters?.scheduledSyncEnabled !== undefined) {
    params.set('scheduledSyncEnabled', String(filters.scheduledSyncEnabled));
  }
  if (filters?.customSyncInterval !== undefined) {
    params.set('customSyncInterval', String(filters.customSyncInterval));
  }
  if (filters?.provider) params.set('provider', filters.provider);
  if (filters?.lastSyncStatus) params.set('lastSyncStatus', filters.lastSyncStatus);
  return params;
}

export interface OrgGitSyncDisableScheduledResult {
  targeted: number;
  updated: number;
  updatedProjectIds: string[];
}

export interface OrgGitSyncEnableScheduledResult {
  targeted: number;
  updated: number;
  updatedProjectIds: string[];
}

export interface OrgGitSyncRetryFailedResult {
  targeted: number;
  enqueued: number;
  skippedPending: number;
  enqueuedProjectIds: string[];
}

export interface OrgGitSyncScheduledProjectResult {
  projectId: string;
  scheduledSyncEnabled: boolean;
  updated: boolean;
}

export interface OrgGitSyncClearIntervalProjectResult {
  projectId: string;
  scheduledSyncIntervalMinutes: number | null;
  updated: boolean;
}

export interface OrgGitSyncClearIntervalResult {
  targeted: number;
  updated: number;
  updatedProjectIds: string[];
}

export interface OrgGitSyncSetIntervalProjectResult {
  projectId: string;
  scheduledSyncIntervalMinutes: number;
  updated: boolean;
}

export interface OrgGitSyncSetIntervalResult {
  targeted: number;
  updated: number;
  updatedProjectIds: string[];
}

export interface OrgGitSyncRetryProjectResult {
  projectId: string;
  enqueued: boolean;
  skippedPending: boolean;
}

export interface OrgGitSyncRunItem {
  id: string;
  projectId: string;
  projectName: string;
  projectKey: string;
  gitLinkId: string;
  source: string;
  status: string;
  fileCount: number;
  errorMessage: string | null;
  startedAt: string;
  finishedAt: string;
}

export interface OrgGitSyncRunPage {
  items: OrgGitSyncRunItem[];
  offset: number;
  limit: number;
  totalCount: number;
  hasMore: boolean;
}

export interface OrgGitSyncRunPresetCount {
  id: string;
  count: number;
}

export interface OrgGitSyncRunFilterCounts {
  presets: OrgGitSyncRunPresetCount[];
}

export interface OrgGitSyncFilterPreset {
  id: string;
  scope: 'overview' | 'runs';
  label: string;
  filters: Record<string, string>;
  count: number;
  visibility: 'private' | 'org';
  createdByUserId: string;
  createdByDisplayName: string;
  createdAt: string;
}

export interface CreateOrgGitSyncFilterPresetBody {
  scope: 'overview' | 'runs';
  label: string;
  filters: Record<string, string>;
  visibility?: 'private' | 'org';
}

export interface UpdateOrgGitSyncFilterPresetBody {
  label?: string;
  filters?: Record<string, string>;
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
  getSyncOverview: (orgId: string, filters?: OrgGitSyncOverviewFilters) => {
    const params = buildOverviewFilterParams(filters);
    const query = params.toString();
    return http
      .get<OrgGitSyncOverview>(
        `/organizations/${orgId}/git-sync-overview${query ? `?${query}` : ''}`
      )
      .then((r) => r.data);
  },
  getSyncOverviewFilterCounts: (orgId: string) =>
    http
      .get<OrgGitSyncOverviewFilterCounts>(`/organizations/${orgId}/git-sync-overview/filter-counts`)
      .then((r) => r.data),
  getBulkActionsSummary: (orgId: string, filters?: OrgGitSyncOverviewFilters) => {
    const params = buildOverviewFilterParams(filters);
    const query = params.toString();
    return http
      .get<OrgGitSyncBulkActionsSummary>(
        `/organizations/${orgId}/git-sync-overview/bulk-actions-summary${query ? `?${query}` : ''}`
      )
      .then((r) => r.data);
  },
  downloadBulkActionsSummaryExport: async (
    orgId: string,
    format: 'csv' | 'json',
    filters?: OrgGitSyncOverviewFilters
  ) => {
    const params = buildOverviewFilterParams(filters);
    params.set('format', format);
    const response = await http.get(
      `/organizations/${orgId}/git-sync-overview/bulk-actions-summary/export`,
      {
        params,
        responseType: 'blob',
      }
    );
    triggerBlobDownload(
      response.data as Blob,
      response.headers['content-disposition'] as string | undefined,
      `git-sync-bulk-actions-summary-${orgId}.${format}`,
    );
  },
  retryFailedSyncs: (orgId: string, filters?: OrgGitSyncOverviewFilters) => {
    const params = buildOverviewFilterParams(filters);
    const query = params.toString();
    return http
      .post<OrgGitSyncRetryFailedResult>(
        `/organizations/${orgId}/git-sync-overview/retry-failed${query ? `?${query}` : ''}`
      )
      .then((r) => r.data);
  },
  enableScheduledSyncs: (orgId: string, filters?: OrgGitSyncOverviewFilters) => {
    const params = buildOverviewFilterParams(filters);
    const query = params.toString();
    return http
      .post<OrgGitSyncEnableScheduledResult>(
        `/organizations/${orgId}/git-sync-overview/enable-scheduled-sync${query ? `?${query}` : ''}`
      )
      .then((r) => r.data);
  },
  disableScheduledSyncs: (orgId: string, filters?: OrgGitSyncOverviewFilters) => {
    const params = buildOverviewFilterParams(filters);
    const query = params.toString();
    return http
      .post<OrgGitSyncDisableScheduledResult>(
        `/organizations/${orgId}/git-sync-overview/disable-scheduled-sync${query ? `?${query}` : ''}`
      )
      .then((r) => r.data);
  },
  retryFailedSyncForProject: (orgId: string, projectId: string) =>
    http
      .post<OrgGitSyncRetryProjectResult>(
        `/organizations/${orgId}/git-sync-overview/retry-project/${projectId}`
      )
      .then((r) => r.data),
  enableScheduledSyncForProject: (orgId: string, projectId: string) =>
    http
      .post<OrgGitSyncScheduledProjectResult>(
        `/organizations/${orgId}/git-sync-overview/enable-scheduled-project/${projectId}`
      )
      .then((r) => r.data),
  disableScheduledSyncForProject: (orgId: string, projectId: string) =>
    http
      .post<OrgGitSyncScheduledProjectResult>(
        `/organizations/${orgId}/git-sync-overview/disable-scheduled-project/${projectId}`
      )
      .then((r) => r.data),
  clearCustomSyncIntervalForProject: (orgId: string, projectId: string) =>
    http
      .post<OrgGitSyncClearIntervalProjectResult>(
        `/organizations/${orgId}/git-sync-overview/clear-interval-project/${projectId}`
      )
      .then((r) => r.data),
  clearCustomSyncIntervals: (orgId: string, filters?: OrgGitSyncOverviewFilters) => {
    const params = buildOverviewFilterParams(filters);
    const query = params.toString();
    return http
      .post<OrgGitSyncClearIntervalResult>(
        `/organizations/${orgId}/git-sync-overview/clear-interval${query ? `?${query}` : ''}`
      )
      .then((r) => r.data);
  },
  setCustomSyncIntervalForProject: (
    orgId: string,
    projectId: string,
    scheduledSyncIntervalMinutes: number
  ) =>
    http
      .post<OrgGitSyncSetIntervalProjectResult>(
        `/organizations/${orgId}/git-sync-overview/set-interval-project/${projectId}?scheduledSyncIntervalMinutes=${scheduledSyncIntervalMinutes}`
      )
      .then((r) => r.data),
  setCustomSyncIntervals: (
    orgId: string,
    scheduledSyncIntervalMinutes: number,
    filters?: OrgGitSyncOverviewFilters
  ) => {
    const params = buildOverviewFilterParams(filters);
    params.set('scheduledSyncIntervalMinutes', String(scheduledSyncIntervalMinutes));
    const query = params.toString();
    return http
      .post<OrgGitSyncSetIntervalResult>(
        `/organizations/${orgId}/git-sync-overview/set-interval?${query}`
      )
      .then((r) => r.data);
  },
  downloadSyncOverviewExport: async (
    orgId: string,
    format: 'csv' | 'json',
    filters?: OrgGitSyncOverviewFilters
  ) => {
    const params = buildOverviewFilterParams(filters);
    params.set('format', format);
    const response = await http.get(`/organizations/${orgId}/git-sync-overview/export`, {
      params,
      responseType: 'blob',
    });
    triggerBlobDownload(
      response.data as Blob,
      response.headers['content-disposition'] as string | undefined,
      `git-sync-overview-${orgId}.${format}`,
    );
  },
  listSyncRuns: (
    orgId: string,
    limit = 20,
    filters?: { offset?: number; source?: string; status?: string; projectId?: string }
  ) => {
    const params = new URLSearchParams({ limit: String(limit) });
    if (filters?.offset != null) params.set('offset', String(filters.offset));
    if (filters?.source && filters.source !== 'all') params.set('source', filters.source);
    if (filters?.status && filters.status !== 'all') params.set('status', filters.status);
    if (filters?.projectId) params.set('projectId', filters.projectId);
    return http
      .get<OrgGitSyncRunPage>(`/organizations/${orgId}/git-sync-runs?${params.toString()}`)
      .then((r) => r.data);
  },
  getSyncRunFilterCounts: (orgId: string) =>
    http
      .get<OrgGitSyncRunFilterCounts>(`/organizations/${orgId}/git-sync-runs/filter-counts`)
      .then((r) => r.data),
  downloadSyncRunsExport: async (
    orgId: string,
    format: 'csv' | 'json',
    filters?: { source?: string; status?: string; projectId?: string }
  ) => {
    const params: Record<string, string> = { format };
    if (filters?.source && filters.source !== 'all') params.source = filters.source;
    if (filters?.status && filters.status !== 'all') params.status = filters.status;
    if (filters?.projectId) params.projectId = filters.projectId;
    const response = await http.get(`/organizations/${orgId}/git-sync-runs/export`, {
      params,
      responseType: 'blob',
    });
    triggerBlobDownload(
      response.data as Blob,
      response.headers['content-disposition'] as string | undefined,
      `git-sync-runs-${orgId}.${format}`,
    );
  },
  listFilterPresets: (orgId: string) =>
    http
      .get<OrgGitSyncFilterPreset[]>(`/organizations/${orgId}/git-sync-filter-presets`)
      .then((r) => r.data),
  createFilterPreset: (orgId: string, body: CreateOrgGitSyncFilterPresetBody) =>
    http
      .post<OrgGitSyncFilterPreset>(`/organizations/${orgId}/git-sync-filter-presets`, body)
      .then((r) => r.data),
  renameFilterPreset: (orgId: string, presetId: string, body: UpdateOrgGitSyncFilterPresetBody) =>
    http
      .patch<OrgGitSyncFilterPreset>(`/organizations/${orgId}/git-sync-filter-presets/${presetId}`, body)
      .then((r) => r.data),
  updateFilterPreset: (orgId: string, presetId: string, body: UpdateOrgGitSyncFilterPresetBody) =>
    http
      .patch<OrgGitSyncFilterPreset>(`/organizations/${orgId}/git-sync-filter-presets/${presetId}`, body)
      .then((r) => r.data),
  deleteFilterPreset: (orgId: string, presetId: string) =>
    http.delete(`/organizations/${orgId}/git-sync-filter-presets/${presetId}`).then(() => undefined),
};
