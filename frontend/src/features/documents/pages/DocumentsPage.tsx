import {
  Alert,
  Box,
  Button,
  CircularProgress,
  FormControl,
  InputLabel,
  List,
  ListItemButton,
  ListItemText,
  MenuItem,
  Paper,
  Select,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { useEffect, useState, type FormEvent } from 'react';
import { Link as RouterLink, useParams } from 'react-router-dom';
import { ApiError } from '../../../shared/api/types';
import { EmptyState } from '../../../shared/ui/EmptyState';
import { projectsApi, type Project } from '../../projects/api/projectsApi';
import { documentsApi, type Document } from '../api/documentsApi';

const DOC_TYPES = ['README', 'API_DOC', 'RELEASE_NOTES', 'TECH_DOC', 'OTHER'];

export function DocumentsPage() {
  const { projectId } = useParams();
  const [project, setProject] = useState<Project | null>(null);
  const [documents, setDocuments] = useState<Document[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [title, setTitle] = useState('');
  const [docType, setDocType] = useState('README');
  const [contentMd, setContentMd] = useState('');
  const [newTitle, setNewTitle] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [generating, setGenerating] = useState(false);

  const selected = documents.find((d) => d.id === selectedId) || null;

  async function load() {
    if (!projectId) return;
    setLoading(true);
    setError(null);
    try {
      const [p, list] = await Promise.all([
        projectsApi.getProject(projectId),
        documentsApi.list(projectId),
      ]);
      setProject(p);
      setDocuments(list);
      if (list.length && !selectedId) {
        setSelectedId(list[0].id);
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to load documents');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [projectId]);

  useEffect(() => {
    if (!selected) {
      setTitle('');
      setDocType('README');
      setContentMd('');
      return;
    }
    setTitle(selected.title);
    setDocType(selected.docType);
    setContentMd(selected.contentMd || '');
  }, [selected]);

  async function onCreate(e: FormEvent) {
    e.preventDefault();
    if (!projectId || !newTitle.trim()) return;
    setSaving(true);
    setError(null);
    try {
      const created = await documentsApi.create(projectId, {
        title: newTitle.trim(),
        docType: 'README',
        contentMd: '',
      });
      setNewTitle('');
      setDocuments((prev) => [created, ...prev]);
      setSelectedId(created.id);
      setMessage('Document created');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Create failed');
    } finally {
      setSaving(false);
    }
  }

  async function onSave(e: FormEvent) {
    e.preventDefault();
    if (!selected) return;
    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      const updated = await documentsApi.update(selected.id, { title, docType, contentMd });
      setDocuments((prev) => prev.map((d) => (d.id === updated.id ? updated : d)));
      setMessage('Document saved');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Save failed');
    } finally {
      setSaving(false);
    }
  }

  async function onGenerate() {
    if (!selected) return;
    setGenerating(true);
    setError(null);
    setMessage(null);
    try {
      const result = await documentsApi.generate(selected.id, 'Target engineers on the team');
      setDocuments((prev) => prev.map((d) => (d.id === result.document.id ? result.document : d)));
      setContentMd(result.document.contentMd);
      setMessage(`Generated via ${result.provider}`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Generate failed');
    } finally {
      setGenerating(false);
    }
  }

  if (loading) {
    return (
      <Box sx={{ display: 'grid', placeItems: 'center', py: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Stack spacing={3}>
      <Stack direction={{ xs: 'column', sm: 'row' }} sx={{ justifyContent: 'space-between', gap: 2 }}>
        <Box>
          <Typography variant="overline" color="primary">
            {project?.projectKey} · Documents
          </Typography>
          <Typography variant="h4">{project?.name}</Typography>
        </Box>
        <Button component={RouterLink} to={`/projects/${projectId}`} variant="outlined">
          Overview
        </Button>
      </Stack>

      {error && <Alert severity="error">{error}</Alert>}
      {message && <Alert severity="success">{message}</Alert>}

      <Box
        sx={{
          display: 'grid',
          gap: 2,
          gridTemplateColumns: { xs: '1fr', md: '300px 1fr' },
          alignItems: 'start',
        }}
      >
        <Paper variant="outlined" sx={{ p: 2 }}>
          <Box component="form" onSubmit={onCreate} sx={{ mb: 2 }}>
            <Stack spacing={1}>
              <TextField
                size="small"
                label="New document"
                value={newTitle}
                onChange={(e) => setNewTitle(e.target.value)}
              />
              <Button type="submit" variant="contained" disabled={saving || !newTitle.trim()}>
                Add
              </Button>
            </Stack>
          </Box>
          <List dense>
            {documents.map((doc) => (
              <ListItemButton
                key={doc.id}
                selected={doc.id === selectedId}
                onClick={() => setSelectedId(doc.id)}
              >
                <ListItemText primary={doc.title} secondary={doc.docType} />
              </ListItemButton>
            ))}
            {documents.length === 0 && (
              <Box sx={{ px: 1, pb: 1 }}>
                <EmptyState
                  title="No documents yet"
                  description="Create a README or API doc, then generate a first draft from shared project context."
                />
              </Box>
            )}
          </List>
        </Paper>

        <Paper variant="outlined" sx={{ p: 3 }}>
          {!selected ? (
            <EmptyState
              title="Select a document"
              description="Choose an existing document or create one with the form on the left."
            />
          ) : (
            <Box component="form" onSubmit={onSave}>
              <Stack spacing={2}>
                <TextField label="Title" value={title} onChange={(e) => setTitle(e.target.value)} required />
                <FormControl fullWidth>
                  <InputLabel id="doc-type">Type</InputLabel>
                  <Select
                    labelId="doc-type"
                    label="Type"
                    value={docType}
                    onChange={(e) => setDocType(e.target.value)}
                  >
                    {DOC_TYPES.map((type) => (
                      <MenuItem key={type} value={type}>
                        {type}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
                <Stack direction="row" spacing={1}>
                  <Button variant="outlined" onClick={() => void onGenerate()} disabled={generating}>
                    {generating ? 'Generating…' : 'Generate with Docs AI'}
                  </Button>
                  <Typography variant="body2" color="text.secondary" sx={{ alignSelf: 'center' }}>
                    AI-generated content is editable
                  </Typography>
                </Stack>
                <TextField
                  label="Markdown content"
                  multiline
                  minRows={16}
                  value={contentMd}
                  onChange={(e) => setContentMd(e.target.value)}
                />
                <Button type="submit" variant="contained" disabled={saving}>
                  Save document
                </Button>
              </Stack>
            </Box>
          )}
        </Paper>
      </Box>
    </Stack>
  );
}
