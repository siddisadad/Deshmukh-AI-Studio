import { describe, expect, it } from 'vitest';
import {
  buildGitSyncOverviewFilterUrl,
  buildGitSyncPageFilterUrl,
  buildGitSyncRunFilterUrl,
  gitSyncRunsSectionHash,
  ORG_SYNC_RUNS_SECTION_ID,
  readOverviewFiltersFromSearchParams,
  readRunFiltersFromSearchParams,
  writeOverviewFiltersToSearchParams,
  writeRunFiltersToSearchParams,
} from './gitSyncFilterUrl';

describe('gitSyncFilterUrl', () => {
  it('reads and writes overview filters from search params', () => {
    const params = new URLSearchParams();
    writeOverviewFiltersToSearchParams(params, {
      linked: 'linked',
      enabled: 'enabled',
      scheduled: 'scheduled',
      interval: 'custom',
      provider: 'github',
      status: 'failed',
    });
    expect(params.get('linked')).toBe('linked');
    expect(params.get('lastSync')).toBe('failed');
    const parsed = readOverviewFiltersFromSearchParams(params);
    expect(parsed).toEqual({
      linked: 'linked',
      enabled: 'enabled',
      scheduled: 'scheduled',
      interval: 'custom',
      provider: 'github',
      status: 'failed',
    });
  });

  it('reads and writes run filters from search params', () => {
    const params = new URLSearchParams();
    writeRunFiltersToSearchParams(params, {
      source: 'webhook',
      status: 'success',
      project: 'proj-1',
    });
    expect(params.get('runSource')).toBe('webhook');
    expect(params.get('runProject')).toBe('proj-1');
    const parsed = readRunFiltersFromSearchParams(params);
    expect(parsed).toEqual({
      source: 'webhook',
      status: 'success',
      project: 'proj-1',
    });
  });

  it('adds runs section hash when run filters are active', () => {
    const run = { source: 'manual' as const, status: 'all' as const, project: 'all' as const };
    expect(gitSyncRunsSectionHash(run)).toBe(`#${ORG_SYNC_RUNS_SECTION_ID}`);
    expect(gitSyncRunsSectionHash({ source: 'all', status: 'all', project: 'all' })).toBe('');
  });

  it('builds full page URLs with hash for active run filters', () => {
    const overview = {
      linked: 'all' as const,
      enabled: 'all' as const,
      scheduled: 'all' as const,
      interval: 'all' as const,
      provider: 'all' as const,
      status: 'all' as const,
    };
    const run = { source: 'scheduled' as const, status: 'failed' as const, project: 'all' as const };
    const url = buildGitSyncPageFilterUrl('/settings/git', overview, run, new URLSearchParams());
    expect(url).toBe(
      `http://localhost/settings/git?runSource=scheduled&runStatus=failed#${ORG_SYNC_RUNS_SECTION_ID}`,
    );
  });

  it('builds overview-only URLs without hash', () => {
    const overview = {
      linked: 'linked' as const,
      enabled: 'all' as const,
      scheduled: 'all' as const,
      interval: 'all' as const,
      provider: 'all' as const,
      status: 'all' as const,
    };
    const url = buildGitSyncOverviewFilterUrl('/settings/git', overview, new URLSearchParams());
    expect(url).toBe('http://localhost/settings/git?linked=linked');
    expect(url).not.toContain('#');
  });

  it('builds run filter URLs with hash', () => {
    const overview = {
      linked: 'all' as const,
      enabled: 'all' as const,
      scheduled: 'all' as const,
      interval: 'all' as const,
      provider: 'all' as const,
      status: 'all' as const,
    };
    const run = { source: 'webhook' as const, status: 'all' as const, project: 'all' as const };
    const url = buildGitSyncRunFilterUrl('/settings/git', overview, run, new URLSearchParams());
    expect(url).toBe(`http://localhost/settings/git?runSource=webhook#${ORG_SYNC_RUNS_SECTION_ID}`);
  });
});
