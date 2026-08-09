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
import { pluginsApi, type OrgPlugin, type OrgPluginPack } from '../api/pluginsApi';

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

  const packsQuery = useQuery({
    queryKey: ['org-plugin-packs', org?.id],
    queryFn: () => pluginsApi.listOrgPacks(org!.id),
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

  const installPack = useMutation({
    mutationFn: (packId: string) => pluginsApi.installPack(org!.id, packId),
    onSuccess: async (updated) => {
      setError(null);
      setMessage(`Installed ${updated.pack.name}`);
      await queryClient.invalidateQueries({ queryKey: ['org-plugin-packs', org?.id] });
      await queryClient.invalidateQueries({ queryKey: ['org-plugins', org?.id] });
    },
    onError: (err) => {
      setMessage(null);
      setError(err instanceof ApiError ? err.message : 'Failed to install pack');
    },
  });

  const uninstallPack = useMutation({
    mutationFn: (packId: string) => pluginsApi.uninstallPack(org!.id, packId),
    onSuccess: async () => {
      setError(null);
      setMessage('Pack removed');
      await queryClient.invalidateQueries({ queryKey: ['org-plugin-packs', org?.id] });
      await queryClient.invalidateQueries({ queryKey: ['org-plugins', org?.id] });
    },
    onError: (err) => {
      setMessage(null);
      setError(err instanceof ApiError ? err.message : 'Failed to uninstall pack');
    },
  });

  const plugins = pluginsQuery.data ?? [];
  const assistants = plugins.filter((p) => p.plugin.type === 'ASSISTANT');
  const tools = plugins.filter((p) => p.plugin.type === 'TOOL');
  const packs = packsQuery.data ?? [];

  function renderRow(item: OrgPlugin) {
    return (
      <Box
        key={item.plugin.id}
        data-testid={`plugin-row-${item.plugin.id}`}
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
          <Box data-testid={`plugin-toggle-${item.plugin.id}`}>
            <Switch
              checked={item.enabled}
              disabled={toggle.isPending}
              onChange={(_, checked) =>
                toggle.mutate({ pluginId: item.plugin.id, enabled: checked })
              }
              slotProps={{ input: { 'aria-label': `Toggle ${item.plugin.name}` } }}
            />
          </Box>
        ) : (
          <Button size="small" disabled variant="text">
            Always on
          </Button>
        )}
      </Box>
    );
  }

  function renderPackRow(item: OrgPluginPack) {
    return (
      <Box
        key={item.pack.id}
        data-testid={`plugin-pack-row-${item.pack.id}`}
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
              {item.pack.name}
            </Typography>
            {item.pack.verified && <Chip size="small" label="verified" color="success" />}
            <Chip size="small" label={item.pack.publisher} variant="outlined" />
          </Stack>
          <Typography variant="body2" color="text.secondary">
            {item.pack.description}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            {item.pack.id} · v{item.pack.version} · {item.pack.pluginIds.length} plugins
          </Typography>
        </Box>
        {item.installed ? (
          <Button
            size="small"
            variant="outlined"
            color="warning"
            data-testid={`plugin-pack-uninstall-${item.pack.id}`}
            disabled={uninstallPack.isPending}
            onClick={() => uninstallPack.mutate(item.pack.id)}
          >
            Uninstall
          </Button>
        ) : (
          <Button
            size="small"
            variant="contained"
            data-testid={`plugin-pack-install-${item.pack.id}`}
            disabled={installPack.isPending}
            onClick={() => installPack.mutate(item.pack.id)}
          >
            Install
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
          Built-in assistants, optional tools, and marketplace packs. Install third-party packs to
          unlock additional tools for your organization.
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
          Marketplace packs
        </Typography>
        {packsQuery.isLoading && (
          <Typography variant="body2" color="text.secondary">Loading marketplace…</Typography>
        )}
        {packs.map(renderPackRow)}
      </Box>

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
