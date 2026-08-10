import {
  Alert,
  Box,
  Button,
  Link,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { ApiError } from '../../../shared/api/types';
import { useAuthStore } from '../../auth/store/authStore';
import { organizationsApi } from '../../projects/api/organizationsApi';
import {
  gitCredentialsApi,
  type GitConnectionTestResult,
  type OrgGitCredential,
  type OrgGitSyncRunItem,
} from '../api/gitCredentialsApi';

const PROVIDERS = ['github', 'gitlab', 'bitbucket'] as const;
const GIT_SYNC_SECTION_HASH = '#git-repository-sync';
const ORG_SYNC_RUNS_SECTION_ID = 'org-git-sync-runs';

function projectGitSettingsPath(projectId: string) {
  return `/projects/${projectId}/settings${GIT_SYNC_SECTION_HASH}`;
}

export function GitCredentialsSettingsPage() {
  const org = useAuthStore((s) => s.organization);
  const queryClient = useQueryClient();
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selectedProvider, setSelectedProvider] = useState<string>('github');
  const [displayName, setDisplayName] = useState('');
  const [apiToken, setApiToken] = useState('');
  const [apiBaseUrl, setApiBaseUrl] = useState('');
  const [testResult, setTestResult] = useState<GitConnectionTestResult | null>(null);
  const [overviewLinkedFilter, setOverviewLinkedFilter] = useState<'all' | 'linked' | 'unlinked'>('all');
  const [overviewEnabledFilter, setOverviewEnabledFilter] = useState<'all' | 'enabled' | 'disabled'>('all');
  const [overviewScheduledSyncFilter, setOverviewScheduledSyncFilter] = useState<'all' | 'scheduled' | 'manual'>('all');
  const [overviewIntervalFilter, setOverviewIntervalFilter] = useState<'all' | 'custom' | 'default'>('all');
  const [overviewProviderFilter, setOverviewProviderFilter] = useState<'all' | 'github' | 'gitlab' | 'bitbucket'>('all');
  const [overviewStatusFilter, setOverviewStatusFilter] = useState<'all' | 'success' | 'failed' | 'never'>('all');
  const [overviewExporting, setOverviewExporting] = useState<'csv' | 'json' | null>(null);
  const [runSourceFilter, setRunSourceFilter] = useState<'all' | 'manual' | 'scheduled' | 'webhook'>('all');
  const [runStatusFilter, setRunStatusFilter] = useState<'all' | 'success' | 'failed'>('all');
  const [runProjectFilter, setRunProjectFilter] = useState<'all' | string>('all');
  const [runOffset, setRunOffset] = useState(0);
  const [runItems, setRunItems] = useState<OrgGitSyncRunItem[]>([]);
  const [runHasMore, setRunHasMore] = useState(false);
  const [runTotalCount, setRunTotalCount] = useState(0);
  const [runLoadingMore, setRunLoadingMore] = useState(false);
  const [runExporting, setRunExporting] = useState<'csv' | 'json' | null>(null);
  const [retryingProjectId, setRetryingProjectId] = useState<string | null>(null);
  const [togglingScheduledProjectId, setTogglingScheduledProjectId] = useState<string | null>(null);

  const orgQuery = useQuery({
    queryKey: ['organization', org?.id],
    queryFn: () => organizationsApi.get(org!.id),
    enabled: !!org?.id,
  });

  const credentialsQuery = useQuery({
    queryKey: ['org-git-credentials', org?.id],
    queryFn: () => gitCredentialsApi.list(org!.id),
    enabled: !!org?.id,
  });

  const eventsQuery = useQuery({
    queryKey: ['org-git-credential-events', org?.id],
    queryFn: () => gitCredentialsApi.listEvents(org!.id, 30),
    enabled: !!org?.id,
  });

  const syncOverviewQuery = useQuery({
    queryKey: [
      'org-git-sync-overview',
      org?.id,
      overviewLinkedFilter,
      overviewEnabledFilter,
      overviewScheduledSyncFilter,
      overviewIntervalFilter,
      overviewProviderFilter,
      overviewStatusFilter,
    ],
    queryFn: () =>
      gitCredentialsApi.getSyncOverview(org!.id, {
        linked: overviewLinkedFilter === 'all' ? undefined : overviewLinkedFilter === 'linked',
        enabled: overviewEnabledFilter === 'all' ? undefined : overviewEnabledFilter === 'enabled',
        scheduledSyncEnabled:
          overviewScheduledSyncFilter === 'all' ? undefined : overviewScheduledSyncFilter === 'scheduled',
        customSyncInterval:
          overviewIntervalFilter === 'all' ? undefined : overviewIntervalFilter === 'custom',
        provider: overviewProviderFilter === 'all' ? undefined : overviewProviderFilter,
        lastSyncStatus: overviewStatusFilter === 'all' ? undefined : overviewStatusFilter,
      }),
    enabled: !!org?.id,
  });

  const runProjectOptionsQuery = useQuery({
    queryKey: ['org-git-sync-overview', org?.id, 'run-project-options'],
    queryFn: () => gitCredentialsApi.getSyncOverview(org!.id),
    enabled: !!org?.id,
  });

  const syncRunsQuery = useQuery({
    queryKey: ['org-git-sync-runs', org?.id, runSourceFilter, runStatusFilter, runProjectFilter],
    queryFn: () =>
      gitCredentialsApi.listSyncRuns(org!.id, 20, {
        offset: 0,
        source: runSourceFilter,
        status: runStatusFilter,
        projectId: runProjectFilter === 'all' ? undefined : runProjectFilter,
      }),
    enabled: !!org?.id,
  });

  useEffect(() => {
    if (syncRunsQuery.data) {
      setRunItems(syncRunsQuery.data.items);
      setRunHasMore(syncRunsQuery.data.hasMore);
      setRunTotalCount(syncRunsQuery.data.totalCount);
      setRunOffset(0);
    }
  }, [syncRunsQuery.data]);

  async function loadSyncRuns(
    source = runSourceFilter,
    status = runStatusFilter,
    project = runProjectFilter
  ) {
    if (!org?.id) return;
    setRunOffset(0);
    const page = await queryClient.fetchQuery({
      queryKey: ['org-git-sync-runs', org.id, source, status, project],
      queryFn: () =>
        gitCredentialsApi.listSyncRuns(org.id, 20, {
          offset: 0,
          source,
          status,
          projectId: project === 'all' ? undefined : project,
        }),
    });
    setRunItems(page.items);
    setRunHasMore(page.hasMore);
    setRunTotalCount(page.totalCount);
    setRunOffset(page.offset);
  }

  async function loadMoreSyncRuns() {
    if (!org?.id || !runHasMore || runLoadingMore) return;
    const nextOffset = runOffset + 20;
    setRunLoadingMore(true);
    try {
      const page = await gitCredentialsApi.listSyncRuns(org.id, 20, {
        offset: nextOffset,
        source: runSourceFilter,
        status: runStatusFilter,
        projectId: runProjectFilter === 'all' ? undefined : runProjectFilter,
      });
      setRunItems((prev) => [...prev, ...page.items]);
      setRunHasMore(page.hasMore);
      setRunTotalCount(page.totalCount);
      setRunOffset(nextOffset);
    } finally {
      setRunLoadingMore(false);
    }
  }

  async function loadSyncOverview(
    linked = overviewLinkedFilter,
    enabled = overviewEnabledFilter,
    scheduled = overviewScheduledSyncFilter,
    interval = overviewIntervalFilter,
    provider = overviewProviderFilter,
    status = overviewStatusFilter
  ) {
    if (!org?.id) return;
    await queryClient.fetchQuery({
      queryKey: ['org-git-sync-overview', org.id, linked, enabled, scheduled, interval, provider, status],
      queryFn: () =>
        gitCredentialsApi.getSyncOverview(org.id, {
          linked: linked === 'all' ? undefined : linked === 'linked',
          enabled: enabled === 'all' ? undefined : enabled === 'enabled',
          scheduledSyncEnabled: scheduled === 'all' ? undefined : scheduled === 'scheduled',
          customSyncInterval: interval === 'all' ? undefined : interval === 'custom',
          provider: provider === 'all' ? undefined : provider,
          lastSyncStatus: status === 'all' ? undefined : status,
        }),
    });
  }

  async function exportSyncOverview(format: 'csv' | 'json') {
    if (!org?.id) return;
    setOverviewExporting(format);
    setError(null);
    try {
      await gitCredentialsApi.downloadSyncOverviewExport(org.id, format, {
        linked: overviewLinkedFilter === 'all' ? undefined : overviewLinkedFilter === 'linked',
        enabled: overviewEnabledFilter === 'all' ? undefined : overviewEnabledFilter === 'enabled',
        scheduledSyncEnabled:
          overviewScheduledSyncFilter === 'all' ? undefined : overviewScheduledSyncFilter === 'scheduled',
        customSyncInterval:
          overviewIntervalFilter === 'all' ? undefined : overviewIntervalFilter === 'custom',
        provider: overviewProviderFilter === 'all' ? undefined : overviewProviderFilter,
        lastSyncStatus: overviewStatusFilter === 'all' ? undefined : overviewStatusFilter,
      });
    } catch (err) {
      setMessage(null);
      setError(err instanceof ApiError ? err.message : 'Failed to export overview');
    } finally {
      setOverviewExporting(null);
    }
  }

  async function exportSyncRuns(format: 'csv' | 'json') {
    if (!org?.id) return;
    setRunExporting(format);
    setError(null);
    try {
      await gitCredentialsApi.downloadSyncRunsExport(org.id, format, {
        source: runSourceFilter,
        status: runStatusFilter,
        projectId: runProjectFilter === 'all' ? undefined : runProjectFilter,
      });
    } catch (err) {
      setMessage(null);
      setError(err instanceof ApiError ? err.message : 'Failed to export sync runs');
    } finally {
      setRunExporting(null);
    }
  }

  const isOwner = orgQuery.data?.role === 'OWNER';
  const canRetryFailedSyncs =
    orgQuery.data?.role === 'OWNER' || orgQuery.data?.role === 'ADMIN';

  const retryFailedSyncs = useMutation({
    mutationFn: () => gitCredentialsApi.retryFailedSyncs(org!.id),
    onSuccess: async (result) => {
      setError(null);
      setMessage(
        `Enqueued ${result.enqueued} of ${result.targeted} failed syncs`
          + (result.skippedPending > 0 ? ` (${result.skippedPending} skipped — sync already pending)` : ''),
      );
      await queryClient.invalidateQueries({ queryKey: ['org-git-sync-overview', org?.id] });
    },
    onError: (err) => {
      setMessage(null);
      setError(err instanceof ApiError ? err.message : 'Failed to retry syncs');
    },
  });

  const enableScheduledSyncs = useMutation({
    mutationFn: () => gitCredentialsApi.enableScheduledSyncs(org!.id),
    onSuccess: async (result) => {
      setError(null);
      setMessage(`Enabled scheduled sync on ${result.updated} of ${result.targeted} manual-only links`);
      await queryClient.invalidateQueries({ queryKey: ['org-git-sync-overview', org?.id] });
    },
    onError: (err) => {
      setMessage(null);
      setError(err instanceof ApiError ? err.message : 'Failed to enable scheduled sync');
    },
  });

  const disableScheduledSyncs = useMutation({
    mutationFn: () => gitCredentialsApi.disableScheduledSyncs(org!.id),
    onSuccess: async (result) => {
      setError(null);
      setMessage(`Disabled scheduled sync on ${result.updated} of ${result.targeted} links`);
      await queryClient.invalidateQueries({ queryKey: ['org-git-sync-overview', org?.id] });
    },
    onError: (err) => {
      setMessage(null);
      setError(err instanceof ApiError ? err.message : 'Failed to disable scheduled sync');
    },
  });

  async function retryFailedSyncForProject(projectId: string, projectKey: string) {
    if (!org?.id) return;
    setRetryingProjectId(projectId);
    setError(null);
    try {
      const result = await gitCredentialsApi.retryFailedSyncForProject(org.id, projectId);
      setMessage(
        result.enqueued
          ? `Enqueued sync for ${projectKey}`
          : `Skipped ${projectKey} — sync already pending`,
      );
      await queryClient.invalidateQueries({ queryKey: ['org-git-sync-overview', org.id] });
    } catch (err) {
      setMessage(null);
      setError(err instanceof ApiError ? err.message : 'Failed to retry sync');
    } finally {
      setRetryingProjectId(null);
    }
  }

  async function enableScheduledSyncForProject(projectId: string, projectKey: string) {
    if (!org?.id) return;
    setTogglingScheduledProjectId(projectId);
    setError(null);
    try {
      const result = await gitCredentialsApi.enableScheduledSyncForProject(org.id, projectId);
      setMessage(
        result.updated
          ? `Enabled scheduled sync for ${projectKey}`
          : `${projectKey} already has scheduled sync enabled`,
      );
      await queryClient.invalidateQueries({ queryKey: ['org-git-sync-overview', org.id] });
    } catch (err) {
      setMessage(null);
      setError(err instanceof ApiError ? err.message : 'Failed to enable scheduled sync');
    } finally {
      setTogglingScheduledProjectId(null);
    }
  }

  async function disableScheduledSyncForProject(projectId: string, projectKey: string) {
    if (!org?.id) return;
    setTogglingScheduledProjectId(projectId);
    setError(null);
    try {
      const result = await gitCredentialsApi.disableScheduledSyncForProject(org.id, projectId);
      setMessage(
        result.updated
          ? `Disabled scheduled sync for ${projectKey}`
          : `${projectKey} already set to manual only`,
      );
      await queryClient.invalidateQueries({ queryKey: ['org-git-sync-overview', org.id] });
    } catch (err) {
      setMessage(null);
      setError(err instanceof ApiError ? err.message : 'Failed to disable scheduled sync');
    } finally {
      setTogglingScheduledProjectId(null);
    }
  }

  async function viewFailedRunsForProject(projectId: string) {
    setRunProjectFilter(projectId);
    setRunStatusFilter('failed');
    setRunSourceFilter('all');
    await loadSyncRuns('all', 'failed', projectId);
    document.getElementById(ORG_SYNC_RUNS_SECTION_ID)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  async function filterOverviewToFailed() {
    setOverviewStatusFilter('failed');
    await loadSyncOverview(
      overviewLinkedFilter,
      overviewEnabledFilter,
      overviewScheduledSyncFilter,
      overviewIntervalFilter,
      overviewProviderFilter,
      'failed'
    );
  }

  async function filterOverviewToLinked() {
    setOverviewLinkedFilter('linked');
    setOverviewEnabledFilter('all');
    setOverviewScheduledSyncFilter('all');
    setOverviewIntervalFilter('all');
    await loadSyncOverview('linked', 'all', 'all', 'all', overviewProviderFilter, overviewStatusFilter);
  }

  async function filterOverviewToEnabled() {
    setOverviewLinkedFilter('linked');
    setOverviewEnabledFilter('enabled');
    setOverviewScheduledSyncFilter('all');
    setOverviewIntervalFilter('all');
    await loadSyncOverview('linked', 'enabled', 'all', 'all', overviewProviderFilter, overviewStatusFilter);
  }

  async function filterOverviewToScheduledSync() {
    setOverviewLinkedFilter('linked');
    setOverviewEnabledFilter('enabled');
    setOverviewScheduledSyncFilter('scheduled');
    setOverviewIntervalFilter('all');
    await loadSyncOverview('linked', 'enabled', 'scheduled', 'all', overviewProviderFilter, overviewStatusFilter);
  }

  async function filterOverviewToManualSync() {
    setOverviewLinkedFilter('linked');
    setOverviewEnabledFilter('enabled');
    setOverviewScheduledSyncFilter('manual');
    setOverviewIntervalFilter('all');
    await loadSyncOverview('linked', 'enabled', 'manual', 'all', overviewProviderFilter, overviewStatusFilter);
  }

  async function filterOverviewToCustomInterval() {
    setOverviewLinkedFilter('linked');
    setOverviewEnabledFilter('enabled');
    setOverviewScheduledSyncFilter('all');
    setOverviewIntervalFilter('custom');
    await loadSyncOverview('linked', 'enabled', 'all', 'custom', overviewProviderFilter, overviewStatusFilter);
  }

  const selectedCredential = credentialsQuery.data?.find((c) => c.provider === selectedProvider);

  const upsert = useMutation({
    mutationFn: () =>
      gitCredentialsApi.upsert(org!.id, selectedProvider, {
        displayName: displayName.trim(),
        apiToken: apiToken.trim() || undefined,
        apiBaseUrl: apiBaseUrl.trim() || undefined,
        enabled: true,
      }),
    onSuccess: async () => {
      setError(null);
      setMessage('Git credential saved');
      setApiToken('');
      await queryClient.invalidateQueries({ queryKey: ['org-git-credentials', org?.id] });
      await queryClient.invalidateQueries({ queryKey: ['org-git-credential-events', org?.id] });
    },
    onError: (err) => {
      setMessage(null);
      setError(err instanceof ApiError ? err.message : 'Failed to save credential');
    },
  });

  const remove = useMutation({
    mutationFn: () => gitCredentialsApi.delete(org!.id, selectedProvider),
    onSuccess: async () => {
      setError(null);
      setMessage('Org credential removed — platform env fallback applies when configured');
      setTestResult(null);
      await queryClient.invalidateQueries({ queryKey: ['org-git-credentials', org?.id] });
      await queryClient.invalidateQueries({ queryKey: ['org-git-credential-events', org?.id] });
    },
    onError: (err) => {
      setMessage(null);
      setError(err instanceof ApiError ? err.message : 'Failed to remove credential');
    },
  });

  const test = useMutation({
    mutationFn: () => gitCredentialsApi.test(org!.id, selectedProvider),
    onSuccess: (result) => {
      setError(null);
      setTestResult(result);
      setMessage(result.ok ? 'Connection test passed' : 'Connection test failed');
      void queryClient.invalidateQueries({ queryKey: ['org-git-credentials', org?.id] });
    },
    onError: (err) => {
      setMessage(null);
      setError(err instanceof ApiError ? err.message : 'Connection test failed');
    },
  });

  function onSelectProvider(provider: string) {
    setSelectedProvider(provider);
    setTestResult(null);
    const cred = credentialsQuery.data?.find((c) => c.provider === provider);
    setDisplayName(cred?.id ? cred.displayName : '');
    setApiBaseUrl(cred?.apiBaseUrl ?? '');
    setApiToken('');
  }

  return (
    <Stack spacing={3} sx={{ maxWidth: 720 }} data-testid="git-credentials-settings">
      <Box>
        <Typography variant="h4">Git credentials</Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
          Per-organization PATs for private repository sync. Org credentials override platform env
          tokens when enabled.
        </Typography>
      </Box>

      {error && <Alert severity="error">{error}</Alert>}
      {message && <Alert severity="success">{message}</Alert>}

      {syncOverviewQuery.data && (
        <Stack spacing={1} data-testid="git-sync-overview">
          <Typography variant="subtitle2">Git sync overview</Typography>
          <Typography variant="body2" color="text.secondary">
            <Link
              component="button"
              variant="body2"
              onClick={() => void filterOverviewToLinked()}
              sx={{ verticalAlign: 'baseline' }}
              data-testid="git-sync-overview-linked-count"
            >
              {syncOverviewQuery.data.linkedProjects} of {syncOverviewQuery.data.totalProjects} projects linked
            </Link>
            {' · '}
            <Link
              component="button"
              variant="body2"
              onClick={() => void filterOverviewToEnabled()}
              sx={{ verticalAlign: 'baseline' }}
              data-testid="git-sync-overview-enabled-count"
            >
              {syncOverviewQuery.data.enabledLinks} enabled
            </Link>
            {syncOverviewQuery.data.scheduledSyncLinks > 0 ? (
              <>
                {' · '}
                <Link
                  component="button"
                  variant="body2"
                  onClick={() => void filterOverviewToScheduledSync()}
                  sx={{ verticalAlign: 'baseline' }}
                  data-testid="git-sync-overview-scheduled-count"
                >
                  {syncOverviewQuery.data.scheduledSyncLinks} scheduled sync
                </Link>
              </>
            ) : null}
            {syncOverviewQuery.data.manualSyncLinks > 0 ? (
              <>
                {' · '}
                <Link
                  component="button"
                  variant="body2"
                  onClick={() => void filterOverviewToManualSync()}
                  sx={{ verticalAlign: 'baseline' }}
                  data-testid="git-sync-overview-manual-count"
                >
                  {syncOverviewQuery.data.manualSyncLinks} manual only
                </Link>
              </>
            ) : null}
            {syncOverviewQuery.data.customSyncIntervalLinks > 0 ? (
              <>
                {' · '}
                <Link
                  component="button"
                  variant="body2"
                  onClick={() => void filterOverviewToCustomInterval()}
                  sx={{ verticalAlign: 'baseline' }}
                  data-testid="git-sync-overview-custom-interval-count"
                >
                  {syncOverviewQuery.data.customSyncIntervalLinks} custom interval
                </Link>
              </>
            ) : null}
            {syncOverviewQuery.data.failedLastSync > 0 ? (
              <>
                {' · '}
                <Link
                  component="button"
                  variant="body2"
                  onClick={() => void filterOverviewToFailed()}
                  sx={{ verticalAlign: 'baseline' }}
                  data-testid="git-sync-overview-failed-count"
                >
                  {syncOverviewQuery.data.failedLastSync} failed last sync
                </Link>
              </>
            ) : null}
            · showing {syncOverviewQuery.data.items.length} matching filter
          </Typography>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} sx={{ alignItems: 'flex-start' }}>
            <TextField
              select
              label="Linked"
              size="small"
              value={overviewLinkedFilter}
              onChange={(e) => {
                const value = e.target.value as 'all' | 'linked' | 'unlinked';
                setOverviewLinkedFilter(value);
                void loadSyncOverview(
                  value,
                  overviewEnabledFilter,
                  overviewScheduledSyncFilter,
                  overviewIntervalFilter,
                  overviewProviderFilter,
                  overviewStatusFilter
                );
              }}
              slotProps={{ htmlInput: { 'data-testid': 'git-sync-overview-linked-filter' } }}
            >
              <MenuItem value="all">All projects</MenuItem>
              <MenuItem value="linked">Linked only</MenuItem>
              <MenuItem value="unlinked">Unlinked only</MenuItem>
            </TextField>
            <TextField
              select
              label="Enabled"
              size="small"
              value={overviewEnabledFilter}
              onChange={(e) => {
                const value = e.target.value as 'all' | 'enabled' | 'disabled';
                setOverviewEnabledFilter(value);
                void loadSyncOverview(
                  overviewLinkedFilter,
                  value,
                  overviewScheduledSyncFilter,
                  overviewIntervalFilter,
                  overviewProviderFilter,
                  overviewStatusFilter
                );
              }}
              slotProps={{ htmlInput: { 'data-testid': 'git-sync-overview-enabled-filter' } }}
            >
              <MenuItem value="all">All link states</MenuItem>
              <MenuItem value="enabled">Enabled only</MenuItem>
              <MenuItem value="disabled">Disabled only</MenuItem>
            </TextField>
            <TextField
              select
              label="Scheduled sync"
              size="small"
              value={overviewScheduledSyncFilter}
              onChange={(e) => {
                const value = e.target.value as 'all' | 'scheduled' | 'manual';
                setOverviewScheduledSyncFilter(value);
                void loadSyncOverview(
                  overviewLinkedFilter,
                  overviewEnabledFilter,
                  value,
                  overviewIntervalFilter,
                  overviewProviderFilter,
                  overviewStatusFilter
                );
              }}
              slotProps={{ htmlInput: { 'data-testid': 'git-sync-overview-scheduled-filter' } }}
            >
              <MenuItem value="all">All sync modes</MenuItem>
              <MenuItem value="scheduled">Scheduled only</MenuItem>
              <MenuItem value="manual">Manual only</MenuItem>
            </TextField>
            <TextField
              select
              label="Sync interval"
              size="small"
              value={overviewIntervalFilter}
              onChange={(e) => {
                const value = e.target.value as 'all' | 'custom' | 'default';
                setOverviewIntervalFilter(value);
                void loadSyncOverview(
                  overviewLinkedFilter,
                  overviewEnabledFilter,
                  overviewScheduledSyncFilter,
                  value,
                  overviewProviderFilter,
                  overviewStatusFilter
                );
              }}
              slotProps={{ htmlInput: { 'data-testid': 'git-sync-overview-interval-filter' } }}
            >
              <MenuItem value="all">All intervals</MenuItem>
              <MenuItem value="custom">Custom interval</MenuItem>
              <MenuItem value="default">Platform default</MenuItem>
            </TextField>
            <TextField
              select
              label="Provider"
              size="small"
              value={overviewProviderFilter}
              onChange={(e) => {
                const value = e.target.value as 'all' | 'github' | 'gitlab' | 'bitbucket';
                setOverviewProviderFilter(value);
                void loadSyncOverview(
                  overviewLinkedFilter,
                  overviewEnabledFilter,
                  overviewScheduledSyncFilter,
                  overviewIntervalFilter,
                  value,
                  overviewStatusFilter
                );
              }}
              slotProps={{ htmlInput: { 'data-testid': 'git-sync-overview-provider-filter' } }}
            >
              <MenuItem value="all">All providers</MenuItem>
              <MenuItem value="github">GitHub</MenuItem>
              <MenuItem value="gitlab">GitLab</MenuItem>
              <MenuItem value="bitbucket">Bitbucket</MenuItem>
            </TextField>
            <TextField
              select
              label="Last sync"
              size="small"
              value={overviewStatusFilter}
              onChange={(e) => {
                const value = e.target.value as 'all' | 'success' | 'failed' | 'never';
                setOverviewStatusFilter(value);
                void loadSyncOverview(
                  overviewLinkedFilter,
                  overviewEnabledFilter,
                  overviewScheduledSyncFilter,
                  overviewIntervalFilter,
                  overviewProviderFilter,
                  value
                );
              }}
              slotProps={{ htmlInput: { 'data-testid': 'git-sync-overview-status-filter' } }}
            >
              <MenuItem value="all">All statuses</MenuItem>
              <MenuItem value="success">Success</MenuItem>
              <MenuItem value="failed">Failed</MenuItem>
              <MenuItem value="never">Never</MenuItem>
            </TextField>
            <Button
              size="small"
              variant="outlined"
              onClick={() => void loadSyncOverview()}
              data-testid="git-sync-overview-refresh"
            >
              Refresh
            </Button>
            <Button
              size="small"
              variant="outlined"
              disabled={overviewExporting !== null}
              onClick={() => void exportSyncOverview('csv')}
              data-testid="git-sync-overview-export-csv"
            >
              {overviewExporting === 'csv' ? 'Exporting…' : 'Export CSV'}
            </Button>
            <Button
              size="small"
              variant="outlined"
              disabled={overviewExporting !== null}
              onClick={() => void exportSyncOverview('json')}
              data-testid="git-sync-overview-export-json"
            >
              {overviewExporting === 'json' ? 'Exporting…' : 'Export JSON'}
            </Button>
            {canRetryFailedSyncs && syncOverviewQuery.data.failedLastSync > 0 && (
              <Button
                size="small"
                variant="contained"
                color="warning"
                disabled={retryFailedSyncs.isPending}
                onClick={() => retryFailedSyncs.mutate()}
                data-testid="git-sync-overview-retry-failed"
              >
                Retry failed syncs
              </Button>
            )}
            {canRetryFailedSyncs && syncOverviewQuery.data.manualSyncLinks > 0 && (
              <Button
                size="small"
                variant="contained"
                color="primary"
                disabled={enableScheduledSyncs.isPending || disableScheduledSyncs.isPending}
                onClick={() => enableScheduledSyncs.mutate()}
                data-testid="git-sync-overview-enable-scheduled"
              >
                Enable scheduled sync
              </Button>
            )}
            {canRetryFailedSyncs && syncOverviewQuery.data.scheduledSyncLinks > 0 && (
              <Button
                size="small"
                variant="outlined"
                color="primary"
                disabled={enableScheduledSyncs.isPending || disableScheduledSyncs.isPending}
                onClick={() => disableScheduledSyncs.mutate()}
                data-testid="git-sync-overview-disable-scheduled"
              >
                Disable scheduled sync
              </Button>
            )}
          </Stack>
          {syncOverviewQuery.data.items.length === 0 && (
            <Typography variant="body2" color="text.secondary">
              No projects match the current filters.
            </Typography>
          )}
          {syncOverviewQuery.data.items.map((item) => (
            <Stack
              key={item.projectId}
              direction={{ xs: 'column', sm: 'row' }}
              spacing={1}
              sx={{ alignItems: { sm: 'center' } }}
            >
              <Typography variant="body2" sx={{ minWidth: 140 }}>
                {item.projectKey} — {item.projectName}
              </Typography>
              {item.linked ? (
                <Typography
                  variant="body2"
                  color={item.lastSyncStatus === 'failed' ? 'error' : 'text.secondary'}
                >
                  {item.provider} · {item.repository} ({item.branch})
                  · {item.enabled ? 'enabled' : 'disabled'}
                  · {item.scheduledSyncEnabled ? 'scheduled' : 'manual only'}
                  · last sync {item.lastSyncStatus}
                  {item.lastSyncedAt ? ` · ${new Date(item.lastSyncedAt).toLocaleString()}` : ''}
                </Typography>
              ) : (
                <Typography variant="body2" color="text.secondary">No git link</Typography>
              )}
              <Link
                component={RouterLink}
                to={projectGitSettingsPath(item.projectId)}
                variant="body2"
                data-testid={`git-sync-overview-link-${item.projectKey}`}
              >
                Git settings
              </Link>
              {item.linked && item.lastSyncStatus === 'failed' && (
                <Button
                  size="small"
                  variant="outlined"
                  color="warning"
                  onClick={() => void viewFailedRunsForProject(item.projectId)}
                  data-testid={`git-sync-overview-view-runs-${item.projectKey}`}
                >
                  View failed runs
                </Button>
              )}
              {canRetryFailedSyncs
                && item.linked
                && item.enabled
                && item.lastSyncStatus === 'failed' && (
                <Button
                  size="small"
                  variant="contained"
                  color="warning"
                  disabled={retryingProjectId === item.projectId || retryFailedSyncs.isPending}
                  onClick={() => void retryFailedSyncForProject(item.projectId, item.projectKey)}
                  data-testid={`git-sync-overview-retry-${item.projectKey}`}
                >
                  {retryingProjectId === item.projectId ? 'Retrying…' : 'Retry sync'}
                </Button>
              )}
              {canRetryFailedSyncs && item.linked && item.enabled && !item.scheduledSyncEnabled && (
                <Button
                  size="small"
                  variant="outlined"
                  color="primary"
                  disabled={
                    togglingScheduledProjectId === item.projectId
                    || enableScheduledSyncs.isPending
                    || disableScheduledSyncs.isPending
                  }
                  onClick={() => void enableScheduledSyncForProject(item.projectId, item.projectKey)}
                  data-testid={`git-sync-overview-enable-scheduled-${item.projectKey}`}
                >
                  {togglingScheduledProjectId === item.projectId ? 'Updating…' : 'Enable scheduled'}
                </Button>
              )}
              {canRetryFailedSyncs && item.linked && item.enabled && item.scheduledSyncEnabled && (
                <Button
                  size="small"
                  variant="outlined"
                  color="primary"
                  disabled={
                    togglingScheduledProjectId === item.projectId
                    || enableScheduledSyncs.isPending
                    || disableScheduledSyncs.isPending
                  }
                  onClick={() => void disableScheduledSyncForProject(item.projectId, item.projectKey)}
                  data-testid={`git-sync-overview-disable-scheduled-${item.projectKey}`}
                >
                  {togglingScheduledProjectId === item.projectId ? 'Updating…' : 'Disable scheduled'}
                </Button>
              )}
            </Stack>
          ))}
        </Stack>
      )}

      <Stack spacing={1} id={ORG_SYNC_RUNS_SECTION_ID} data-testid="org-git-sync-runs">
        <Typography variant="subtitle2">Recent sync runs (org-wide)</Typography>
        {runTotalCount > 0 && (
          <Typography variant="body2" color="text.secondary">
            Showing {runItems.length} of {runTotalCount}
          </Typography>
        )}
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} sx={{ alignItems: 'flex-start' }}>
          <TextField
            select
            label="Project"
            size="small"
            value={runProjectFilter}
            onChange={(e) => {
              const value = e.target.value;
              setRunProjectFilter(value);
              void loadSyncRuns(runSourceFilter, runStatusFilter, value);
            }}
            slotProps={{ htmlInput: { 'data-testid': 'org-git-sync-run-project-filter' } }}
          >
            <MenuItem value="all">All projects</MenuItem>
            {(runProjectOptionsQuery.data?.items ?? [])
              .sort((a, b) => a.projectKey.localeCompare(b.projectKey))
              .map((item) => (
                <MenuItem key={item.projectId} value={item.projectId}>
                  {item.projectKey} — {item.projectName}
                </MenuItem>
              ))}
          </TextField>
          <TextField
            select
            label="Source"
            size="small"
            value={runSourceFilter}
            onChange={(e) => {
              const value = e.target.value as 'all' | 'manual' | 'scheduled' | 'webhook';
              setRunSourceFilter(value);
              void loadSyncRuns(value, runStatusFilter, runProjectFilter);
            }}
            slotProps={{ htmlInput: { 'data-testid': 'org-git-sync-run-source-filter' } }}
          >
            <MenuItem value="all">All sources</MenuItem>
            <MenuItem value="manual">Manual</MenuItem>
            <MenuItem value="scheduled">Scheduled</MenuItem>
            <MenuItem value="webhook">Webhook</MenuItem>
          </TextField>
          <TextField
            select
            label="Status"
            size="small"
            value={runStatusFilter}
            onChange={(e) => {
              const value = e.target.value as 'all' | 'success' | 'failed';
              setRunStatusFilter(value);
              void loadSyncRuns(runSourceFilter, value, runProjectFilter);
            }}
            slotProps={{ htmlInput: { 'data-testid': 'org-git-sync-run-status-filter' } }}
          >
            <MenuItem value="all">All statuses</MenuItem>
            <MenuItem value="success">Success</MenuItem>
            <MenuItem value="failed">Failed</MenuItem>
          </TextField>
          <Button
            size="small"
            variant="outlined"
            onClick={() => void loadSyncRuns()}
            data-testid="org-git-sync-runs-refresh"
          >
            Refresh
          </Button>
          <Button
            size="small"
            variant="outlined"
            disabled={runExporting !== null}
            onClick={() => void exportSyncRuns('csv')}
            data-testid="org-git-sync-runs-export-csv"
          >
            {runExporting === 'csv' ? 'Exporting…' : 'Export CSV'}
          </Button>
          <Button
            size="small"
            variant="outlined"
            disabled={runExporting !== null}
            onClick={() => void exportSyncRuns('json')}
            data-testid="org-git-sync-runs-export-json"
          >
            {runExporting === 'json' ? 'Exporting…' : 'Export JSON'}
          </Button>
        </Stack>
        {syncRunsQuery.isLoading && runItems.length === 0 && (
          <Typography variant="body2" color="text.secondary">Loading sync runs…</Typography>
        )}
        {runItems.length === 0 && !syncRunsQuery.isLoading && (
          <Typography variant="body2" color="text.secondary">No sync runs match the current filters.</Typography>
        )}
        {runItems.map((run) => (
          <Stack
            key={run.id}
            direction={{ xs: 'column', sm: 'row' }}
            spacing={1}
            sx={{ alignItems: { sm: 'center' } }}
          >
            <Typography
              variant="body2"
              color={run.status === 'failed' ? 'error' : 'text.secondary'}
            >
              {run.projectKey} — {new Date(run.finishedAt).toLocaleString()} · {run.source} · {run.status}
              {run.status === 'success' ? ` · ${run.fileCount} files` : ''}
              {run.errorMessage ? ` · ${run.errorMessage}` : ''}
            </Typography>
            <Link
              component={RouterLink}
              to={projectGitSettingsPath(run.projectId)}
              variant="body2"
              data-testid={`org-git-sync-run-link-${run.projectKey}`}
            >
              Git settings
            </Link>
          </Stack>
        ))}
        {runHasMore && (
          <Button
            size="small"
            variant="outlined"
            disabled={runLoadingMore}
            onClick={() => void loadMoreSyncRuns()}
            data-testid="org-git-sync-runs-load-more"
          >
            {runLoadingMore ? 'Loading…' : 'Load more'}
          </Button>
        )}
      </Stack>

      <Stack spacing={1}>
        <Typography variant="subtitle2">Configured providers</Typography>
        {credentialsQuery.data?.map((cred: OrgGitCredential) => (
          <Typography key={cred.provider} variant="body2" color="text.secondary">
            {cred.provider}: {cred.credentialSource}
            {cred.configured ? ' · configured' : ' · not configured'}
            {cred.lastTestStatus ? ` · last test ${cred.lastTestStatus}` : ''}
          </Typography>
        ))}
      </Stack>

      {isOwner ? (
        <Stack spacing={2}>
          <TextField
            select
            label="Provider"
            value={selectedProvider}
            onChange={(e) => onSelectProvider(e.target.value)}
            slotProps={{ htmlInput: { 'data-testid': 'git-cred-provider' } }}
          >
            {PROVIDERS.map((provider) => (
              <MenuItem key={provider} value={provider}>{provider}</MenuItem>
            ))}
          </TextField>
          <TextField
            label="Display name"
            value={displayName}
            onChange={(e) => setDisplayName(e.target.value)}
            placeholder={selectedCredential?.displayName ?? 'GitHub PAT'}
            slotProps={{ htmlInput: { 'data-testid': 'git-cred-display-name' } }}
          />
          <TextField
            label="API token"
            type="password"
            value={apiToken}
            onChange={(e) => setApiToken(e.target.value)}
            placeholder={selectedCredential?.id ? 'Leave blank to keep existing token' : 'Required'}
            slotProps={{ htmlInput: { 'data-testid': 'git-cred-token' } }}
          />
          <TextField
            label="API base URL (optional)"
            value={apiBaseUrl}
            onChange={(e) => setApiBaseUrl(e.target.value)}
            placeholder={selectedCredential?.apiBaseUrl ?? ''}
            helperText="Self-managed GitLab or custom API host."
            slotProps={{ htmlInput: { 'data-testid': 'git-cred-base-url' } }}
          />
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
            <Button
              variant="contained"
              disabled={upsert.isPending || !displayName.trim()}
              onClick={() => upsert.mutate()}
              data-testid="git-cred-save"
            >
              Save credential
            </Button>
            <Button
              variant="outlined"
              disabled={test.isPending}
              onClick={() => test.mutate()}
              data-testid="git-cred-test"
            >
              Test connection
            </Button>
            {selectedCredential?.id && (
              <Button
                variant="outlined"
                color="warning"
                disabled={remove.isPending}
                onClick={() => remove.mutate()}
                data-testid="git-cred-delete"
              >
                Remove org credential
              </Button>
            )}
          </Stack>
          {testResult && (
            <Stack spacing={0.5}>
              <Typography variant="subtitle2">Test result: {testResult.message}</Typography>
              {testResult.checks.map((check) => (
                <Typography key={check.name} variant="body2" color="text.secondary">
                  {check.name}: {check.status} — {check.message}
                </Typography>
              ))}
            </Stack>
          )}
        </Stack>
      ) : (
        <Alert severity="info">Only organization owners can manage git credentials.</Alert>
      )}

      {eventsQuery.data && eventsQuery.data.length > 0 && (
        <Stack spacing={0.5}>
          <Typography variant="subtitle2">Rotation audit</Typography>
          {eventsQuery.data.map((event) => (
            <Typography key={event.id} variant="body2" color="text.secondary">
              {new Date(event.createdAt).toLocaleString()} · {event.provider} · {event.action}
              {event.displayName ? ` · ${event.displayName}` : ''}
            </Typography>
          ))}
        </Stack>
      )}
    </Stack>
  );
}
