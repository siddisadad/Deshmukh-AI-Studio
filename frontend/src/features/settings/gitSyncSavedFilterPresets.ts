export type OverviewFiltersSnapshot = {
  linked: 'all' | 'linked' | 'unlinked';
  enabled: 'all' | 'enabled' | 'disabled';
  scheduled: 'all' | 'scheduled' | 'manual';
  interval: 'all' | 'custom' | 'default';
  provider: 'all' | 'github' | 'gitlab' | 'bitbucket';
  status: 'all' | 'success' | 'failed' | 'never';
};

export type RunFiltersSnapshot = {
  source: 'all' | 'manual' | 'scheduled' | 'webhook';
  status: 'all' | 'success' | 'failed';
  project: 'all' | string;
};

export type SavedOverviewFilterPreset = {
  id: string;
  label: string;
  filters: OverviewFiltersSnapshot;
};

export type SavedRunFilterPreset = {
  id: string;
  label: string;
  filters: RunFiltersSnapshot;
};

const OVERVIEW_KEY_PREFIX = 'aistudio.org-git-sync.saved-overview-presets.';
const RUN_KEY_PREFIX = 'aistudio.org-git-sync.saved-run-presets.';
const MAX_SAVED_PRESETS = 12;

function overviewKey(orgId: string) {
  return `${OVERVIEW_KEY_PREFIX}${orgId}`;
}

function runKey(orgId: string) {
  return `${RUN_KEY_PREFIX}${orgId}`;
}

function readJson<T>(key: string): T | null {
  try {
    const raw = localStorage.getItem(key);
    if (!raw) return null;
    return JSON.parse(raw) as T;
  } catch {
    return null;
  }
}

function writeJson(key: string, value: unknown) {
  try {
    localStorage.setItem(key, JSON.stringify(value));
    return true;
  } catch {
    return false;
  }
}

function normalizeLabel(label: string): string {
  return label.trim().slice(0, 40);
}

function filtersEqual(a: OverviewFiltersSnapshot, b: OverviewFiltersSnapshot): boolean {
  return (
    a.linked === b.linked
    && a.enabled === b.enabled
    && a.scheduled === b.scheduled
    && a.interval === b.interval
    && a.provider === b.provider
    && a.status === b.status
  );
}

function runFiltersEqual(a: RunFiltersSnapshot, b: RunFiltersSnapshot): boolean {
  return a.source === b.source && a.status === b.status && a.project === b.project;
}

export function loadSavedOverviewPresets(orgId: string): SavedOverviewFilterPreset[] {
  const parsed = readJson<SavedOverviewFilterPreset[]>(overviewKey(orgId));
  if (!Array.isArray(parsed)) return [];
  return parsed.filter(
    (item) =>
      item
      && typeof item.id === 'string'
      && typeof item.label === 'string'
      && item.filters
  );
}

export function loadSavedRunPresets(orgId: string): SavedRunFilterPreset[] {
  const parsed = readJson<SavedRunFilterPreset[]>(runKey(orgId));
  if (!Array.isArray(parsed)) return [];
  return parsed.filter(
    (item) =>
      item
      && typeof item.id === 'string'
      && typeof item.label === 'string'
      && item.filters
  );
}

export function addSavedOverviewPreset(
  orgId: string,
  label: string,
  filters: OverviewFiltersSnapshot
): { presets: SavedOverviewFilterPreset[]; error?: string } {
  const normalizedLabel = normalizeLabel(label);
  if (!normalizedLabel) {
    return { presets: loadSavedOverviewPresets(orgId), error: 'Preset name is required' };
  }
  const existing = loadSavedOverviewPresets(orgId);
  if (existing.some((preset) => filtersEqual(preset.filters, filters))) {
    return { presets: existing, error: 'These overview filters are already saved' };
  }
  if (existing.length >= MAX_SAVED_PRESETS) {
    return { presets: existing, error: `Maximum ${MAX_SAVED_PRESETS} saved overview presets` };
  }
  const preset: SavedOverviewFilterPreset = {
    id: `saved-overview-${Date.now()}`,
    label: normalizedLabel,
    filters: { ...filters },
  };
  const presets = [...existing, preset];
  if (!writeJson(overviewKey(orgId), presets)) {
    return { presets: existing, error: 'Failed to save overview preset' };
  }
  return { presets };
}

export function removeSavedOverviewPreset(orgId: string, presetId: string): SavedOverviewFilterPreset[] {
  const presets = loadSavedOverviewPresets(orgId).filter((preset) => preset.id !== presetId);
  writeJson(overviewKey(orgId), presets);
  return presets;
}

export function addSavedRunPreset(
  orgId: string,
  label: string,
  filters: RunFiltersSnapshot
): { presets: SavedRunFilterPreset[]; error?: string } {
  const normalizedLabel = normalizeLabel(label);
  if (!normalizedLabel) {
    return { presets: loadSavedRunPresets(orgId), error: 'Preset name is required' };
  }
  const existing = loadSavedRunPresets(orgId);
  if (existing.some((preset) => runFiltersEqual(preset.filters, filters))) {
    return { presets: existing, error: 'These run filters are already saved' };
  }
  if (existing.length >= MAX_SAVED_PRESETS) {
    return { presets: existing, error: `Maximum ${MAX_SAVED_PRESETS} saved run presets` };
  }
  const preset: SavedRunFilterPreset = {
    id: `saved-run-${Date.now()}`,
    label: normalizedLabel,
    filters: { ...filters },
  };
  const presets = [...existing, preset];
  if (!writeJson(runKey(orgId), presets)) {
    return { presets: existing, error: 'Failed to save run preset' };
  }
  return { presets };
}

export function removeSavedRunPreset(orgId: string, presetId: string): SavedRunFilterPreset[] {
  const presets = loadSavedRunPresets(orgId).filter((preset) => preset.id !== presetId);
  writeJson(runKey(orgId), presets);
  return presets;
}

export function clearLocalSavedOverviewPresets(orgId: string) {
  try {
    localStorage.removeItem(overviewKey(orgId));
  } catch {
    // ignore
  }
}

export function clearLocalSavedRunPresets(orgId: string) {
  try {
    localStorage.removeItem(runKey(orgId));
  } catch {
    // ignore
  }
}
