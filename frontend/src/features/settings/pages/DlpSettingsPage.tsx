import {
  Alert,
  Box,
  Button,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { ApiError } from '../../../shared/api/types';
import { useAuthStore } from '../../auth/store/authStore';
import { organizationsApi } from '../../projects/api/organizationsApi';
import { dlpApi, type CreateDlpConnectorRequest } from '../api/dlpApi';

export function DlpSettingsPage() {
  const org = useAuthStore((s) => s.organization);
  const queryClient = useQueryClient();
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [slug, setSlug] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [webhookUrl, setWebhookUrl] = useState('');
  const [connectorType, setConnectorType] = useState<'WEBHOOK' | 'SIEM'>('WEBHOOK');
  const [customPatternsJson, setCustomPatternsJson] = useState('');

  const orgQuery = useQuery({
    queryKey: ['organization', org?.id],
    queryFn: () => organizationsApi.get(org!.id),
    enabled: !!org?.id,
  });

  const connectorsQuery = useQuery({
    queryKey: ['org-dlp-connectors', org?.id],
    queryFn: () => dlpApi.listConnectors(org!.id),
    enabled: !!org?.id,
  });

  const eventsQuery = useQuery({
    queryKey: ['org-dlp-events', org?.id],
    queryFn: () => dlpApi.listEvents(org!.id),
    enabled: !!org?.id,
  });

  const isOwner = orgQuery.data?.role === 'OWNER';

  const create = useMutation({
    mutationFn: (body: CreateDlpConnectorRequest) => dlpApi.createConnector(org!.id, body),
    onSuccess: async () => {
      setError(null);
      setMessage('DLP connector created');
      setSlug('');
      setDisplayName('');
      setWebhookUrl('');
      await queryClient.invalidateQueries({ queryKey: ['org-dlp-connectors', org?.id] });
    },
    onError: (err) => {
      setMessage(null);
      setError(err instanceof ApiError ? err.message : 'Failed to create connector');
    },
  });

  const remove = useMutation({
    mutationFn: (connectorId: string) => dlpApi.deleteConnector(org!.id, connectorId),
    onSuccess: async () => {
      setError(null);
      setMessage('Connector removed');
      await queryClient.invalidateQueries({ queryKey: ['org-dlp-connectors', org?.id] });
    },
    onError: (err) => {
      setMessage(null);
      setError(err instanceof ApiError ? err.message : 'Failed to remove connector');
    },
  });

  function onCreate() {
    create.mutate({
      slug,
      connectorType,
      displayName,
      webhookUrl,
      enabled: true,
      blockOnMatch: true,
      customPatternsJson: customPatternsJson || undefined,
    });
  }

  return (
    <Stack spacing={3} sx={{ maxWidth: 720 }} data-testid="dlp-settings">
      <Box>
        <Typography variant="h4">DLP & SIEM</Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
          Configure per-organization DLP webhook connectors and SIEM export endpoints for thread
          export policy matches.
        </Typography>
      </Box>

      {error && <Alert severity="error">{error}</Alert>}
      {message && <Alert severity="success">{message}</Alert>}

      {connectorsQuery.data && connectorsQuery.data.length > 0 && (
        <Stack spacing={2} data-testid="dlp-connector-list">
          <Typography variant="h6">Connectors</Typography>
          {connectorsQuery.data.map((connector) => (
            <Box key={connector.id} sx={{ py: 1.5, borderTop: 1, borderColor: 'divider' }}>
              <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                {connector.displayName}{' '}
                <Typography component="span" variant="body2" color="text.secondary">
                  ({connector.connectorType})
                </Typography>
              </Typography>
              <Typography variant="body2" color="text.secondary">{connector.webhookUrl}</Typography>
              {isOwner && (
                <Button
                  size="small"
                  color="error"
                  sx={{ mt: 1 }}
                  onClick={() => remove.mutate(connector.id)}
                  disabled={remove.isPending}
                >
                  Remove
                </Button>
              )}
            </Box>
          ))}
        </Stack>
      )}

      {eventsQuery.data && eventsQuery.data.length > 0 && (
        <Stack spacing={1} data-testid="dlp-event-list">
          <Typography variant="h6">Recent DLP events</Typography>
          {eventsQuery.data.slice(0, 10).map((event) => (
            <Typography key={event.id} variant="body2" color="text.secondary">
              {event.createdAt}: {event.matchCategories}
              {event.blocked ? ' (blocked)' : ''}
              {event.siemExportedAt ? ' · SIEM exported' : ''}
            </Typography>
          ))}
        </Stack>
      )}

      {isOwner && (
        <Stack spacing={2}>
          <Typography variant="h6">Add connector</Typography>
          <TextField
            select
            label="Type"
            value={connectorType}
            onChange={(e) => setConnectorType(e.target.value as 'WEBHOOK' | 'SIEM')}
          >
            <MenuItem value="WEBHOOK">WEBHOOK (real-time)</MenuItem>
            <MenuItem value="SIEM">SIEM (batched export)</MenuItem>
          </TextField>
          <TextField label="Slug" value={slug} onChange={(e) => setSlug(e.target.value)} />
          <TextField
            label="Display name"
            value={displayName}
            onChange={(e) => setDisplayName(e.target.value)}
          />
          <TextField
            label="Webhook URL"
            value={webhookUrl}
            onChange={(e) => setWebhookUrl(e.target.value)}
          />
          <TextField
            label="Custom patterns JSON (optional)"
            value={customPatternsJson}
            onChange={(e) => setCustomPatternsJson(e.target.value)}
            multiline
            minRows={2}
            placeholder='[{"category":"internal_id","pattern":"EMP-\\d+","description":"Employee ID"}]'
          />
          <Button
            variant="contained"
            onClick={onCreate}
            disabled={create.isPending || !slug || !displayName || !webhookUrl}
            data-testid="dlp-create-connector"
          >
            {create.isPending ? 'Creating…' : 'Add connector'}
          </Button>
        </Stack>
      )}
    </Stack>
  );
}
