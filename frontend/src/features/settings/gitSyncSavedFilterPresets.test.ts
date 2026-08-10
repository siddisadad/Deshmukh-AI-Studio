import { beforeEach, describe, expect, it } from 'vitest';
import {
  addSavedOverviewPreset,
  addSavedRunPreset,
  loadSavedOverviewPresets,
  loadSavedRunPresets,
  removeSavedOverviewPreset,
  removeSavedRunPreset,
} from './gitSyncSavedFilterPresets';

const ORG_ID = 'org-test-123';

const overviewFilters = {
  linked: 'linked' as const,
  enabled: 'enabled' as const,
  scheduled: 'manual' as const,
  interval: 'all' as const,
  provider: 'github' as const,
  status: 'failed' as const,
};

const runFilters = {
  source: 'manual' as const,
  status: 'failed' as const,
  project: 'all' as const,
};

describe('gitSyncSavedFilterPresets', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('saves and loads overview presets per org', () => {
    const result = addSavedOverviewPreset(ORG_ID, 'My failed GitHub', overviewFilters);
    expect(result.error).toBeUndefined();
    expect(result.presets).toHaveLength(1);
    expect(loadSavedOverviewPresets(ORG_ID)[0].label).toBe('My failed GitHub');
  });

  it('rejects duplicate overview filters', () => {
    addSavedOverviewPreset(ORG_ID, 'First', overviewFilters);
    const duplicate = addSavedOverviewPreset(ORG_ID, 'Second', overviewFilters);
    expect(duplicate.error).toContain('already saved');
    expect(duplicate.presets).toHaveLength(1);
  });

  it('removes saved overview presets', () => {
    const { presets } = addSavedOverviewPreset(ORG_ID, 'Temp', overviewFilters);
    const remaining = removeSavedOverviewPreset(ORG_ID, presets[0].id);
    expect(remaining).toHaveLength(0);
  });

  it('saves and removes run presets', () => {
    const { presets } = addSavedRunPreset(ORG_ID, 'Failed manual', runFilters);
    expect(presets[0].filters.source).toBe('manual');
    const remaining = removeSavedRunPreset(ORG_ID, presets[0].id);
    expect(remaining).toHaveLength(0);
    expect(loadSavedRunPresets(ORG_ID)).toHaveLength(0);
  });
});
