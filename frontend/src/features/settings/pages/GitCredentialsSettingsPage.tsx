import {
  Alert,
  Box,
  Button,
  Checkbox,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  IconButton,
  Link,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useRef, useState, useCallback } from 'react';
import { Link as RouterLink, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { ApiError } from '../../../shared/api/types';
import { useAuthStore } from '../../auth/store/authStore';
import { organizationsApi } from '../../projects/api/organizationsApi';
import {
  gitCredentialsApi,
  type GitConnectionTestResult,
  type OrgGitCredential,
  type OrgGitSyncRunItem,
  type OrgGitSyncOverviewFilters,
} from '../api/gitCredentialsApi';
import {
  clearLocalSavedOverviewPresets,
  clearLocalSavedRunPresets,
  loadSavedOverviewPresets,
  loadSavedRunPresets,
  type OverviewFiltersSnapshot,
  type RunFiltersSnapshot,
  type SavedOverviewFilterPreset,
  type SavedRunFilterPreset,
} from '../gitSyncSavedFilterPresets';
import {
  ORG_SYNC_RUNS_SECTION_ID,
  buildGitSyncOverviewFilterUrl,
  buildGitSyncPageFilterUrl,
  buildGitSyncRunFilterUrl,
  countActiveOverviewFilterDimensions,
  countActiveRunFilterDimensions,
  gitSyncRunsSectionHash,
  readOverviewFiltersFromSearchParams,
  readRunFiltersFromSearchParams,
  writeOverviewFiltersToSearchParams,
  writeRunFiltersToSearchParams,
  type OverviewEnabledFilter,
  type OverviewFilterState,
  type OverviewIntervalFilter,
  type OverviewLinkedFilter,
  type OverviewProviderFilter,
  type OverviewScheduledFilter,
  type OverviewStatusFilter,
  type RunFilterState,
  type RunProjectFilter,
  type RunSourceFilter,
  type RunStatusFilter,
} from '../gitSyncFilterUrl';

const PROVIDERS = ['github', 'gitlab', 'bitbucket'] as const;
const GIT_SYNC_SECTION_HASH = '#git-repository-sync';

type OverviewFilterPreset = {
  id: string;
  label: string;
  filters: {
    linked: OverviewLinkedFilter;
    enabled: OverviewEnabledFilter;
    scheduled: OverviewScheduledFilter;
    interval: OverviewIntervalFilter;
    provider: OverviewProviderFilter;
    status: OverviewStatusFilter;
  };
};

const OVERVIEW_FILTER_PRESETS: OverviewFilterPreset[] = [
  {
    id: 'failed-enabled',
    label: 'Failed (enabled)',
    filters: {
      linked: 'linked',
      enabled: 'enabled',
      scheduled: 'all',
      interval: 'all',
      provider: 'all',
      status: 'failed',
    },
  },
  {
    id: 'manual-enabled',
    label: 'Manual only',
    filters: {
      linked: 'linked',
      enabled: 'enabled',
      scheduled: 'manual',
      interval: 'all',
      provider: 'all',
      status: 'all',
    },
  },
  {
    id: 'scheduled-enabled',
    label: 'Scheduled',
    filters: {
      linked: 'linked',
      enabled: 'enabled',
      scheduled: 'scheduled',
      interval: 'all',
      provider: 'all',
      status: 'all',
    },
  },
  {
    id: 'failed-scheduled',
    label: 'Failed scheduled',
    filters: {
      linked: 'linked',
      enabled: 'enabled',
      scheduled: 'scheduled',
      interval: 'all',
      provider: 'all',
      status: 'failed',
    },
  },
  {
    id: 'custom-interval',
    label: 'Custom interval',
    filters: {
      linked: 'linked',
      enabled: 'enabled',
      scheduled: 'all',
      interval: 'custom',
      provider: 'all',
      status: 'all',
    },
  },
  {
    id: 'never-synced',
    label: 'Never synced',
    filters: {
      linked: 'linked',
      enabled: 'enabled',
      scheduled: 'all',
      interval: 'all',
      provider: 'all',
      status: 'never',
    },
  },
  {
    id: 'unlinked',
    label: 'Unlinked',
    filters: {
      linked: 'unlinked',
      enabled: 'all',
      scheduled: 'all',
      interval: 'all',
      provider: 'all',
      status: 'all',
    },
  },
  {
    id: 'github-enabled',
    label: 'GitHub',
    filters: {
      linked: 'linked',
      enabled: 'enabled',
      scheduled: 'all',
      interval: 'all',
      provider: 'github',
      status: 'all',
    },
  },
  {
    id: 'gitlab-enabled',
    label: 'GitLab',
    filters: {
      linked: 'linked',
      enabled: 'enabled',
      scheduled: 'all',
      interval: 'all',
      provider: 'gitlab',
      status: 'all',
    },
  },
  {
    id: 'bitbucket-enabled',
    label: 'Bitbucket',
    filters: {
      linked: 'linked',
      enabled: 'enabled',
      scheduled: 'all',
      interval: 'all',
      provider: 'bitbucket',
      status: 'all',
    },
  },
];

type RunFilterPreset = {
  id: string;
  label: string;
  filters: RunFilterState;
};

const RUN_FILTER_PRESETS: RunFilterPreset[] = [
  {
    id: 'failed',
    label: 'Failed',
    filters: { source: 'all', status: 'failed', project: 'all' },
  },
  {
    id: 'success',
    label: 'Success',
    filters: { source: 'all', status: 'success', project: 'all' },
  },
  {
    id: 'manual',
    label: 'Manual',
    filters: { source: 'manual', status: 'all', project: 'all' },
  },
  {
    id: 'scheduled',
    label: 'Scheduled',
    filters: { source: 'scheduled', status: 'all', project: 'all' },
  },
  {
    id: 'webhook',
    label: 'Webhook',
    filters: { source: 'webhook', status: 'all', project: 'all' },
  },
  {
    id: 'failed-manual',
    label: 'Failed manual',
    filters: { source: 'manual', status: 'failed', project: 'all' },
  },
  {
    id: 'failed-scheduled',
    label: 'Failed scheduled',
    filters: { source: 'scheduled', status: 'failed', project: 'all' },
  },
];

function projectGitSettingsPath(projectId: string) {
  return `/projects/${projectId}/settings${GIT_SYNC_SECTION_HASH}`;
}

export function GitCredentialsSettingsPage() {
  const org = useAuthStore((s) => s.organization);
  const user = useAuthStore((s) => s.user);
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const overviewUrlInitializedRef = useRef(false);
  const runUrlInitializedRef = useRef(false);

  useEffect(() => {
    const onPopState = () => {
      overviewUrlInitializedRef.current = false;
      runUrlInitializedRef.current = false;
    };
    window.addEventListener('popstate', onPopState);
    return () => window.removeEventListener('popstate', onPopState);
  }, []);

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
  const [bulkSummaryExporting, setBulkSummaryExporting] = useState<'csv' | 'json' | null>(null);
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
  const [clearingIntervalProjectId, setClearingIntervalProjectId] = useState<string | null>(null);
  const [intervalDialogProject, setIntervalDialogProject] = useState<{
    projectId: string;
    projectKey: string;
    currentMinutes: number | null;
  } | null>(null);
  const [intervalMinutesInput, setIntervalMinutesInput] = useState('');
  const [settingIntervalProjectId, setSettingIntervalProjectId] = useState<string | null>(null);
  const [bulkSetIntervalDialogOpen, setBulkSetIntervalDialogOpen] = useState(false);
  const [settingBulkInterval, setSettingBulkInterval] = useState(false);
  const [savePresetDialog, setSavePresetDialog] = useState<'overview' | 'run' | null>(null);
  const [savePresetNameInput, setSavePresetNameInput] = useState('');
  const [savePresetShareWithOrg, setSavePresetShareWithOrg] = useState(false);
  const [savingFilterPreset, setSavingFilterPreset] = useState(false);
  const [renamePreset, setRenamePreset] = useState<{
    id: string;
    scope: 'overview' | 'run';
    label: string;
  } | null>(null);
  const [renamePresetNameInput, setRenamePresetNameInput] = useState('');
  const [renamingFilterPreset, setRenamingFilterPreset] = useState(false);

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

  const overviewFilterCountsQuery = useQuery({
    queryKey: ['org-git-sync-overview-filter-counts', org?.id],
    queryFn: () => gitCredentialsApi.getSyncOverviewFilterCounts(org!.id),
    enabled: !!org?.id,
  });

  const filterPresetsQuery = useQuery({
    queryKey: ['org-git-sync-filter-presets', org?.id],
    queryFn: () => gitCredentialsApi.listFilterPresets(org!.id),
    enabled: !!org?.id,
  });

  const savedOverviewPresets: SavedOverviewFilterPreset[] = (filterPresetsQuery.data ?? [])
    .filter((preset) => preset.scope === 'overview')
    .map((preset) => ({
      id: preset.id,
      label: preset.label,
      filters: preset.filters as OverviewFiltersSnapshot,
      visibility: preset.visibility,
      createdByUserId: preset.createdByUserId,
      createdByDisplayName: preset.createdByDisplayName,
    }));

  const savedRunPresets: SavedRunFilterPreset[] = (filterPresetsQuery.data ?? [])
    .filter((preset) => preset.scope === 'runs')
    .map((preset) => ({
      id: preset.id,
      label: preset.label,
      filters: preset.filters as RunFiltersSnapshot,
      visibility: preset.visibility,
      createdByUserId: preset.createdByUserId,
      createdByDisplayName: preset.createdByDisplayName,
    }));

  useEffect(() => {
    if (!org?.id || !filterPresetsQuery.data) return;
    const migrateKey = `aistudio.org-git-sync.presets-migrated.${org.id}`;
    if (localStorage.getItem(migrateKey) === '1') return;

    const localOverview = loadSavedOverviewPresets(org.id);
    const localRun = loadSavedRunPresets(org.id);
    if (filterPresetsQuery.data.length > 0) {
      clearLocalSavedOverviewPresets(org.id);
      clearLocalSavedRunPresets(org.id);
      localStorage.setItem(migrateKey, '1');
      return;
    }
    if (localOverview.length === 0 && localRun.length === 0) {
      localStorage.setItem(migrateKey, '1');
      return;
    }

    void (async () => {
      let migrationFailed = false;
      for (const preset of localOverview) {
        try {
          await gitCredentialsApi.createFilterPreset(org.id, {
            scope: 'overview',
            label: preset.label,
            filters: preset.filters,
          });
        } catch {
          migrationFailed = true;
        }
      }
      for (const preset of localRun) {
        try {
          await gitCredentialsApi.createFilterPreset(org.id, {
            scope: 'runs',
            label: preset.label,
            filters: preset.filters,
          });
        } catch {
          migrationFailed = true;
        }
      }
      if (migrationFailed) return;
      clearLocalSavedOverviewPresets(org.id);
      clearLocalSavedRunPresets(org.id);
      localStorage.setItem(migrateKey, '1');
      await queryClient.invalidateQueries({ queryKey: ['org-git-sync-filter-presets', org.id] });
    })();
  }, [org?.id, filterPresetsQuery.data, queryClient]);

  const isOwnerOrAdmin =
    orgQuery.data?.role === 'OWNER' || orgQuery.data?.role === 'ADMIN';

  const bulkActionsSummaryQuery = useQuery({
    queryKey: [
      'org-git-sync-bulk-actions-summary',
      org?.id,
      overviewLinkedFilter,
      overviewEnabledFilter,
      overviewScheduledSyncFilter,
      overviewIntervalFilter,
      overviewProviderFilter,
      overviewStatusFilter,
    ],
    queryFn: () => gitCredentialsApi.getBulkActionsSummary(org!.id, buildOverviewFilters()),
    enabled: !!org?.id && isOwnerOrAdmin,
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

  const runFilterCountsQuery = useQuery({
    queryKey: ['org-git-sync-runs-filter-counts', org?.id],
    queryFn: () => gitCredentialsApi.getSyncRunFilterCounts(org!.id),
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
    void queryClient.invalidateQueries({ queryKey: ['org-git-sync-runs-filter-counts', org.id] });
    void queryClient.invalidateQueries({ queryKey: ['org-git-sync-filter-presets', org.id] });
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

  function hasActiveRunFilters() {
    return runSourceFilter !== 'all' || runStatusFilter !== 'all' || runProjectFilter !== 'all';
  }

  function currentRunFilterStateForUrl(): RunFilterState {
    return {
      source: runSourceFilter,
      status: runStatusFilter,
      project: runProjectFilter,
    };
  }

  const syncFilterNavigate = useCallback((nextParams: URLSearchParams, runForHash: RunFilterState) => {
    navigate(
      {
        pathname: location.pathname,
        search: nextParams.toString(),
        hash: gitSyncRunsSectionHash(runForHash),
      },
      { replace: true }
    );
  }, [navigate, location.pathname]);

  async function applyRunFilterState(
    source: RunSourceFilter,
    status: RunStatusFilter,
    project: RunProjectFilter,
    scrollToRuns = false
  ) {
    setRunSourceFilter(source);
    setRunStatusFilter(status);
    setRunProjectFilter(project);
    const nextParams = new URLSearchParams(searchParams);
    writeRunFiltersToSearchParams(nextParams, { source, status, project });
    syncFilterNavigate(nextParams, { source, status, project });
    await loadSyncRuns(source, status, project);
    if (scrollToRuns) {
      document.getElementById(ORG_SYNC_RUNS_SECTION_ID)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }

  async function clearRunFilters() {
    await applyRunFilterState('all', 'all', 'all');
  }

  async function copyRunFilterLink() {
    setError(null);
    try {
      const url = buildGitSyncRunFilterUrl(
        location.pathname,
        currentOverviewFilterState(),
        currentRunFilterState(),
        searchParams,
        window.location.origin
      );
      await navigator.clipboard.writeText(url);
      setMessage('Filtered sync runs link copied to clipboard');
    } catch {
      setMessage(null);
      setError('Failed to copy sync runs filter link');
    }
  }

  function getActiveRunFilterChips() {
    const chips: {
      id: string;
      label: string;
      clear: () => Promise<void>;
    }[] = [];

    if (runSourceFilter !== 'all') {
      const sourceLabels: Record<Exclude<RunSourceFilter, 'all'>, string> = {
        manual: 'Source: manual',
        scheduled: 'Source: scheduled',
        webhook: 'Source: webhook',
      };
      chips.push({
        id: 'runSource',
        label: sourceLabels[runSourceFilter],
        clear: () => applyRunFilterState('all', runStatusFilter, runProjectFilter),
      });
    }
    if (runStatusFilter !== 'all') {
      chips.push({
        id: 'runStatus',
        label: runStatusFilter === 'success' ? 'Status: success' : 'Status: failed',
        clear: () => applyRunFilterState(runSourceFilter, 'all', runProjectFilter),
      });
    }
    if (runProjectFilter !== 'all') {
      const projectOption = runProjectOptionsQuery.data?.items.find(
        (item) => item.projectId === runProjectFilter
      );
      chips.push({
        id: 'runProject',
        label: projectOption
          ? `Project: ${projectOption.projectKey}`
          : `Project: ${runProjectFilter}`,
        clear: () => applyRunFilterState(runSourceFilter, runStatusFilter, 'all'),
      });
    }

    return chips;
  }

  function isRunPresetActive(preset: RunFilterPreset) {
    const f = preset.filters;
    return runSourceFilter === f.source
      && runStatusFilter === f.status
      && runProjectFilter === f.project;
  }

  async function applyRunPreset(preset: RunFilterPreset) {
    const f = preset.filters;
    await applyRunFilterState(f.source, f.status, f.project);
  }

  function formatRunPresetLabel(preset: RunFilterPreset): string {
    const count = runFilterCountsQuery.data?.presets.find((item) => item.id === preset.id)?.count;
    if (count == null) return preset.label;
    return `${preset.label} (${count})`;
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
    const filters: OrgGitSyncOverviewFilters = {
      linked: linked === 'all' ? undefined : linked === 'linked',
      enabled: enabled === 'all' ? undefined : enabled === 'enabled',
      scheduledSyncEnabled: scheduled === 'all' ? undefined : scheduled === 'scheduled',
      customSyncInterval: interval === 'all' ? undefined : interval === 'custom',
      provider: provider === 'all' ? undefined : provider,
      lastSyncStatus: status === 'all' ? undefined : status,
    };
    await queryClient.fetchQuery({
      queryKey: ['org-git-sync-overview', org.id, linked, enabled, scheduled, interval, provider, status],
      queryFn: () => gitCredentialsApi.getSyncOverview(org.id, filters),
    });
    void queryClient.invalidateQueries({ queryKey: ['org-git-sync-overview-filter-counts', org.id] });
    void queryClient.invalidateQueries({ queryKey: ['org-git-sync-filter-presets', org.id] });
  }

  function buildOverviewFilters(): OrgGitSyncOverviewFilters {
    return {
      linked: overviewLinkedFilter === 'all' ? undefined : overviewLinkedFilter === 'linked',
      enabled: overviewEnabledFilter === 'all' ? undefined : overviewEnabledFilter === 'enabled',
      scheduledSyncEnabled:
        overviewScheduledSyncFilter === 'all' ? undefined : overviewScheduledSyncFilter === 'scheduled',
      customSyncInterval: overviewIntervalFilter === 'all' ? undefined : overviewIntervalFilter === 'custom',
      provider: overviewProviderFilter === 'all' ? undefined : overviewProviderFilter,
      lastSyncStatus: overviewStatusFilter === 'all' ? undefined : overviewStatusFilter,
    };
  }

  function hasActiveOverviewFilters() {
    return overviewLinkedFilter !== 'all'
      || overviewEnabledFilter !== 'all'
      || overviewScheduledSyncFilter !== 'all'
      || overviewIntervalFilter !== 'all'
      || overviewProviderFilter !== 'all'
      || overviewStatusFilter !== 'all';
  }

  function isOverviewPresetActive(preset: OverviewFilterPreset) {
    const f = preset.filters;
    return overviewLinkedFilter === f.linked
      && overviewEnabledFilter === f.enabled
      && overviewScheduledSyncFilter === f.scheduled
      && overviewIntervalFilter === f.interval
      && overviewProviderFilter === f.provider
      && overviewStatusFilter === f.status;
  }

  function getActiveOverviewFilterChips() {
    const chips: {
      id: string;
      label: string;
      clear: () => Promise<void>;
    }[] = [];

    if (overviewLinkedFilter !== 'all') {
      chips.push({
        id: 'linked',
        label: overviewLinkedFilter === 'linked' ? 'Linked only' : 'Unlinked only',
        clear: () =>
          applyOverviewFilterState(
            'all',
            overviewEnabledFilter,
            overviewScheduledSyncFilter,
            overviewIntervalFilter,
            overviewProviderFilter,
            overviewStatusFilter
          ),
      });
    }
    if (overviewEnabledFilter !== 'all') {
      chips.push({
        id: 'enabled',
        label: overviewEnabledFilter === 'enabled' ? 'Enabled only' : 'Disabled only',
        clear: () =>
          applyOverviewFilterState(
            overviewLinkedFilter,
            'all',
            overviewScheduledSyncFilter,
            overviewIntervalFilter,
            overviewProviderFilter,
            overviewStatusFilter
          ),
      });
    }
    if (overviewScheduledSyncFilter !== 'all') {
      chips.push({
        id: 'scheduled',
        label:
          overviewScheduledSyncFilter === 'scheduled' ? 'Scheduled only' : 'Manual only',
        clear: () =>
          applyOverviewFilterState(
            overviewLinkedFilter,
            overviewEnabledFilter,
            'all',
            overviewIntervalFilter,
            overviewProviderFilter,
            overviewStatusFilter
          ),
      });
    }
    if (overviewIntervalFilter !== 'all') {
      chips.push({
        id: 'interval',
        label:
          overviewIntervalFilter === 'custom' ? 'Custom interval' : 'Platform default',
        clear: () =>
          applyOverviewFilterState(
            overviewLinkedFilter,
            overviewEnabledFilter,
            overviewScheduledSyncFilter,
            'all',
            overviewProviderFilter,
            overviewStatusFilter
          ),
      });
    }
    if (overviewProviderFilter !== 'all') {
      chips.push({
        id: 'provider',
        label: overviewProviderFilter,
        clear: () =>
          applyOverviewFilterState(
            overviewLinkedFilter,
            overviewEnabledFilter,
            overviewScheduledSyncFilter,
            overviewIntervalFilter,
            'all',
            overviewStatusFilter
          ),
      });
    }
    if (overviewStatusFilter !== 'all') {
      const statusLabels: Record<Exclude<OverviewStatusFilter, 'all'>, string> = {
        success: 'Last sync: success',
        failed: 'Last sync: failed',
        never: 'Last sync: never',
      };
      chips.push({
        id: 'lastSync',
        label: statusLabels[overviewStatusFilter],
        clear: () =>
          applyOverviewFilterState(
            overviewLinkedFilter,
            overviewEnabledFilter,
            overviewScheduledSyncFilter,
            overviewIntervalFilter,
            overviewProviderFilter,
            'all'
          ),
      });
    }

    return chips;
  }

  async function applyOverviewFilterState(
    linked: OverviewLinkedFilter,
    enabled: OverviewEnabledFilter,
    scheduled: OverviewScheduledFilter,
    interval: OverviewIntervalFilter,
    provider: OverviewProviderFilter,
    status: OverviewStatusFilter
  ) {
    setOverviewLinkedFilter(linked);
    setOverviewEnabledFilter(enabled);
    setOverviewScheduledSyncFilter(scheduled);
    setOverviewIntervalFilter(interval);
    setOverviewProviderFilter(provider);
    setOverviewStatusFilter(status);
    const nextParams = new URLSearchParams(searchParams);
    writeOverviewFiltersToSearchParams(nextParams, {
      linked,
      enabled,
      scheduled,
      interval,
      provider,
      status,
    });
    syncFilterNavigate(nextParams, currentRunFilterStateForUrl());
    await loadSyncOverview(linked, enabled, scheduled, interval, provider, status);
  }

  useEffect(() => {
    if (!org?.id || overviewUrlInitializedRef.current) return;
    overviewUrlInitializedRef.current = true;
    const fromUrl = readOverviewFiltersFromSearchParams(searchParams);
    if (fromUrl) {
      setOverviewLinkedFilter(fromUrl.linked);
      setOverviewEnabledFilter(fromUrl.enabled);
      setOverviewScheduledSyncFilter(fromUrl.scheduled);
      setOverviewIntervalFilter(fromUrl.interval);
      setOverviewProviderFilter(fromUrl.provider);
      setOverviewStatusFilter(fromUrl.status);
      const nextParams = new URLSearchParams(searchParams);
      writeOverviewFiltersToSearchParams(nextParams, fromUrl);
      const runForHash =
        readRunFiltersFromSearchParams(searchParams) ?? {
          source: 'all',
          status: 'all',
          project: 'all',
        };
      syncFilterNavigate(nextParams, runForHash);
      void loadSyncOverview(
        fromUrl.linked,
        fromUrl.enabled,
        fromUrl.scheduled,
        fromUrl.interval,
        fromUrl.provider,
        fromUrl.status
      );
      return;
    }
    if (hasActiveOverviewFilters()) {
      setOverviewLinkedFilter('all');
      setOverviewEnabledFilter('all');
      setOverviewScheduledSyncFilter('all');
      setOverviewIntervalFilter('all');
      setOverviewProviderFilter('all');
      setOverviewStatusFilter('all');
      const nextParams = new URLSearchParams(searchParams);
      writeOverviewFiltersToSearchParams(nextParams, {
        linked: 'all',
        enabled: 'all',
        scheduled: 'all',
        interval: 'all',
        provider: 'all',
        status: 'all',
      });
      syncFilterNavigate(nextParams, currentRunFilterStateForUrl());
      void loadSyncOverview('all', 'all', 'all', 'all', 'all', 'all');
    }
  }, [org?.id, searchParams, syncFilterNavigate, location.pathname]);

  useEffect(() => {
    if (!org?.id || runUrlInitializedRef.current) return;
    runUrlInitializedRef.current = true;
    const fromUrl = readRunFiltersFromSearchParams(searchParams);
    if (fromUrl) {
      setRunSourceFilter(fromUrl.source);
      setRunStatusFilter(fromUrl.status);
      setRunProjectFilter(fromUrl.project);
      const nextParams = new URLSearchParams(searchParams);
      writeRunFiltersToSearchParams(nextParams, fromUrl);
      syncFilterNavigate(nextParams, fromUrl);
      void loadSyncRuns(fromUrl.source, fromUrl.status, fromUrl.project);
      document.getElementById(ORG_SYNC_RUNS_SECTION_ID)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
      return;
    }
    if (hasActiveRunFilters()) {
      setRunSourceFilter('all');
      setRunStatusFilter('all');
      setRunProjectFilter('all');
      const nextParams = new URLSearchParams(searchParams);
      writeRunFiltersToSearchParams(nextParams, { source: 'all', status: 'all', project: 'all' });
      syncFilterNavigate(nextParams, { source: 'all', status: 'all', project: 'all' });
      void loadSyncRuns('all', 'all', 'all');
    }
  }, [org?.id, searchParams, syncFilterNavigate, location.pathname]);

  async function applyOverviewPreset(preset: OverviewFilterPreset) {
    const f = preset.filters;
    await applyOverviewFilterState(
      f.linked,
      f.enabled,
      f.scheduled,
      f.interval,
      f.provider,
      f.status
    );
  }

  function formatOverviewPresetLabel(preset: OverviewFilterPreset): string {
    const count = overviewFilterCountsQuery.data?.presets.find((item) => item.id === preset.id)?.count;
    if (count == null) return preset.label;
    return `${preset.label} (${count})`;
  }

  function formatSavedOverviewPresetLabel(preset: SavedOverviewFilterPreset): string {
    const apiPreset = filterPresetsQuery.data?.find((item) => item.id === preset.id);
    const count = apiPreset?.count;
    let label = preset.label;
    if (count != null) label = `${label} (${count})`;
    if (preset.visibility === 'org') label = `${label} · Org`;
    return label;
  }

  function formatSavedRunPresetLabel(preset: SavedRunFilterPreset): string {
    const apiPreset = filterPresetsQuery.data?.find((item) => item.id === preset.id);
    const count = apiPreset?.count;
    let label = preset.label;
    if (count != null) label = `${label} (${count})`;
    if (preset.visibility === 'org') label = `${label} · Org`;
    return label;
  }

  function canDeleteSavedPreset(presetId: string): boolean {
    const preset = filterPresetsQuery.data?.find((item) => item.id === presetId);
    if (!preset) return false;
    if (preset.visibility === 'private') return true;
    if (preset.createdByUserId === user?.id) return true;
    return isOwnerOrAdmin;
  }

  function savedPresetChipColor(preset: SavedOverviewFilterPreset | SavedRunFilterPreset, active: boolean) {
    if (active) return 'secondary';
    if (preset.visibility === 'org') return 'info';
    return 'default';
  }

  function openSaveOverviewPresetDialog() {
    setSavePresetNameInput('');
    setSavePresetShareWithOrg(false);
    setSavePresetDialog('overview');
  }

  function openSaveRunPresetDialog() {
    setSavePresetNameInput('');
    setSavePresetShareWithOrg(false);
    setSavePresetDialog('run');
  }

  function closeSavePresetDialog() {
    setSavePresetDialog(null);
    setSavePresetNameInput('');
    setSavePresetShareWithOrg(false);
  }

  function openRenamePresetDialog(
    preset: SavedOverviewFilterPreset | SavedRunFilterPreset,
    scope: 'overview' | 'run'
  ) {
    setRenamePreset({ id: preset.id, scope, label: preset.label });
    setRenamePresetNameInput(preset.label);
  }

  function closeRenamePresetDialog() {
    setRenamePreset(null);
    setRenamePresetNameInput('');
  }

  async function submitRenamePreset() {
    if (!org?.id || !renamePreset) return;
    const nextLabel = normalizeSavePresetName(renamePresetNameInput);
    if (!nextLabel) return;
    setRenamingFilterPreset(true);
    setError(null);
    try {
      await gitCredentialsApi.renameFilterPreset(org.id, renamePreset.id, { label: nextLabel });
      setMessage(
        renamePreset.scope === 'overview'
          ? `Overview preset renamed to "${nextLabel}"`
          : `Run preset renamed to "${nextLabel}"`
      );
      await queryClient.invalidateQueries({ queryKey: ['org-git-sync-filter-presets', org.id] });
      closeRenamePresetDialog();
    } catch (err) {
      setMessage(null);
      setError(err instanceof ApiError ? err.message : 'Failed to rename filter preset');
    } finally {
      setRenamingFilterPreset(false);
    }
  }

  async function submitSavePreset() {
    if (!org?.id || !savePresetDialog) return;
    setSavingFilterPreset(true);
    setError(null);
    try {
      if (savePresetDialog === 'overview') {
        await gitCredentialsApi.createFilterPreset(org.id, {
          scope: 'overview',
          label: savePresetNameInput,
          filters: currentOverviewFilterState(),
          visibility: savePresetShareWithOrg ? 'org' : 'private',
        });
        setMessage(
          savePresetShareWithOrg
            ? `Overview preset "${normalizeSavePresetName(savePresetNameInput)}" shared with organization`
            : `Overview preset "${normalizeSavePresetName(savePresetNameInput)}" saved`
        );
      } else {
        await gitCredentialsApi.createFilterPreset(org.id, {
          scope: 'runs',
          label: savePresetNameInput,
          filters: currentRunFilterState(),
          visibility: savePresetShareWithOrg ? 'org' : 'private',
        });
        setMessage(
          savePresetShareWithOrg
            ? `Run preset "${normalizeSavePresetName(savePresetNameInput)}" shared with organization`
            : `Run preset "${normalizeSavePresetName(savePresetNameInput)}" saved`
        );
      }
      await queryClient.invalidateQueries({ queryKey: ['org-git-sync-filter-presets', org.id] });
      closeSavePresetDialog();
    } catch (err) {
      setMessage(null);
      setError(err instanceof ApiError ? err.message : 'Failed to save filter preset');
    } finally {
      setSavingFilterPreset(false);
    }
  }

  function normalizeSavePresetName(label: string) {
    return label.trim().slice(0, 40);
  }

  async function deleteSavedOverviewPreset(presetId: string) {
    if (!org?.id) return;
    try {
      await gitCredentialsApi.deleteFilterPreset(org.id, presetId);
      await queryClient.invalidateQueries({ queryKey: ['org-git-sync-filter-presets', org.id] });
    } catch (err) {
      setMessage(null);
      setError(err instanceof ApiError ? err.message : 'Failed to delete overview preset');
    }
  }

  async function deleteSavedRunPreset(presetId: string) {
    if (!org?.id) return;
    try {
      await gitCredentialsApi.deleteFilterPreset(org.id, presetId);
      await queryClient.invalidateQueries({ queryKey: ['org-git-sync-filter-presets', org.id] });
    } catch (err) {
      setMessage(null);
      setError(err instanceof ApiError ? err.message : 'Failed to delete run preset');
    }
  }

  async function applySavedOverviewPreset(preset: SavedOverviewFilterPreset) {
    const f = preset.filters;
    await applyOverviewFilterState(
      f.linked,
      f.enabled,
      f.scheduled,
      f.interval,
      f.provider,
      f.status
    );
  }

  async function applySavedRunPreset(preset: SavedRunFilterPreset) {
    const f = preset.filters;
    await applyRunFilterState(f.source, f.status, f.project);
  }

  function isSavedOverviewPresetActive(preset: SavedOverviewFilterPreset) {
    return isOverviewPresetActive(preset);
  }

  function isSavedRunPresetActive(preset: SavedRunFilterPreset) {
    return isRunPresetActive(preset);
  }

  async function copyOverviewFilterLink() {
    setError(null);
    try {
      const url = buildGitSyncOverviewFilterUrl(
        location.pathname,
        currentOverviewFilterState(),
        searchParams,
        window.location.origin
      );
      await navigator.clipboard.writeText(url);
      setMessage('Filtered overview link copied to clipboard');
    } catch {
      setMessage(null);
      setError('Failed to copy filtered overview link');
    }
  }

  async function exportSyncOverview(format: 'csv' | 'json') {
    if (!org?.id) return;
    setOverviewExporting(format);
    setError(null);
    try {
      await gitCredentialsApi.downloadSyncOverviewExport(org.id, format, buildOverviewFilters());
    } catch (err) {
      setMessage(null);
      setError(err instanceof ApiError ? err.message : 'Failed to export overview');
    } finally {
      setOverviewExporting(null);
    }
  }

  async function exportBulkActionsSummary(format: 'csv' | 'json') {
    if (!org?.id) return;
    setBulkSummaryExporting(format);
    setError(null);
    try {
      await gitCredentialsApi.downloadBulkActionsSummaryExport(org.id, format, buildOverviewFilters());
    } catch (err) {
      setMessage(null);
      setError(err instanceof ApiError ? err.message : 'Failed to export bulk actions summary');
    } finally {
      setBulkSummaryExporting(null);
    }
  }

  async function clearOverviewFilters() {
    await applyOverviewFilterState('all', 'all', 'all', 'all', 'all', 'all');
  }

  function currentOverviewFilterState(): OverviewFilterState {
    return {
      linked: overviewLinkedFilter,
      enabled: overviewEnabledFilter,
      scheduled: overviewScheduledSyncFilter,
      interval: overviewIntervalFilter,
      provider: overviewProviderFilter,
      status: overviewStatusFilter,
    };
  }

  function currentRunFilterState(): RunFilterState {
    return {
      source: runSourceFilter,
      status: runStatusFilter,
      project: runProjectFilter,
    };
  }

  function hasAnyPageFilters() {
    return hasActiveOverviewFilters() || hasActiveRunFilters();
  }

  function pageActiveFilterCount() {
    return countActiveOverviewFilterDimensions(currentOverviewFilterState())
      + countActiveRunFilterDimensions(currentRunFilterState());
  }

  async function clearAllPageFilters() {
    setOverviewLinkedFilter('all');
    setOverviewEnabledFilter('all');
    setOverviewScheduledSyncFilter('all');
    setOverviewIntervalFilter('all');
    setOverviewProviderFilter('all');
    setOverviewStatusFilter('all');
    setRunSourceFilter('all');
    setRunStatusFilter('all');
    setRunProjectFilter('all');
    const nextParams = new URLSearchParams(searchParams);
    writeOverviewFiltersToSearchParams(nextParams, {
      linked: 'all',
      enabled: 'all',
      scheduled: 'all',
      interval: 'all',
      provider: 'all',
      status: 'all',
    });
    writeRunFiltersToSearchParams(nextParams, { source: 'all', status: 'all', project: 'all' });
    syncFilterNavigate(nextParams, { source: 'all', status: 'all', project: 'all' });
    await loadSyncOverview('all', 'all', 'all', 'all', 'all', 'all');
    await loadSyncRuns('all', 'all', 'all');
  }

  async function copyFullPageFilterLink() {
    setError(null);
    try {
      const url = buildGitSyncPageFilterUrl(
        location.pathname,
        currentOverviewFilterState(),
        currentRunFilterState(),
        searchParams,
        window.location.origin
      );
      await navigator.clipboard.writeText(url);
      setMessage('Page link with overview and run filters copied to clipboard');
    } catch {
      setMessage(null);
      setError('Failed to copy page filter link');
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

  const filteredManualSyncLinks =
    syncOverviewQuery.data?.items.filter((item) => item.linked && item.enabled && !item.scheduledSyncEnabled)
      .length ?? 0;
  const filteredScheduledSyncLinks =
    syncOverviewQuery.data?.items.filter((item) => item.linked && item.enabled && item.scheduledSyncEnabled)
      .length ?? 0;
  const filteredFailedLastSync =
    syncOverviewQuery.data?.items.filter(
      (item) => item.linked && item.enabled && item.lastSyncStatus === 'failed'
    ).length ?? 0;
  const filteredCustomSyncIntervalLinks =
    syncOverviewQuery.data?.items.filter(
      (item) => item.linked && item.enabled && item.scheduledSyncIntervalMinutes != null
    ).length ?? 0;
  const filteredEnabledLinkedLinks =
    syncOverviewQuery.data?.items.filter((item) => item.linked && item.enabled).length ?? 0;
  const overviewFiltersActive = hasActiveOverviewFilters();
  const bulkScheduledScopeLabel = overviewFiltersActive ? ' (filtered)' : '';
  const bulkRetryScopeLabel = overviewFiltersActive ? ' (filtered)' : '';
  const bulkClearIntervalScopeLabel = overviewFiltersActive ? ' (filtered)' : '';
  const bulkSetIntervalScopeLabel = overviewFiltersActive ? ' (filtered)' : '';

  const retryFailedSyncs = useMutation({
    mutationFn: () => gitCredentialsApi.retryFailedSyncs(org!.id, buildOverviewFilters()),
    onSuccess: async (result) => {
      setError(null);
      const scope = hasActiveOverviewFilters() ? ' (filtered)' : '';
      setMessage(
        `Enqueued ${result.enqueued} of ${result.targeted} failed syncs${scope}`
          + (result.skippedPending > 0 ? ` (${result.skippedPending} skipped — sync already pending)` : ''),
      );
      await queryClient.invalidateQueries({ queryKey: ['org-git-sync-overview', org?.id] });
      await queryClient.invalidateQueries({ queryKey: ['org-git-sync-overview-filter-counts', org?.id] });
      await queryClient.invalidateQueries({ queryKey: ['org-git-sync-bulk-actions-summary', org?.id] });
    },
    onError: (err) => {
      setMessage(null);
      setError(err instanceof ApiError ? err.message : 'Failed to retry syncs');
    },
  });

  const enableScheduledSyncs = useMutation({
    mutationFn: () => gitCredentialsApi.enableScheduledSyncs(org!.id, buildOverviewFilters()),
    onSuccess: async (result) => {
      setError(null);
      const scope = hasActiveOverviewFilters() ? ' (filtered)' : '';
      setMessage(`Enabled scheduled sync on ${result.updated} of ${result.targeted} manual-only links${scope}`);
      await queryClient.invalidateQueries({ queryKey: ['org-git-sync-overview', org?.id] });
      await queryClient.invalidateQueries({ queryKey: ['org-git-sync-overview-filter-counts', org?.id] });
      await queryClient.invalidateQueries({ queryKey: ['org-git-sync-bulk-actions-summary', org?.id] });
    },
    onError: (err) => {
      setMessage(null);
      setError(err instanceof ApiError ? err.message : 'Failed to enable scheduled sync');
    },
  });

  const disableScheduledSyncs = useMutation({
    mutationFn: () => gitCredentialsApi.disableScheduledSyncs(org!.id, buildOverviewFilters()),
    onSuccess: async (result) => {
      setError(null);
      const scope = hasActiveOverviewFilters() ? ' (filtered)' : '';
      setMessage(`Disabled scheduled sync on ${result.updated} of ${result.targeted} links${scope}`);
      await queryClient.invalidateQueries({ queryKey: ['org-git-sync-overview', org?.id] });
      await queryClient.invalidateQueries({ queryKey: ['org-git-sync-overview-filter-counts', org?.id] });
      await queryClient.invalidateQueries({ queryKey: ['org-git-sync-bulk-actions-summary', org?.id] });
    },
    onError: (err) => {
      setMessage(null);
      setError(err instanceof ApiError ? err.message : 'Failed to disable scheduled sync');
    },
  });

  const clearCustomSyncIntervals = useMutation({
    mutationFn: () => gitCredentialsApi.clearCustomSyncIntervals(org!.id, buildOverviewFilters()),
    onSuccess: async (result) => {
      setError(null);
      const scope = hasActiveOverviewFilters() ? ' (filtered)' : '';
      setMessage(`Cleared custom interval on ${result.updated} of ${result.targeted} links${scope}`);
      await queryClient.invalidateQueries({ queryKey: ['org-git-sync-overview', org?.id] });
      await queryClient.invalidateQueries({ queryKey: ['org-git-sync-overview-filter-counts', org?.id] });
      await queryClient.invalidateQueries({ queryKey: ['org-git-sync-bulk-actions-summary', org?.id] });
    },
    onError: (err) => {
      setMessage(null);
      setError(err instanceof ApiError ? err.message : 'Failed to clear sync intervals');
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
      await queryClient.invalidateQueries({ queryKey: ['org-git-sync-overview-filter-counts', org.id] });
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
      await queryClient.invalidateQueries({ queryKey: ['org-git-sync-overview-filter-counts', org.id] });
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
      await queryClient.invalidateQueries({ queryKey: ['org-git-sync-overview-filter-counts', org.id] });
    } catch (err) {
      setMessage(null);
      setError(err instanceof ApiError ? err.message : 'Failed to disable scheduled sync');
    } finally {
      setTogglingScheduledProjectId(null);
    }
  }

  async function clearCustomSyncIntervalForProject(projectId: string, projectKey: string) {
    if (!org?.id) return;
    setClearingIntervalProjectId(projectId);
    setError(null);
    try {
      const result = await gitCredentialsApi.clearCustomSyncIntervalForProject(org.id, projectId);
      setMessage(
        result.updated
          ? `Cleared custom sync interval for ${projectKey}`
          : `${projectKey} already uses the platform default interval`,
      );
      await queryClient.invalidateQueries({ queryKey: ['org-git-sync-overview', org.id] });
      await queryClient.invalidateQueries({ queryKey: ['org-git-sync-overview-filter-counts', org.id] });
    } catch (err) {
      setMessage(null);
      setError(err instanceof ApiError ? err.message : 'Failed to clear sync interval');
    } finally {
      setClearingIntervalProjectId(null);
    }
  }

  function openBulkSetIntervalDialog() {
    setBulkSetIntervalDialogOpen(true);
    setIntervalDialogProject(null);
    setIntervalMinutesInput('');
    setError(null);
  }

  function openSetIntervalDialog(
    projectId: string,
    projectKey: string,
    currentMinutes: number | null
  ) {
    setBulkSetIntervalDialogOpen(false);
    setIntervalDialogProject({ projectId, projectKey, currentMinutes });
    setIntervalMinutesInput(currentMinutes != null ? String(currentMinutes) : '');
    setError(null);
  }

  function closeSetIntervalDialog() {
    if (settingIntervalProjectId || settingBulkInterval) return;
    setIntervalDialogProject(null);
    setBulkSetIntervalDialogOpen(false);
    setIntervalMinutesInput('');
  }

  async function submitSetInterval() {
    if (!org?.id) return;
    const trimmed = intervalMinutesInput.trim();
    const minutes = Number(trimmed);
    if (!trimmed || !Number.isInteger(minutes) || minutes < 15 || minutes > 10080) {
      setError('Interval must be a whole number between 15 and 10080 minutes');
      return;
    }

    if (bulkSetIntervalDialogOpen) {
      setSettingBulkInterval(true);
      setError(null);
      try {
        const result = await gitCredentialsApi.setCustomSyncIntervals(
          org.id,
          minutes,
          buildOverviewFilters()
        );
        const scope = hasActiveOverviewFilters() ? ' (filtered)' : '';
        setMessage(`Set sync interval to ${minutes}m on ${result.updated} of ${result.targeted} links${scope}`);
        setBulkSetIntervalDialogOpen(false);
        setIntervalMinutesInput('');
        await queryClient.invalidateQueries({ queryKey: ['org-git-sync-overview', org.id] });
      await queryClient.invalidateQueries({ queryKey: ['org-git-sync-overview-filter-counts', org.id] });
        await queryClient.invalidateQueries({ queryKey: ['org-git-sync-bulk-actions-summary', org.id] });
      } catch (err) {
        setMessage(null);
        setError(err instanceof ApiError ? err.message : 'Failed to set sync intervals');
      } finally {
        setSettingBulkInterval(false);
      }
      return;
    }

    if (!intervalDialogProject) return;
    setSettingIntervalProjectId(intervalDialogProject.projectId);
    setError(null);
    try {
      const result = await gitCredentialsApi.setCustomSyncIntervalForProject(
        org.id,
        intervalDialogProject.projectId,
        minutes
      );
      setMessage(
        result.updated
          ? `Set sync interval to ${minutes}m for ${intervalDialogProject.projectKey}`
          : `${intervalDialogProject.projectKey} already has interval ${minutes}m`,
      );
      setIntervalDialogProject(null);
      setIntervalMinutesInput('');
      await queryClient.invalidateQueries({ queryKey: ['org-git-sync-overview', org.id] });
      await queryClient.invalidateQueries({ queryKey: ['org-git-sync-overview-filter-counts', org.id] });
    } catch (err) {
      setMessage(null);
      setError(err instanceof ApiError ? err.message : 'Failed to set sync interval');
    } finally {
      setSettingIntervalProjectId(null);
    }
  }

  async function viewFailedRunsForProject(projectId: string) {
    await applyRunFilterState('all', 'failed', projectId, true);
  }

  async function filterOverviewToFailed() {
    const preset = OVERVIEW_FILTER_PRESETS.find((p) => p.id === 'failed-enabled');
    if (preset) {
      await applyOverviewPreset(preset);
      return;
    }
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
    await applyOverviewFilterState(
      'linked',
      'all',
      'all',
      'all',
      overviewProviderFilter,
      overviewStatusFilter
    );
  }

  async function filterOverviewToEnabled() {
    await applyOverviewFilterState(
      'linked',
      'enabled',
      'all',
      'all',
      overviewProviderFilter,
      overviewStatusFilter
    );
  }

  async function filterOverviewToScheduledSync() {
    const preset = OVERVIEW_FILTER_PRESETS.find((p) => p.id === 'scheduled-enabled');
    if (preset) await applyOverviewPreset(preset);
  }

  async function filterOverviewToManualSync() {
    const preset = OVERVIEW_FILTER_PRESETS.find((p) => p.id === 'manual-enabled');
    if (preset) await applyOverviewPreset(preset);
  }

  async function filterOverviewToCustomInterval() {
    const preset = OVERVIEW_FILTER_PRESETS.find((p) => p.id === 'custom-interval');
    if (preset) await applyOverviewPreset(preset);
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

      {hasAnyPageFilters() && (
        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          spacing={1}
          sx={{ alignItems: 'flex-start' }}
          data-testid="git-sync-page-filter-toolbar"
        >
          <Typography variant="body2" color="text.secondary">
            {pageActiveFilterCount()} active filter{pageActiveFilterCount() === 1 ? '' : 's'} across overview
            and runs
          </Typography>
          <Button
            size="small"
            variant="outlined"
            onClick={() => void copyFullPageFilterLink()}
            data-testid="git-sync-page-copy-filter-link"
          >
            Copy page link
          </Button>
          <Button
            size="small"
            variant="text"
            onClick={() => void clearAllPageFilters()}
            data-testid="git-sync-page-clear-all-filters"
          >
            Clear all filters
          </Button>
        </Stack>
      )}

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
          <Stack
            direction="row"
            spacing={0.5}
            sx={{ flexWrap: 'wrap', gap: 0.5 }}
            data-testid="git-sync-overview-filter-presets"
          >
            {OVERVIEW_FILTER_PRESETS.map((preset) => (
              <Chip
                key={preset.id}
                label={formatOverviewPresetLabel(preset)}
                size="small"
                clickable
                color={isOverviewPresetActive(preset) ? 'primary' : 'default'}
                variant={isOverviewPresetActive(preset) ? 'filled' : 'outlined'}
                onClick={() => void applyOverviewPreset(preset)}
                data-testid={`git-sync-overview-preset-${preset.id}`}
              />
            ))}
          </Stack>
          {(savedOverviewPresets.length > 0 || overviewFiltersActive) && (
            <Stack
              direction="row"
              spacing={0.5}
              sx={{ flexWrap: 'wrap', gap: 0.5, alignItems: 'center' }}
              data-testid="git-sync-overview-saved-presets"
            >
              {savedOverviewPresets.map((preset) => (
                <Stack
                  key={preset.id}
                  direction="row"
                  spacing={0}
                  sx={{ alignItems: 'center' }}
                >
                  <Chip
                    label={formatSavedOverviewPresetLabel(preset)}
                    size="small"
                    clickable
                    color={savedPresetChipColor(preset, isSavedOverviewPresetActive(preset))}
                    variant={isSavedOverviewPresetActive(preset) ? 'filled' : 'outlined'}
                    onClick={() => void applySavedOverviewPreset(preset)}
                    onDelete={
                      canDeleteSavedPreset(preset.id)
                        ? () => void deleteSavedOverviewPreset(preset.id)
                        : undefined
                    }
                    data-testid={`git-sync-overview-saved-preset-${preset.id}`}
                  />
                  {canDeleteSavedPreset(preset.id) && (
                    <IconButton
                      size="small"
                      aria-label={`Rename ${preset.label}`}
                      onClick={() => openRenamePresetDialog(preset, 'overview')}
                      data-testid={`git-sync-overview-rename-preset-${preset.id}`}
                    >
                      <EditOutlinedIcon fontSize="inherit" />
                    </IconButton>
                  )}
                </Stack>
              ))}
              {overviewFiltersActive && (
                <Button
                  size="small"
                  variant="text"
                  onClick={openSaveOverviewPresetDialog}
                  data-testid="git-sync-overview-save-preset"
                >
                  Save filters
                </Button>
              )}
            </Stack>
          )}
          {overviewFiltersActive && (
            <Stack
              direction="row"
              spacing={0.5}
              sx={{ flexWrap: 'wrap', gap: 0.5 }}
              data-testid="git-sync-overview-active-filters"
            >
              {getActiveOverviewFilterChips().map((chip) => (
                <Chip
                  key={chip.id}
                  label={chip.label}
                  size="small"
                  onDelete={() => void chip.clear()}
                  data-testid={`git-sync-overview-active-filter-${chip.id}`}
                />
              ))}
            </Stack>
          )}
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} sx={{ alignItems: 'flex-start' }}>
            <TextField
              select
              label="Linked"
              size="small"
              value={overviewLinkedFilter}
              onChange={(e) => {
                const value = e.target.value as 'all' | 'linked' | 'unlinked';
                void applyOverviewFilterState(
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
                void applyOverviewFilterState(
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
                void applyOverviewFilterState(
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
                void applyOverviewFilterState(
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
                void applyOverviewFilterState(
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
                void applyOverviewFilterState(
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
            {overviewFiltersActive && (
              <Button
                size="small"
                variant="text"
                onClick={() => void clearOverviewFilters()}
                data-testid="git-sync-overview-clear-filters"
              >
                Clear filters
              </Button>
            )}
            {overviewFiltersActive && (
              <Button
                size="small"
                variant="outlined"
                onClick={() => void copyOverviewFilterLink()}
                data-testid="git-sync-overview-copy-filter-link"
              >
                Copy filtered link
              </Button>
            )}
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
          </Stack>
          {canRetryFailedSyncs && bulkActionsSummaryQuery.data && (
            <Stack spacing={0.5} data-testid="git-sync-bulk-actions-summary">
              <Typography variant="body2" color="text.secondary">
                Bulk actions preview
                {overviewFiltersActive ? ' (filtered scope)' : ''}
                : {bulkActionsSummaryQuery.data.filteredItems} projects in view
                {bulkActionsSummaryQuery.data.retryFailedTargeted > 0
                  ? ` · retry failed ${bulkActionsSummaryQuery.data.retryFailedTargeted}`
                    + (bulkActionsSummaryQuery.data.retryFailedPendingSkipped > 0
                      ? ` (${bulkActionsSummaryQuery.data.retryFailedPendingSkipped} pending)`
                      : '')
                  : ''}
                {bulkActionsSummaryQuery.data.enableScheduledTargeted > 0
                  ? ` · enable scheduled ${bulkActionsSummaryQuery.data.enableScheduledTargeted}`
                  : ''}
                {bulkActionsSummaryQuery.data.disableScheduledTargeted > 0
                  ? ` · disable scheduled ${bulkActionsSummaryQuery.data.disableScheduledTargeted}`
                  : ''}
                {bulkActionsSummaryQuery.data.clearIntervalTargeted > 0
                  ? ` · clear interval ${bulkActionsSummaryQuery.data.clearIntervalTargeted}`
                  : ''}
                {bulkActionsSummaryQuery.data.setIntervalTargeted > 0
                  ? ` · set interval ${bulkActionsSummaryQuery.data.setIntervalTargeted}`
                  : ''}
              </Typography>
              <Stack direction="row" spacing={1}>
                <Button
                  size="small"
                  variant="outlined"
                  disabled={bulkSummaryExporting !== null}
                  onClick={() => void exportBulkActionsSummary('csv')}
                  data-testid="git-sync-bulk-actions-summary-export-csv"
                >
                  {bulkSummaryExporting === 'csv' ? 'Exporting…' : 'Export summary CSV'}
                </Button>
                <Button
                  size="small"
                  variant="outlined"
                  disabled={bulkSummaryExporting !== null}
                  onClick={() => void exportBulkActionsSummary('json')}
                  data-testid="git-sync-bulk-actions-summary-export-json"
                >
                  {bulkSummaryExporting === 'json' ? 'Exporting…' : 'Export summary JSON'}
                </Button>
              </Stack>
            </Stack>
          )}
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} sx={{ alignItems: 'flex-start' }}>
            {canRetryFailedSyncs && filteredFailedLastSync > 0 && (
              <Button
                size="small"
                variant="contained"
                color="warning"
                disabled={retryFailedSyncs.isPending}
                onClick={() => retryFailedSyncs.mutate()}
                data-testid="git-sync-overview-retry-failed"
              >
                Retry failed syncs{bulkRetryScopeLabel}
              </Button>
            )}
            {canRetryFailedSyncs && filteredManualSyncLinks > 0 && (
              <Button
                size="small"
                variant="contained"
                color="primary"
                disabled={enableScheduledSyncs.isPending || disableScheduledSyncs.isPending}
                onClick={() => enableScheduledSyncs.mutate()}
                data-testid="git-sync-overview-enable-scheduled"
              >
                Enable scheduled sync{bulkScheduledScopeLabel}
              </Button>
            )}
            {canRetryFailedSyncs && filteredScheduledSyncLinks > 0 && (
              <Button
                size="small"
                variant="outlined"
                color="primary"
                disabled={enableScheduledSyncs.isPending || disableScheduledSyncs.isPending}
                onClick={() => disableScheduledSyncs.mutate()}
                data-testid="git-sync-overview-disable-scheduled"
              >
                Disable scheduled sync{bulkScheduledScopeLabel}
              </Button>
            )}
            {canRetryFailedSyncs && filteredEnabledLinkedLinks > 0 && (
              <Button
                size="small"
                variant="outlined"
                color="secondary"
                disabled={
                  settingBulkInterval
                  || settingIntervalProjectId != null
                  || clearCustomSyncIntervals.isPending
                }
                onClick={() => openBulkSetIntervalDialog()}
                data-testid="git-sync-overview-set-interval-bulk"
              >
                Set interval{bulkSetIntervalScopeLabel}
              </Button>
            )}
            {canRetryFailedSyncs && filteredCustomSyncIntervalLinks > 0 && (
              <Button
                size="small"
                variant="outlined"
                color="secondary"
                disabled={
                  clearCustomSyncIntervals.isPending
                  || clearingIntervalProjectId != null
                  || settingIntervalProjectId != null
                }
                onClick={() => clearCustomSyncIntervals.mutate()}
                data-testid="git-sync-overview-clear-interval-bulk"
              >
                Clear interval{bulkClearIntervalScopeLabel}
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
                  {item.scheduledSyncIntervalMinutes != null
                    ? ` · interval ${item.scheduledSyncIntervalMinutes}m`
                    : ''}
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
              {canRetryFailedSyncs && item.linked && item.enabled && (
                <Button
                  size="small"
                  variant="outlined"
                  color="secondary"
                  disabled={
                    settingIntervalProjectId === item.projectId
                    || clearingIntervalProjectId === item.projectId
                  }
                  onClick={() =>
                    openSetIntervalDialog(
                      item.projectId,
                      item.projectKey,
                      item.scheduledSyncIntervalMinutes
                    )
                  }
                  data-testid={`git-sync-overview-set-interval-${item.projectKey}`}
                >
                  Set interval
                </Button>
              )}
              {canRetryFailedSyncs
                && item.linked
                && item.enabled
                && item.scheduledSyncIntervalMinutes != null && (
                <Button
                  size="small"
                  variant="outlined"
                  color="secondary"
                  disabled={
                    clearingIntervalProjectId === item.projectId
                    || togglingScheduledProjectId === item.projectId
                    || clearCustomSyncIntervals.isPending
                  }
                  onClick={() => void clearCustomSyncIntervalForProject(item.projectId, item.projectKey)}
                  data-testid={`git-sync-overview-clear-interval-${item.projectKey}`}
                >
                  {clearingIntervalProjectId === item.projectId ? 'Updating…' : 'Clear interval'}
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
        <Stack
          direction="row"
          spacing={0.5}
          sx={{ flexWrap: 'wrap', gap: 0.5 }}
          data-testid="org-git-sync-runs-filter-presets"
        >
          {RUN_FILTER_PRESETS.map((preset) => (
            <Chip
              key={preset.id}
              label={formatRunPresetLabel(preset)}
              size="small"
              clickable
              color={isRunPresetActive(preset) ? 'primary' : 'default'}
              variant={isRunPresetActive(preset) ? 'filled' : 'outlined'}
              onClick={() => void applyRunPreset(preset)}
              data-testid={`org-git-sync-runs-preset-${preset.id}`}
            />
          ))}
        </Stack>
        {(savedRunPresets.length > 0 || hasActiveRunFilters()) && (
          <Stack
            direction="row"
            spacing={0.5}
            sx={{ flexWrap: 'wrap', gap: 0.5, alignItems: 'center' }}
            data-testid="org-git-sync-runs-saved-presets"
          >
            {savedRunPresets.map((preset) => (
              <Stack
                key={preset.id}
                direction="row"
                spacing={0}
                sx={{ alignItems: 'center' }}
              >
                <Chip
                  label={formatSavedRunPresetLabel(preset)}
                  size="small"
                  clickable
                  color={savedPresetChipColor(preset, isSavedRunPresetActive(preset))}
                  variant={isSavedRunPresetActive(preset) ? 'filled' : 'outlined'}
                  onClick={() => void applySavedRunPreset(preset)}
                  onDelete={
                    canDeleteSavedPreset(preset.id)
                      ? () => void deleteSavedRunPreset(preset.id)
                      : undefined
                  }
                  data-testid={`org-git-sync-runs-saved-preset-${preset.id}`}
                />
                {canDeleteSavedPreset(preset.id) && (
                  <IconButton
                    size="small"
                    aria-label={`Rename ${preset.label}`}
                    onClick={() => openRenamePresetDialog(preset, 'run')}
                    data-testid={`org-git-sync-runs-rename-preset-${preset.id}`}
                  >
                    <EditOutlinedIcon fontSize="inherit" />
                  </IconButton>
                )}
              </Stack>
            ))}
            {hasActiveRunFilters() && (
              <Button
                size="small"
                variant="text"
                onClick={openSaveRunPresetDialog}
                data-testid="org-git-sync-runs-save-preset"
              >
                Save filters
              </Button>
            )}
          </Stack>
        )}
        {hasActiveRunFilters() && (
          <Stack
            direction="row"
            spacing={0.5}
            sx={{ flexWrap: 'wrap', gap: 0.5 }}
            data-testid="org-git-sync-runs-active-filters"
          >
            {getActiveRunFilterChips().map((chip) => (
              <Chip
                key={chip.id}
                label={chip.label}
                size="small"
                onDelete={() => void chip.clear()}
                data-testid={`org-git-sync-runs-active-filter-${chip.id}`}
              />
            ))}
          </Stack>
        )}
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} sx={{ alignItems: 'flex-start' }}>
          <TextField
            select
            label="Project"
            size="small"
            value={runProjectFilter}
            onChange={(e) => {
              const value = e.target.value;
              void applyRunFilterState(runSourceFilter, runStatusFilter, value);
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
              const value = e.target.value as RunSourceFilter;
              void applyRunFilterState(value, runStatusFilter, runProjectFilter);
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
              const value = e.target.value as RunStatusFilter;
              void applyRunFilterState(runSourceFilter, value, runProjectFilter);
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
          {hasActiveRunFilters() && (
            <Button
              size="small"
              variant="text"
              onClick={() => void clearRunFilters()}
              data-testid="org-git-sync-runs-clear-filters"
            >
              Clear filters
            </Button>
          )}
          {hasActiveRunFilters() && (
            <Button
              size="small"
              variant="outlined"
              onClick={() => void copyRunFilterLink()}
              data-testid="org-git-sync-runs-copy-filter-link"
            >
              Copy filtered link
            </Button>
          )}
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

      <Dialog
        open={intervalDialogProject != null || bulkSetIntervalDialogOpen}
        onClose={closeSetIntervalDialog}
        data-testid="git-sync-overview-set-interval-dialog"
      >
        <DialogTitle>
          Set sync interval
          {bulkSetIntervalDialogOpen
            ? bulkSetIntervalScopeLabel
            : intervalDialogProject
              ? ` — ${intervalDialogProject.projectKey}`
              : ''}
        </DialogTitle>
        <DialogContent>
          <TextField
            label="Scheduled sync interval (minutes)"
            size="small"
            fullWidth
            margin="dense"
            value={intervalMinutesInput}
            onChange={(e) => setIntervalMinutesInput(e.target.value)}
            helperText="15–10080 minutes. Platform default when cleared."
            slotProps={{ htmlInput: { 'data-testid': 'git-sync-overview-set-interval-input' } }}
          />
        </DialogContent>
        <DialogActions>
          <Button
            onClick={closeSetIntervalDialog}
            disabled={settingIntervalProjectId != null || settingBulkInterval}
          >
            Cancel
          </Button>
          <Button
            onClick={() => void submitSetInterval()}
            disabled={settingIntervalProjectId != null || settingBulkInterval}
            data-testid="git-sync-overview-set-interval-save"
          >
            {settingIntervalProjectId != null || settingBulkInterval ? 'Saving…' : 'Save'}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog
        open={savePresetDialog != null}
        onClose={closeSavePresetDialog}
        data-testid="git-sync-save-preset-dialog"
      >
        <DialogTitle>
          Save {savePresetDialog === 'overview' ? 'overview' : 'run'} filter preset
        </DialogTitle>
        <DialogContent>
          <TextField
            label="Preset name"
            size="small"
            fullWidth
            margin="dense"
            value={savePresetNameInput}
            onChange={(e) => setSavePresetNameInput(e.target.value)}
            helperText={
              savePresetShareWithOrg
                ? 'Shared with all org members (max 12 org presets per section).'
                : 'Private to your account for this organization (max 12 per section).'
            }
            slotProps={{ htmlInput: { 'data-testid': 'git-sync-save-preset-name-input' } }}
          />
          {isOwnerOrAdmin && (
            <FormControlLabel
              control={
                <Checkbox
                  checked={savePresetShareWithOrg}
                  onChange={(e) => setSavePresetShareWithOrg(e.target.checked)}
                  data-testid="git-sync-save-preset-share-org"
                />
              }
              label="Share with organization"
            />
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={closeSavePresetDialog} disabled={savingFilterPreset}>Cancel</Button>
          <Button
            onClick={() => void submitSavePreset()}
            disabled={
              savingFilterPreset || normalizeSavePresetName(savePresetNameInput).length === 0
            }
            data-testid="git-sync-save-preset-confirm"
          >
            {savingFilterPreset ? 'Saving…' : 'Save'}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog
        open={renamePreset != null}
        onClose={closeRenamePresetDialog}
        data-testid="git-sync-rename-preset-dialog"
      >
        <DialogTitle>
          Rename {renamePreset?.scope === 'overview' ? 'overview' : 'run'} filter preset
        </DialogTitle>
        <DialogContent>
          <TextField
            label="Preset name"
            size="small"
            fullWidth
            margin="dense"
            value={renamePresetNameInput}
            onChange={(e) => setRenamePresetNameInput(e.target.value)}
            helperText="Filters stay the same; only the display name changes."
            slotProps={{ htmlInput: { 'data-testid': 'git-sync-rename-preset-name-input' } }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={closeRenamePresetDialog} disabled={renamingFilterPreset}>
            Cancel
          </Button>
          <Button
            onClick={() => void submitRenamePreset()}
            disabled={
              renamingFilterPreset
              || normalizeSavePresetName(renamePresetNameInput).length === 0
              || normalizeSavePresetName(renamePresetNameInput) === renamePreset?.label
            }
            data-testid="git-sync-rename-preset-confirm"
          >
            {renamingFilterPreset ? 'Renaming…' : 'Rename'}
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}
