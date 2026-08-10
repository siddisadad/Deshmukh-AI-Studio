export const ORG_SYNC_RUNS_SECTION_ID = 'org-git-sync-runs';

export type OverviewLinkedFilter = 'all' | 'linked' | 'unlinked';
export type OverviewEnabledFilter = 'all' | 'enabled' | 'disabled';
export type OverviewScheduledFilter = 'all' | 'scheduled' | 'manual';
export type OverviewIntervalFilter = 'all' | 'custom' | 'default';
export type OverviewProviderFilter = 'all' | 'github' | 'gitlab' | 'bitbucket';
export type OverviewStatusFilter = 'all' | 'success' | 'failed' | 'never';

export type OverviewFilterState = {
  linked: OverviewLinkedFilter;
  enabled: OverviewEnabledFilter;
  scheduled: OverviewScheduledFilter;
  interval: OverviewIntervalFilter;
  provider: OverviewProviderFilter;
  status: OverviewStatusFilter;
};

export type RunSourceFilter = 'all' | 'manual' | 'scheduled' | 'webhook';
export type RunStatusFilter = 'all' | 'success' | 'failed';
export type RunProjectFilter = 'all' | string;

export type RunFilterState = {
  source: RunSourceFilter;
  status: RunStatusFilter;
  project: RunProjectFilter;
};

const OVERVIEW_FILTER_URL_KEYS = [
  'linked',
  'enabled',
  'scheduled',
  'interval',
  'provider',
  'lastSync',
] as const;

const RUN_FILTER_URL_KEYS = ['runSource', 'runStatus', 'runProject'] as const;

function parseOverviewLinkedParam(value: string | null): OverviewLinkedFilter {
  if (value === 'linked' || value === 'unlinked') return value;
  return 'all';
}

function parseOverviewEnabledParam(value: string | null): OverviewEnabledFilter {
  if (value === 'enabled' || value === 'disabled') return value;
  return 'all';
}

function parseOverviewScheduledParam(value: string | null): OverviewScheduledFilter {
  if (value === 'scheduled' || value === 'manual') return value;
  return 'all';
}

function parseOverviewIntervalParam(value: string | null): OverviewIntervalFilter {
  if (value === 'custom' || value === 'default') return value;
  return 'all';
}

function parseOverviewProviderParam(value: string | null): OverviewProviderFilter {
  if (value === 'github' || value === 'gitlab' || value === 'bitbucket') return value;
  return 'all';
}

function parseOverviewStatusParam(value: string | null): OverviewStatusFilter {
  if (value === 'success' || value === 'failed' || value === 'never') return value;
  return 'all';
}

function hasOverviewFilterUrlParams(params: URLSearchParams) {
  return OVERVIEW_FILTER_URL_KEYS.some((key) => params.get(key) != null);
}

export function readOverviewFiltersFromSearchParams(params: URLSearchParams): OverviewFilterState | null {
  if (!hasOverviewFilterUrlParams(params)) return null;
  return {
    linked: parseOverviewLinkedParam(params.get('linked')),
    enabled: parseOverviewEnabledParam(params.get('enabled')),
    scheduled: parseOverviewScheduledParam(params.get('scheduled')),
    interval: parseOverviewIntervalParam(params.get('interval')),
    provider: parseOverviewProviderParam(params.get('provider')),
    status: parseOverviewStatusParam(params.get('lastSync')),
  };
}

export function writeOverviewFiltersToSearchParams(params: URLSearchParams, filters: OverviewFilterState) {
  for (const key of OVERVIEW_FILTER_URL_KEYS) {
    params.delete(key);
  }
  if (filters.linked !== 'all') params.set('linked', filters.linked);
  if (filters.enabled !== 'all') params.set('enabled', filters.enabled);
  if (filters.scheduled !== 'all') params.set('scheduled', filters.scheduled);
  if (filters.interval !== 'all') params.set('interval', filters.interval);
  if (filters.provider !== 'all') params.set('provider', filters.provider);
  if (filters.status !== 'all') params.set('lastSync', filters.status);
}

