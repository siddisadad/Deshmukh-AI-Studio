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
import { useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { ApiError } from '../../../shared/api/types';
import { useAuthStore } from '../../auth/store/authStore';
import { organizationsApi } from '../../projects/api/organizationsApi';
import {
  gitCredentialsApi,
  type GitConnectionTestResult,
  type OrgGitCredential,
} from '../api/gitCredentialsApi';

const PROVIDERS = ['github', 'gitlab', 'bitbucket'] as const;

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
    queryKey: ['org-git-sync-overview', org?.id],
    queryFn: () => gitCredentialsApi.getSyncOverview(org!.id),
    enabled: !!org?.id,
  });

  const isOwner = orgQuery.data?.role === 'OWNER';
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
            {syncOverviewQuery.data.linkedProjects} of {syncOverviewQuery.data.totalProjects} projects linked
            · {syncOverviewQuery.data.enabledLinks} enabled
            {syncOverviewQuery.data.failedLastSync > 0
              ? ` · ${syncOverviewQuery.data.failedLastSync} failed last sync`
              : ''}
          </Typography>
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
                  · last sync {item.lastSyncStatus}
                  {item.lastSyncedAt ? ` · ${new Date(item.lastSyncedAt).toLocaleString()}` : ''}
                </Typography>
              ) : (
                <Typography variant="body2" color="text.secondary">No git link</Typography>
              )}
              <Link
                component={RouterLink}
                to={`/projects/${item.projectId}/settings`}
                variant="body2"
                data-testid={`git-sync-overview-link-${item.projectKey}`}
              >
                Project settings
              </Link>
            </Stack>
          ))}
          <Button
            variant="outlined"
            size="small"
            onClick={() => void syncOverviewQuery.refetch()}
            data-testid="git-sync-overview-refresh"
          >
            Refresh overview
          </Button>
        </Stack>
      )}

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
