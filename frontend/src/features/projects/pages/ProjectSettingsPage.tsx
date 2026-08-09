import {
  Alert,
  Box,
  Button,
  CircularProgress,
  MenuItem,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ApiError } from '../../../shared/api/types';
import {
  contextAssetsApi,
  type ContextAsset,
  type ContextAssetType,
} from '../api/contextAssetsApi';
import {
  knowledgeApi,
  type KnowledgeHit,
  type KnowledgeStatus,
} from '../api/knowledgeApi';
import { jobsApi, type BackgroundJob } from '../api/jobsApi';
import { chatApi } from '../../chat/api/chatApi';
import { gitLinkApi, type GitSyncRun, type ProjectGitLink } from '../api/gitLinkApi';
import { codeMetadataApi, type CodeMetadataSummary } from '../api/codeMetadataApi';
import { projectsApi, type Project } from '../api/projectsApi';

const ASSET_TYPES: ContextAssetType[] = ['DATABASE_DESIGN', 'API_SPEC', 'SOURCE_METADATA', 'OTHER'];
const GIT_PROVIDERS = ['github', 'gitlab', 'bitbucket'] as const;
type GitProvider = (typeof GIT_PROVIDERS)[number];

export function ProjectSettingsPage() {
  const { projectId } = useParams();
  const navigate = useNavigate();
  const [project, setProject] = useState<Project | null>(null);
  const [name, setName] = useState('');
  const [projectKey, setProjectKey] = useState('');
  const [description, setDescription] = useState('');
  const [chatRetentionDays, setChatRetentionDays] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [assets, setAssets] = useState<ContextAsset[]>([]);
  const [assetType, setAssetType] = useState<ContextAssetType>('API_SPEC');
  const [assetTitle, setAssetTitle] = useState('');
  const [assetContent, setAssetContent] = useState('');
  const [knowledge, setKnowledge] = useState<KnowledgeStatus | null>(null);
  const [knowledgeQuery, setKnowledgeQuery] = useState('');
  const [knowledgeHits, setKnowledgeHits] = useState<KnowledgeHit[]>([]);
  const [jobs, setJobs] = useState<BackgroundJob[]>([]);
  const [codeMetadata, setCodeMetadata] = useState<CodeMetadataSummary | null>(null);
  const [codeManifestJson, setCodeManifestJson] = useState('');
  const [gitLink, setGitLink] = useState<ProjectGitLink | null>(null);
  const [gitRepository, setGitRepository] = useState('');
  const [gitBranch, setGitBranch] = useState('main');
  const [gitProvider, setGitProvider] = useState<GitProvider>('github');
  const [gitEnabled, setGitEnabled] = useState(true);
  const [gitScheduledSyncEnabled, setGitScheduledSyncEnabled] = useState(true);
  const [gitSyncIntervalMinutes, setGitSyncIntervalMinutes] = useState('');
  const [gitPathIgnorePatterns, setGitPathIgnorePatterns] = useState('');
  const [gitPathIncludePatterns, setGitPathIncludePatterns] = useState('');
  const [gitSyncRuns, setGitSyncRuns] = useState<GitSyncRun[]>([]);
  const [gitSyncRunSourceFilter, setGitSyncRunSourceFilter] = useState<'all' | 'manual' | 'scheduled' | 'webhook'>('all');
  const [gitSyncRunStatusFilter, setGitSyncRunStatusFilter] = useState<'all' | 'success' | 'failed'>('all');
  const [showGitWebhookSecret, setShowGitWebhookSecret] = useState(false);

  useEffect(() => {
    if (!projectId) return;
    setLoading(true);
    Promise.all([
      projectsApi.getProject(projectId),
      contextAssetsApi.list(projectId),
      knowledgeApi.status(projectId),
      jobsApi.list(projectId, 10),
      codeMetadataApi.summary(projectId),
      gitLinkApi.get(projectId),
      gitLinkApi.listSyncRuns(projectId, 20, { source: 'all', status: 'all' }),
    ])
      .then(([p, listed, status, listedJobs, codeSummary, link, syncRuns]) => {
        setProject(p);
        setName(p.name);
        setProjectKey(p.projectKey);
        setDescription(p.description || '');
        setChatRetentionDays(
          p.chatRetentionDays != null && p.chatRetentionDays > 0 ? String(p.chatRetentionDays) : '',
        );
        setAssets(listed);
        setKnowledge(status);
        setJobs(listedJobs);
        setCodeMetadata(codeSummary);
        setGitLink(link);
        setGitSyncRuns(syncRuns);
        setGitRepository(link.repository || '');
        setGitBranch(link.branch || 'main');
        setGitProvider(
          GIT_PROVIDERS.includes(link.provider as GitProvider) ? (link.provider as GitProvider) : 'github',
        );
        setGitEnabled(link.enabled);
        setGitScheduledSyncEnabled(link.scheduledSyncEnabled ?? true);
        setGitSyncIntervalMinutes(
          link.scheduledSyncIntervalMinutes != null && link.scheduledSyncIntervalMinutes > 0
            ? String(link.scheduledSyncIntervalMinutes)
            : '',
        );
        setGitPathIgnorePatterns(
          link.pathIgnorePatterns?.length ? link.pathIgnorePatterns.join('\n') : '',
        );
        setGitPathIncludePatterns(
          link.pathIncludePatterns?.length ? link.pathIncludePatterns.join('\n') : '',
        );
        const current = listed.find((a) => a.assetType === 'API_SPEC') || listed[0];
        if (current) {
          setAssetType(current.assetType);
          setAssetTitle(current.title);
          setAssetContent(current.content);
        }
      })
      .catch((err) => setError(err instanceof ApiError ? err.message : 'Failed to load project'))
      .finally(() => setLoading(false));
  }, [projectId]);

  function selectAsset(type: ContextAssetType) {
    setAssetType(type);
    const existing = assets.find((a) => a.assetType === type);
    setAssetTitle(existing?.title || '');
    setAssetContent(existing?.content || '');
  }

  async function onSave(e: FormEvent) {
    e.preventDefault();
    if (!projectId) return;
    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      const retentionTrimmed = chatRetentionDays.trim();
      const body: {
        name: string;
        projectKey: string;
        description: string;
        chatRetentionDays?: number;
        clearChatRetention?: boolean;
      } = { name, projectKey, description };
      if (retentionTrimmed) {
        body.chatRetentionDays = Number(retentionTrimmed);
      } else {
        body.clearChatRetention = true;
      }
      const updated = await projectsApi.updateProject(projectId, body);
      setProject(updated);
      setMessage('Project updated');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Update failed');
    } finally {
      setSaving(false);
    }
  }

  async function loadGitSyncRuns(
    source = gitSyncRunSourceFilter,
    status = gitSyncRunStatusFilter,
  ) {
    if (!projectId) return;
    const runs = await gitLinkApi.listSyncRuns(projectId, 20, { source, status });
    setGitSyncRuns(runs);
  }

  async function onSaveGitLink(e: FormEvent) {
    e.preventDefault();
    if (!projectId || !gitRepository.trim()) return;
    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      const intervalTrimmed = gitSyncIntervalMinutes.trim();
      const patternLines = gitPathIgnorePatterns
        .split('\n')
        .map((line) => line.trim())
        .filter((line) => line.length > 0);
      const includeLines = gitPathIncludePatterns
        .split('\n')
        .map((line) => line.trim())
        .filter((line) => line.length > 0);
      const link = await gitLinkApi.upsert(projectId, {
        provider: gitProvider,
        repository: gitRepository.trim(),
        branch: gitBranch.trim() || 'main',
        enabled: gitEnabled,
        scheduledSyncEnabled: gitScheduledSyncEnabled,
        ...(intervalTrimmed
          ? { scheduledSyncIntervalMinutes: Number(intervalTrimmed) }
          : { clearScheduledSyncInterval: true }),
        ...(includeLines.length > 0
          ? { pathIncludePatterns: includeLines }
          : { clearPathIncludePatterns: true }),
        ...(patternLines.length > 0
          ? { pathIgnorePatterns: patternLines }
          : { clearPathIgnorePatterns: true }),
      });
      setGitLink(link);
      setMessage(`Git link saved — configure ${gitProvider} webhook with the secret below`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to save Git link');
    } finally {
      setSaving(false);
    }
  }

  async function onSyncGitNow() {
    if (!projectId) return;
    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      const link = await gitLinkApi.syncNow(projectId);
      setGitLink(link);
      setGitSyncRuns(await gitLinkApi.listSyncRuns(projectId, 20, {
        source: gitSyncRunSourceFilter,
        status: gitSyncRunStatusFilter,
      }));
      setCodeMetadata(await codeMetadataApi.summary(projectId));
      setKnowledge(await knowledgeApi.status(projectId));
      setMessage(`Git sync succeeded · status ${link.lastSyncStatus}`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Git sync failed');
    } finally {
      setSaving(false);
    }
  }

  async function onTestGitConnection() {
    if (!projectId) return;
    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      const result = await gitLinkApi.testConnection(projectId);
      setMessage(
        result.ok
          ? `Connection test passed: ${result.checks.map((c) => c.name).join(', ')}`
          : `Connection test failed: ${result.message}`
      );
      if (!result.ok) {
        setError(result.checks.map((c) => `${c.name}: ${c.message}`).join(' · '));
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Connection test failed');
    } finally {
      setSaving(false);
    }
  }

  async function onRegenerateWebhookSecret() {
    if (!projectId || !gitLink?.id) return;
    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      const link = await gitLinkApi.regenerateWebhookSecret(projectId);
      setGitLink(link);
      setShowGitWebhookSecret(true);
      setMessage('Webhook secret regenerated — update your git host webhook configuration');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to regenerate webhook secret');
    } finally {
      setSaving(false);
    }
  }

  async function onDisconnectGitLink() {
    if (!projectId || !gitLink?.id) return;
    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      await gitLinkApi.delete(projectId);
      setGitLink(null);
      setGitRepository('');
      setGitBranch('main');
      setGitSyncRuns([]);
      setShowGitWebhookSecret(false);
      setMessage('Git link disconnected');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to disconnect git link');
    } finally {
      setSaving(false);
    }
  }

  async function copyGitWebhookValue(label: string, value: string) {
    try {
      await navigator.clipboard.writeText(value);
      setMessage(`${label} copied to clipboard`);
      setError(null);
    } catch {
      setError(`Failed to copy ${label}`);
    }
  }

  async function onSyncGitAsync() {
    if (!projectId) return;
    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      const job = await gitLinkApi.syncAsync(projectId);
      setJobs((prev) => [job, ...prev.filter((j) => j.id !== job.id)].slice(0, 10));
      setMessage(`Git sync job queued (${job.id.slice(0, 8)}…)`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to queue Git sync');
    } finally {
      setSaving(false);
    }
  }

  async function onSaveCodeManifest(e: FormEvent) {
    e.preventDefault();
    if (!projectId) return;
    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      const parsed = JSON.parse(codeManifestJson) as Array<{
        path: string;
        language?: string;
        snippet?: string;
        sizeBytes?: number;
      }>;
      if (!Array.isArray(parsed)) {
        throw new Error('Manifest must be a JSON array');
      }
      const summary = await codeMetadataApi.replace(projectId, parsed);
      setCodeMetadata(summary);
      setKnowledge(await knowledgeApi.status(projectId));
      setMessage(`Code metadata saved · ${summary.fileCount} files indexed for RAG`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : err instanceof Error ? err.message : 'Invalid manifest');
    } finally {
      setSaving(false);
    }
  }

  async function onSaveAsset(e: FormEvent) {
    e.preventDefault();
    if (!projectId) return;
    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      const saved = await contextAssetsApi.upsert(projectId, assetType, {
        title: assetTitle,
        content: assetContent,
        metadata: '{}',
      });
      setAssets((prev) => {
        const others = prev.filter((a) => a.assetType !== saved.assetType);
        return [...others, saved].sort((a, b) => a.assetType.localeCompare(b.assetType));
      });
      setMessage('Context asset saved — included in AI prompts and knowledge index');
      setKnowledge(await knowledgeApi.status(projectId));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to save context asset');
    } finally {
      setSaving(false);
    }
  }

  async function onReindex() {
    if (!projectId) return;
    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      const job = await jobsApi.reindexAsync(projectId);
      setJobs((prev) => [job, ...prev.filter((j) => j.id !== job.id)].slice(0, 10));
      setMessage(`Reindex job queued (${job.id.slice(0, 8)}…) — waiting for worker`);
      for (let i = 0; i < 15; i++) {
        await new Promise((r) => setTimeout(r, 800));
        const latest = await jobsApi.get(job.id);
        setJobs((prev) => [latest, ...prev.filter((j) => j.id !== latest.id)].slice(0, 10));
        if (latest.status === 'SUCCEEDED' || latest.status === 'FAILED') {
          if (latest.status === 'SUCCEEDED') {
            const status = await knowledgeApi.status(projectId);
            setKnowledge(status);
            setMessage(
              status.corpusLimitReached
                ? `Reindex succeeded · ${status.indexedChunks} chunks (corpus limit reached)`
                : `Reindex succeeded · ${status.indexedChunks} chunks indexed`
            );
          } else {
            setMessage(`Reindex failed: ${latest.errorMessage || 'unknown error'}`);
          }
          break;
        }
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Reindex failed');
    } finally {
      setSaving(false);
    }
  }

  async function onSearchKnowledge(e: FormEvent) {
    e.preventDefault();
    if (!projectId || !knowledgeQuery.trim()) return;
    setSaving(true);
    setError(null);
    try {
      const result = await knowledgeApi.search(projectId, knowledgeQuery.trim());
      setKnowledgeHits(result.hits);
      setKnowledge((prev) => ({
        enabled: true,
        embeddingProvider: result.embeddingProvider,
        indexedChunks: result.indexedChunks,
        maxChunksPerProject: prev?.maxChunksPerProject ?? 10000,
        corpusLimitReached:
          prev?.corpusLimitReached ?? result.indexedChunks >= (prev?.maxChunksPerProject ?? 10000),
      }));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Search failed');
    } finally {
      setSaving(false);
    }
  }

  async function onPurgeExpiredThreads() {
    if (!projectId) return;
    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      const result = await chatApi.purgeExpiredConversations(projectId);
      setMessage(`Purged ${result.purgedCount} expired thread(s) (legal hold skipped)`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Retention purge failed');
    } finally {
      setSaving(false);
    }
  }

  async function onArchiveToggle() {
    if (!projectId || !project) return;
    setSaving(true);
    setError(null);
    try {
      const updated =
        project.status === 'ARCHIVED'
          ? await projectsApi.unarchiveProject(projectId)
          : await projectsApi.archiveProject(projectId);
      setProject(updated);
      setMessage(updated.status === 'ARCHIVED' ? 'Project archived' : 'Project restored');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Archive action failed');
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return (
      <Box sx={{ display: 'grid', placeItems: 'center', py: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (!project) {
    return <Alert severity="error">{error || 'Project not found'}</Alert>;
  }

  return (
    <Stack spacing={3} sx={{ maxWidth: 720 }}>
      <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h4">Project settings</Typography>
        <Button onClick={() => navigate(`/projects/${project.id}`)}>Back</Button>
      </Stack>

      <Paper component="form" onSubmit={onSave} variant="outlined" sx={{ p: 3 }}>
        <Stack spacing={2}>
          {error && <Alert severity="error">{error}</Alert>}
          {message && <Alert severity="success">{message}</Alert>}
          <TextField label="Name" value={name} onChange={(e) => setName(e.target.value)} required />
          <TextField label="Key" value={projectKey} onChange={(e) => setProjectKey(e.target.value.toUpperCase())} required />
          <TextField
            label="Description"
            multiline
            minRows={3}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />
          <TextField
            label="Chat thread retention (days)"
            type="number"
            value={chatRetentionDays}
            onChange={(e) => setChatRetentionDays(e.target.value)}
            helperText="Optional auto-delete after N days since last message. Leave empty to disable. Legal hold threads are never purged."
            slotProps={{ htmlInput: { min: 1, max: 3650, 'data-testid': 'chat-retention-days' } }}
          />
          <Button type="submit" variant="contained" disabled={saving}>
            Save changes
          </Button>
          <Button
            type="button"
            variant="outlined"
            disabled={saving || !chatRetentionDays.trim()}
            onClick={() => void onPurgeExpiredThreads()}
            data-testid="chat-retention-purge"
          >
            Purge expired threads now
          </Button>
        </Stack>
      </Paper>

      <Paper component="form" onSubmit={onSaveAsset} variant="outlined" sx={{ p: 3 }}>
        <Stack spacing={2}>
          <Typography variant="h6">Shared AI context assets</Typography>
          <Typography color="text.secondary">
            Database design, API specs, and source metadata are injected into assistant prompts for this project.
          </Typography>
          <TextField
            select
            label="Asset type"
            value={assetType}
            onChange={(e) => selectAsset(e.target.value as ContextAssetType)}
          >
            {ASSET_TYPES.map((type) => (
              <MenuItem key={type} value={type}>
                {type.replaceAll('_', ' ')}
                {assets.some((a) => a.assetType === type) ? ' · saved' : ''}
              </MenuItem>
            ))}
          </TextField>
          <TextField
            label="Title"
            value={assetTitle}
            onChange={(e) => setAssetTitle(e.target.value)}
            required
          />
          <TextField
            label="Content"
            multiline
            minRows={8}
            value={assetContent}
            onChange={(e) => setAssetContent(e.target.value)}
            placeholder="Paste schema, OpenAPI snippets, or module notes…"
            slotProps={{ htmlInput: { 'data-testid': 'context-asset-content' } }}
          />
          <Button
            type="submit"
            variant="contained"
            disabled={saving || !assetTitle.trim()}
            data-testid="context-asset-save"
          >
            Save context asset
          </Button>
        </Stack>
      </Paper>

      <Paper component="form" onSubmit={onSaveGitLink} variant="outlined" sx={{ p: 3 }}>
        <Stack spacing={2}>
          <Typography variant="h6">Git repository sync</Typography>
          <Typography color="text.secondary">
            Link a Git host repository for automatic code metadata sync. Last sync:{' '}
            {gitLink?.lastSyncStatus || 'never'}
            {gitLink?.lastSyncedAt ? ` · ${new Date(gitLink.lastSyncedAt).toLocaleString()}` : ''}
          </Typography>
          <TextField
            select
            label="Provider"
            value={gitProvider}
            onChange={(e) => setGitProvider(e.target.value as GitProvider)}
            slotProps={{ htmlInput: { 'data-testid': 'git-provider' } }}
          >
            {GIT_PROVIDERS.map((provider) => (
              <MenuItem key={provider} value={provider}>
                {provider}
              </MenuItem>
            ))}
          </TextField>
          <TextField
            label={
              gitProvider === 'bitbucket'
                ? 'Repository (workspace/slug)'
                : gitProvider === 'gitlab'
                  ? 'Repository (namespace/project)'
                  : 'Repository (owner/name)'
            }
            value={gitRepository}
            onChange={(e) => setGitRepository(e.target.value)}
            placeholder="acme/platform-api"
            required
            slotProps={{ htmlInput: { 'data-testid': 'git-repository' } }}
          />
          <TextField
            label="Branch"
            value={gitBranch}
            onChange={(e) => setGitBranch(e.target.value)}
            slotProps={{ htmlInput: { 'data-testid': 'git-branch' } }}
          />
          <TextField
            select
            label="Enabled"
            value={gitEnabled ? 'yes' : 'no'}
            onChange={(e) => setGitEnabled(e.target.value === 'yes')}
          >
            <MenuItem value="yes">Enabled</MenuItem>
            <MenuItem value="no">Disabled</MenuItem>
          </TextField>
          <TextField
            select
            label="Scheduled sync"
            value={gitScheduledSyncEnabled ? 'yes' : 'no'}
            onChange={(e) => setGitScheduledSyncEnabled(e.target.value === 'yes')}
            helperText="When disabled, webhooks and manual sync still work."
            slotProps={{ htmlInput: { 'data-testid': 'git-scheduled-sync' } }}
          >
            <MenuItem value="yes">Enabled</MenuItem>
            <MenuItem value="no">Disabled</MenuItem>
          </TextField>
          <TextField
            label="Scheduled sync interval (minutes)"
            value={gitSyncIntervalMinutes}
            onChange={(e) => setGitSyncIntervalMinutes(e.target.value)}
            placeholder="Platform default (e.g. 60)"
            helperText="Optional per-project override (15–10080). Leave blank for platform GIT_SYNC_SCHEDULED_INTERVAL_MS."
            slotProps={{ htmlInput: { 'data-testid': 'git-sync-interval' } }}
          />
          <TextField
            label="Path include patterns"
            value={gitPathIncludePatterns}
            onChange={(e) => setGitPathIncludePatterns(e.target.value)}
            multiline
            minRows={2}
            placeholder="src/**\ndocs/**/*.md"
            helperText="Optional scope limit — one Ant-style glob per line. Empty syncs all paths (after ignores)."
            slotProps={{ htmlInput: { 'data-testid': 'git-path-include' } }}
          />
          <TextField
            label="Path ignore patterns"
            value={gitPathIgnorePatterns}
            onChange={(e) => setGitPathIgnorePatterns(e.target.value)}
            multiline
            minRows={3}
            placeholder="README.md\n**/node_modules/**\n*.min.js"
            helperText="One Ant-style glob per line. Matched paths are skipped during git sync."
            slotProps={{ htmlInput: { 'data-testid': 'git-path-ignore' } }}
          />
          {gitLink?.webhookUrl && (
            <Stack spacing={1}>
              <Typography variant="subtitle2">Webhook setup</Typography>
              <Typography variant="body2" color="text.secondary">
                {gitProvider === 'github'
                  ? 'GitHub: repository Settings → Webhooks → content type application/json, secret below.'
                  : gitProvider === 'gitlab'
                    ? 'GitLab: project Settings → Webhooks → secret token below.'
                    : 'Bitbucket: repository Settings → Webhooks → secret below.'}
              </Typography>
              <Stack direction="row" spacing={1} sx={{ alignItems: 'center', flexWrap: 'wrap' }}>
                <Typography variant="body2" sx={{ fontFamily: 'monospace', fontSize: '0.85rem' }}>
                  URL: {gitLink.webhookUrl}
                </Typography>
                <Button
                  size="small"
                  variant="outlined"
                  onClick={() => void copyGitWebhookValue('Webhook URL', gitLink.webhookUrl)}
                  data-testid="git-webhook-copy-url"
                >
                  Copy URL
                </Button>
              </Stack>
              {gitLink.webhookSecret && (
                <Stack direction="row" spacing={1} sx={{ alignItems: 'center', flexWrap: 'wrap' }}>
                  <Typography variant="body2" sx={{ fontFamily: 'monospace', fontSize: '0.85rem' }}>
                    Secret: {showGitWebhookSecret ? gitLink.webhookSecret : `${gitLink.webhookSecret.slice(0, 8)}…`}
                  </Typography>
                  <Button
                    size="small"
                    variant="outlined"
                    onClick={() => setShowGitWebhookSecret((v) => !v)}
                    data-testid="git-webhook-reveal-secret"
                  >
                    {showGitWebhookSecret ? 'Hide' : 'Reveal'}
                  </Button>
                  <Button
                    size="small"
                    variant="outlined"
                    onClick={() => void copyGitWebhookValue('Webhook secret', gitLink.webhookSecret!)}
                    data-testid="git-webhook-copy-secret"
                  >
                    Copy secret
                  </Button>
                  <Button
                    size="small"
                    variant="outlined"
                    onClick={() => void onRegenerateWebhookSecret()}
                    disabled={saving}
                    data-testid="git-webhook-regenerate"
                  >
                    Regenerate secret
                  </Button>
                </Stack>
              )}
            </Stack>
          )}
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
            <Button type="submit" variant="contained" disabled={saving || !gitRepository.trim()} data-testid="git-link-save">
              Save Git link
            </Button>
            <Button variant="outlined" onClick={() => void onTestGitConnection()} disabled={saving || !gitLink?.id} data-testid="git-test-connection">
              Test connection
            </Button>
            <Button variant="outlined" onClick={() => void onSyncGitNow()} disabled={saving || !gitLink?.id} data-testid="git-sync-now">
              Sync now
            </Button>
            <Button variant="outlined" onClick={() => void onSyncGitAsync()} disabled={saving || !gitLink?.id} data-testid="git-sync-async">
              Sync in background
            </Button>
            <Button
              variant="outlined"
              color="warning"
              onClick={() => void onDisconnectGitLink()}
              disabled={saving || !gitLink?.id}
              data-testid="git-link-disconnect"
            >
              Disconnect
            </Button>
          </Stack>
          {gitLink?.lastSyncError && (
            <Typography color="error" variant="body2">{gitLink.lastSyncError}</Typography>
          )}
          <Stack spacing={1}>
            <Typography variant="subtitle2">Recent sync runs</Typography>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} sx={{ alignItems: 'flex-start' }}>
              <TextField
                select
                label="Source"
                size="small"
                value={gitSyncRunSourceFilter}
                onChange={(e) => {
                  const value = e.target.value as 'all' | 'manual' | 'scheduled' | 'webhook';
                  setGitSyncRunSourceFilter(value);
                  void loadGitSyncRuns(value, gitSyncRunStatusFilter);
                }}
                slotProps={{ htmlInput: { 'data-testid': 'git-sync-run-source-filter' } }}
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
                value={gitSyncRunStatusFilter}
                onChange={(e) => {
                  const value = e.target.value as 'all' | 'success' | 'failed';
                  setGitSyncRunStatusFilter(value);
                  void loadGitSyncRuns(gitSyncRunSourceFilter, value);
                }}
                slotProps={{ htmlInput: { 'data-testid': 'git-sync-run-status-filter' } }}
              >
                <MenuItem value="all">All statuses</MenuItem>
                <MenuItem value="success">Success</MenuItem>
                <MenuItem value="failed">Failed</MenuItem>
              </TextField>
              <Button
                size="small"
                variant="outlined"
                onClick={() => void loadGitSyncRuns()}
                disabled={saving || !gitLink?.id}
                data-testid="git-sync-runs-refresh"
              >
                Refresh
              </Button>
            </Stack>
            {gitSyncRuns.length === 0 && (
              <Typography variant="body2" color="text.secondary">No sync runs match the current filters.</Typography>
            )}
            {gitSyncRuns.map((run) => (
              <Typography
                key={run.id}
                variant="body2"
                color={run.status === 'failed' ? 'error' : 'text.secondary'}
              >
                {new Date(run.finishedAt).toLocaleString()} · {run.source} · {run.status}
                {run.status === 'success' ? ` · ${run.fileCount} files` : ''}
                {run.errorMessage ? ` · ${run.errorMessage}` : ''}
              </Typography>
            ))}
          </Stack>
        </Stack>
      </Paper>

      <Paper component="form" onSubmit={onSaveCodeManifest} variant="outlined" sx={{ p: 3 }}>
        <Stack spacing={2}>
          <Typography variant="h6">Code metadata (RAG)</Typography>
          <Typography color="text.secondary">
            Upload a manifest of repository paths and optional snippets for semantic search.{' '}
            {codeMetadata?.fileCount ?? 0} / {codeMetadata?.maxFilesPerProject ?? 500} files stored.
          </Typography>
          <TextField
            label="Manifest JSON"
            multiline
            minRows={6}
            value={codeManifestJson}
            onChange={(e) => setCodeManifestJson(e.target.value)}
            placeholder={`[
  {"path":"src/auth/LoginService.java","language":"java","snippet":"class LoginService { ... }","sizeBytes":1200}
]`}
            helperText="Array of path, language, snippet, and sizeBytes. Replaces the full manifest and reindexes."
            slotProps={{ htmlInput: { 'data-testid': 'code-metadata-manifest' } }}
          />
          <Button
            type="submit"
            variant="contained"
            disabled={saving || !codeManifestJson.trim()}
            data-testid="code-metadata-save"
          >
            Save manifest & reindex
          </Button>
        </Stack>
      </Paper>

      <Paper variant="outlined" sx={{ p: 3 }}>
        <Stack spacing={2} component="form" onSubmit={onSearchKnowledge}>
          <Typography variant="h6">Knowledge index (RAG)</Typography>
          <Typography color="text.secondary">
            Embeddings power semantic retrieval into chat and AI actions. Provider:{' '}
            {knowledge?.embeddingProvider || 'mock'} · {knowledge?.indexedChunks ?? 0} /{' '}
            {knowledge?.maxChunksPerProject ?? 10000} chunks indexed.
          </Typography>
          {knowledge?.corpusLimitReached && (
            <Typography color="warning.main" variant="body2">
              Corpus limit reached — reindex truncated content. Raise RAG_MAX_CHUNKS_PER_PROJECT or prune
              sources.
            </Typography>
          )}
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
            <Button
              variant="outlined"
              onClick={() => void onReindex()}
              disabled={saving}
              data-testid="knowledge-reindex"
            >
              Reindex in background
            </Button>
          </Stack>
          <TextField
            label="Search knowledge"
            value={knowledgeQuery}
            onChange={(e) => setKnowledgeQuery(e.target.value)}
            placeholder="e.g. password reset API"
            slotProps={{ htmlInput: { 'data-testid': 'knowledge-search-input' } }}
          />
          <Button
            type="submit"
            variant="contained"
            disabled={saving || !knowledgeQuery.trim()}
            data-testid="knowledge-search-submit"
          >
            Search
          </Button>
          {knowledgeHits.map((hit) => (
            <Box
              key={hit.id}
              data-testid="knowledge-hit"
              sx={{ borderTop: '1px solid', borderColor: 'divider', pt: 1.5 }}
            >
              <Typography variant="subtitle2">
                [{hit.sourceType}] {hit.title} · score {hit.score.toFixed(3)}
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ whiteSpace: 'pre-wrap' }}>
                {hit.content.length > 320 ? `${hit.content.slice(0, 320)}…` : hit.content}
              </Typography>
            </Box>
          ))}
          {jobs.length > 0 && (
            <Box sx={{ borderTop: '1px solid', borderColor: 'divider', pt: 2 }}>
              <Typography variant="subtitle2" gutterBottom>
                Recent background jobs
              </Typography>
              <Stack spacing={1}>
                {jobs.map((job) => (
                  <Typography key={job.id} variant="body2" color="text.secondary">
                    {job.jobType.replaceAll('_', ' ').toLowerCase()} · {job.status.toLowerCase()} ·{' '}
                    {new Date(job.createdAt).toLocaleString()}
                  </Typography>
                ))}
              </Stack>
            </Box>
          )}
        </Stack>
      </Paper>

      <Paper variant="outlined" sx={{ p: 3 }}>
        <Typography variant="h6" gutterBottom>
          Archive
        </Typography>
        <Typography color="text.secondary" sx={{ mb: 2 }}>
          Archived projects are hidden from the default dashboard.
        </Typography>
        <Button color={project.status === 'ARCHIVED' ? 'primary' : 'warning'} variant="outlined" onClick={() => void onArchiveToggle()} disabled={saving}>
          {project.status === 'ARCHIVED' ? 'Restore project' : 'Archive project'}
        </Button>
      </Paper>
    </Stack>
  );
}