function parseRunSourceParam(value: string | null): RunSourceFilter {
  if (value === 'manual' || value === 'scheduled' || value === 'webhook') return value;
  return 'all';
}

function parseRunStatusParam(value: string | null): RunStatusFilter {
  if (value === 'success' || value === 'failed') return value;
  return 'all';
}

function parseRunProjectParam(value: string | null): RunProjectFilter {
  if (value && value.trim().length > 0) return value.trim();
  return 'all';
}

function hasRunFilterUrlParams(params: URLSearchParams) {
  return RUN_FILTER_URL_KEYS.some((key) => params.get(key) != null);
}

export function readRunFiltersFromSearchParams(params: URLSearchParams): RunFilterState | null {
  if (!hasRunFilterUrlParams(params)) return null;
  return {
    source: parseRunSourceParam(params.get('runSource')),
    status: parseRunStatusParam(params.get('runStatus')),
    project: parseRunProjectParam(params.get('runProject')),
  };
}

export function writeRunFiltersToSearchParams(params: URLSearchParams, filters: RunFilterState) {
  for (const key of RUN_FILTER_URL_KEYS) {
    params.delete(key);
  }
  if (filters.source !== 'all') params.set('runSource', filters.source);
  if (filters.status !== 'all') params.set('runStatus', filters.status);
  if (filters.project !== 'all') params.set('runProject', filters.project);
}

export function countActiveOverviewFilterDimensions(filters: OverviewFilterState): number {
  let count = 0;
  if (filters.linked !== 'all') count += 1;
  if (filters.enabled !== 'all') count += 1;
  if (filters.scheduled !== 'all') count += 1;
  if (filters.interval !== 'all') count += 1;
  if (filters.provider !== 'all') count += 1;
  if (filters.status !== 'all') count += 1;
  return count;
}

export function countActiveRunFilterDimensions(filters: RunFilterState): number {
  let count = 0;
  if (filters.source !== 'all') count += 1;
  if (filters.status !== 'all') count += 1;
  if (filters.project !== 'all') count += 1;
  return count;
}

export function gitSyncRunsSectionHash(run: RunFilterState): string {
  return countActiveRunFilterDimensions(run) > 0 ? `#${ORG_SYNC_RUNS_SECTION_ID}` : '';
}

export function buildGitSyncPageFilterUrl(
  pathname: string,
  overview: OverviewFilterState,
  run: RunFilterState,
  existingParams: URLSearchParams,
  origin = 'http://localhost'
): string {
  const params = new URLSearchParams(existingParams);
  writeOverviewFiltersToSearchParams(params, overview);
  writeRunFiltersToSearchParams(params, run);
  const query = params.toString();
  let url = `${origin}${pathname}${query ? `?${query}` : ''}`;
  const hash = gitSyncRunsSectionHash(run);
  if (hash) url += hash;
  return url;
}

export function buildGitSyncOverviewFilterUrl(
  pathname: string,
  overview: OverviewFilterState,
  existingParams: URLSearchParams,
  origin = 'http://localhost'
): string {
  const params = new URLSearchParams(existingParams);
  writeOverviewFiltersToSearchParams(params, overview);
  const query = params.toString();
  return `${origin}${pathname}${query ? `?${query}` : ''}`;
}

export function buildGitSyncRunFilterUrl(
  pathname: string,
  overview: OverviewFilterState,
  run: RunFilterState,
  existingParams: URLSearchParams,
  origin = 'http://localhost'
): string {
  const params = new URLSearchParams(existingParams);
  writeOverviewFiltersToSearchParams(params, overview);
  writeRunFiltersToSearchParams(params, run);
  const query = params.toString();
  let url = `${origin}${pathname}${query ? `?${query}` : ''}`;
  const hash = gitSyncRunsSectionHash(run);
  if (hash) url += hash;
  return url;
}
