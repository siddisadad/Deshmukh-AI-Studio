import {
  Alert,
  Box,
  Button,
  Chip,
  Stack,
  Switch,
  Typography,
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { ApiError } from '../../../shared/api/types';
import { useAuthStore } from '../../auth/store/authStore';
import { pluginsApi, type OrgPlugin } from '../api/pluginsApi';

export function PluginsSettingsPage() {
  const org = useAuthStore((s) => s.organization);
  const queryClient = useQueryClient();
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const pluginsQuery = useQuery({
    queryKey: ['org-plugins', org?.id],
    queryFn: () => pluginsApi.listOrg(org!.id),
    enabled: !!org?.id,
  });

  const toggle = useMutation({
    mutationFn: ({ pluginId, enabled }: { pluginId: string; enabled: boolean }) =>
      pluginsApi.setEnabled(org!.id, pluginId, enabled),
    onSuccess: async (updated) => {
      setError(null);
      setMessage(
        `${updated.plugin.name} ${updated.enabled ? 'enabled' : 'disabled'}`,
      );
      await queryClient.invalidateQueries({ queryKey: ['org-plugins', org?.id] });
    },
    onError: (err) => {
      setMessage(null);
      setError(err instanceof ApiError ? err.message : 'Failed to update plugin');
    },
  });

  const plugins = pluginsQuery.data ?? [];
  const assistants = plugins.filter((p) => p.plugin.type === 'ASSISTANT');
  const tools = plugins.filter((p) => p.plugin.type === 'TOOL');

  function renderRow(item: OrgPlugin) {
    return (
      <Box
        key={item.plugin.id}
        sx={{
          py: 2,
          borderTop: 1,
          borderColor: 'divider',
          display: 'flex',
          gap: 2,
          alignItems: 'flex-start',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
        }}
      >
        <Box sx={{ flex: 1, minWidth: 220 }}>
          <Stack direction="row" spacing={1} sx={{ alignItems: 'center', mb: 0.5 }}>
            <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
              {item.plugin.name}
            </Typography>
            <Chip size="small" label={item.plugin.type} variant="outlined" />
            {item.plugin.builtin && <Chip size="small" label="built-in" />}
          </Stack>
          <Typography variant="body2" color="text.secondary">
            {item.plugin.description}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            {item.plugin.id} · v{item.plugin.version}
          </Typography>
        </Box>
        {item.canDisable ? (
          <Switch
            checked={item.enabled}
            disabled={toggle.isPending}
            onChange={(_, checked) =>
              toggle.mutate({ pluginId: item.plugin.id, enabled: checked })
            }
            inputProps={{ 'aria-label': `Toggle ${item.plugin.name}` }}
          />
        ) : (
          <Button size="small" disabled variant="text">
            Always on
          </Button>
        )}
      </Box>
    );
  }

  return (
    <Stack spacing={3} sx={{ maxWidth: 760 }}>
      <Box>
        <Typography variant="h4">Plugins</Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
          Assistant and tool extensions discovered via the plugin SPI. Disable sample tools per
          organization; built-in assistants stay on.
        </Typography>
      </Box>

      {error && <Alert severity="error">{error}</Alert>}
      {message && <Alert severity="success">{message}</Alert>}
      {pluginsQuery.isError && (
        <Alert severity="error">
          {pluginsQuery.error instanceof ApiError
            ? pluginsQuery.error.message
            : 'Failed to load plugins'}
        </Alert>
      )}

      <Box>
        <Typography variant="h6" sx={{ mb: 1 }}>
          Assistants
        </Typography>
        {assistants.map(renderRow)}
      </Box>

      <Box>
        <Typography variant="h6" sx={{ mb: 1 }}>
          Tools
        </Typography>
        {tools.map(renderRow)}
      </Box>
    </Stack>
  );
}
