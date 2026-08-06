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
import { projectsApi, type Project } from '../api/projectsApi';

const ASSET_TYPES: ContextAssetType[] = ['DATABASE_DESIGN', 'API_SPEC', 'SOURCE_METADATA', 'OTHER'];

export function ProjectSettingsPage() {
  const { projectId } = useParams();
  const navigate = useNavigate();
  const [project, setProject] = useState<Project | null>(null);
  const [name, setName] = useState('');
  const [projectKey, setProjectKey] = useState('');
  const [description, setDescription] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [assets, setAssets] = useState<ContextAsset[]>([]);
  const [assetType, setAssetType] = useState<ContextAssetType>('API_SPEC');
  const [assetTitle, setAssetTitle] = useState('');
  const [assetContent, setAssetContent] = useState('');

  useEffect(() => {
    if (!projectId) return;
    setLoading(true);
    Promise.all([projectsApi.getProject(projectId), contextAssetsApi.list(projectId)])
      .then(([p, listed]) => {
        setProject(p);
        setName(p.name);
        setProjectKey(p.projectKey);
        setDescription(p.description || '');
        setAssets(listed);
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
      const updated = await projectsApi.updateProject(projectId, {
        name,
        projectKey,
        description,
      });
      setProject(updated);
      setMessage('Project updated');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Update failed');
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
      setMessage('Context asset saved — included in AI prompts');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to save context asset');
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
          <Button type="submit" variant="contained" disabled={saving}>
            Save changes
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
          />
          <Button type="submit" variant="contained" disabled={saving || !assetTitle.trim()}>
            Save context asset
          </Button>
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
