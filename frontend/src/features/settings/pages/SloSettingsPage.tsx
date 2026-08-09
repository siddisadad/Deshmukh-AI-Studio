import { Alert, Box, Button, Stack, TextField, Typography } from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { ApiError } from '../../../shared/api/types';
import { useAuthStore } from '../../auth/store/authStore';
import { organizationsApi } from '../../projects/api/organizationsApi';
import { sloApi } from '../api/sloApi';

export function SloSettingsPage() {
  const org = useAuthStore((s) => s.organization);
  const queryClient = useQueryClient();
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [availabilityTarget, setAvailabilityTarget] = useState('0.995');
  const [latencyTarget, setLatencyTarget] = useState('0.95');
  const [latencyThresholdSeconds, setLatencyThresholdSeconds] = useState('2');

  const orgQuery = useQuery({
    queryKey: ['organization', org?.id],
    queryFn: () => organizationsApi.get(org!.id),
    enabled: !!org?.id,
  });

  const sloQuery = useQuery({
    queryKey: ['org-slo', org?.id],
    queryFn: () => sloApi.get(org!.id),
    enabled: !!org?.id,
  });

  const isOwner = orgQuery.data?.role === 'OWNER';

  useEffect(() => {
    const settings = sloQuery.data;
    if (!settings) {
      return;
    }
    setAvailabilityTarget(String(settings.availabilityTarget));
    setLatencyTarget(String(settings.latencyTarget));
    setLatencyThresholdSeconds(String(settings.latencyThresholdSeconds));
  }, [sloQuery.data]);

  const save = useMutation({
    mutationFn: () =>
      sloApi.update(org!.id, {
        availabilityTarget: Number(availabilityTarget),
        latencyTarget: Number(latencyTarget),
        latencyThresholdSeconds: Number(latencyThresholdSeconds),
      }),
    onSuccess: async (settings) => {
      setError(null);
      setMessage('SLO targets saved');
      await queryClient.setQueryData(['org-slo', org?.id], settings);
    },
    onError: (err) => {
      setMessage(null);
      setError(err instanceof ApiError ? err.message : 'Failed to save SLO targets');
    },
  });

  return (
    <Stack spacing={3} sx={{ maxWidth: 720 }} data-testid="slo-settings">
      <Box>
        <Typography variant="h4">SLO targets</Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
          Configure per-organization availability and latency SLO targets used for tenant error
          budgets and multi-window burn-rate alerts in Prometheus.
        </Typography>
      </Box>

      {error && <Alert severity="error">{error}</Alert>}
      {message && <Alert severity="success">{message}</Alert>}
      {sloQuery.isError && (
        <Alert severity="error">
          {sloQuery.error instanceof ApiError
            ? sloQuery.error.message
            : 'Failed to load SLO settings'}
        </Alert>
      )}

      {isOwner ? (
        <Stack spacing={2}>
          <TextField
            label="Availability target (0.9–0.999)"
            value={availabilityTarget}
            onChange={(e) => setAvailabilityTarget(e.target.value)}
            fullWidth
            slotProps={{ htmlInput: { 'data-testid': 'slo-availability-target' } }}
          />
          <TextField
            label="Latency target (0.5–0.99)"
            value={latencyTarget}
            onChange={(e) => setLatencyTarget(e.target.value)}
            helperText="Fraction of requests under the latency threshold."
            fullWidth
            slotProps={{ htmlInput: { 'data-testid': 'slo-latency-target' } }}
          />
          <TextField
            label="Latency threshold (seconds)"
            value={latencyThresholdSeconds}
            onChange={(e) => setLatencyThresholdSeconds(e.target.value)}
            helperText="Prometheus SLI recording uses the 2s histogram bucket; threshold is exported for targets."
            fullWidth
            slotProps={{ htmlInput: { 'data-testid': 'slo-latency-threshold' } }}
          />
          <Button
            variant="contained"
            onClick={() => save.mutate()}
            disabled={save.isPending || !org?.id}
            data-testid="slo-save"
          >
            {save.isPending ? 'Saving…' : 'Save SLO targets'}
          </Button>
        </Stack>
      ) : (
        <Alert severity="info">Only organization owners can edit SLO targets.</Alert>
      )}
    </Stack>
  );
}
