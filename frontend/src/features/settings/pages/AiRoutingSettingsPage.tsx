import {
  Alert,
  Box,
  Button,
  LinearProgress,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { ApiError } from '../../../shared/api/types';
import { useAuthStore } from '../../auth/store/authStore';
import { organizationsApi } from '../../projects/api/organizationsApi';
import { aiPolicyApi } from '../api/aiPolicyApi';

function usagePercent(used: number, max: number): number {
  if (max <= 0) {
    return 100;
  }
  return Math.min(100, Math.round((used / max) * 100));
}

export function AiRoutingSettingsPage() {
  const org = useAuthStore((s) => s.organization);
  const queryClient = useQueryClient();
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [providerChain, setProviderChain] = useState('');
  const [dailyTokenBudget, setDailyTokenBudget] = useState('');
  const [modelMap, setModelMap] = useState('');
  const [deployRegion, setDeployRegion] = useState('');

  const policyQuery = useQuery({
    queryKey: ['ai-policy', org?.id],
    queryFn: () => aiPolicyApi.get(org!.id),
    enabled: !!org?.id,
  });

  const changesQuery = useQuery({
    queryKey: ['ai-policy-changes', org?.id],
    queryFn: () => aiPolicyApi.listChanges(org!.id, 20),
    enabled: !!org?.id,
  });

  const orgQuery = useQuery({
    queryKey: ['organization', org?.id],
    queryFn: () => organizationsApi.get(org!.id),
    enabled: !!org?.id,
  });

  const role = orgQuery.data?.role;
  const isOwner = role === 'OWNER';
  const canEdit = role === 'OWNER' || role === 'ADMIN';

  useEffect(() => {
    const policy = policyQuery.data;
    if (!policy) {
      return;
    }
    setProviderChain(policy.providerChain ?? '');
    setDailyTokenBudget(
      policy.dailyTokenBudget != null ? String(policy.dailyTokenBudget) : '',
    );
    setModelMap(policy.modelMap ?? '');
    setDeployRegion(policy.deployRegion ?? '');
  }, [policyQuery.data]);

  async function refreshPolicy() {
    await queryClient.invalidateQueries({ queryKey: ['ai-policy', org?.id] });
    await queryClient.invalidateQueries({ queryKey: ['ai-policy-changes', org?.id] });
  }

  const save = useMutation({
    mutationFn: () =>
      aiPolicyApi.update(org!.id, {
        providerChain: providerChain.trim(),
        dailyTokenBudget: dailyTokenBudget.trim()
          ? Number(dailyTokenBudget)
          : null,
        modelMap: modelMap.trim(),
        deployRegion: deployRegion.trim(),
      }),
    onSuccess: async (policy) => {
      setError(null);
      if (policy.changeApprovalRequired && !isOwner && policy.pendingChange) {
        setMessage('Change submitted for owner approval');
      } else {
        setMessage('AI routing policy saved');
      }
      await queryClient.setQueryData(['ai-policy', org?.id], policy);
      await queryClient.invalidateQueries({ queryKey: ['ai-policy-changes', org?.id] });
    },
    onError: (err) => {
      setMessage(null);
      setError(err instanceof ApiError ? err.message : 'Failed to save AI routing policy');
    },
  });

  const approve = useMutation({
    mutationFn: () => aiPolicyApi.approvePending(org!.id),
    onSuccess: async (policy) => {
      setError(null);
      setMessage('Pending AI routing policy approved');
      await queryClient.setQueryData(['ai-policy', org?.id], policy);
      await refreshPolicy();
    },
    onError: (err) => {
      setMessage(null);
      setError(err instanceof ApiError ? err.message : 'Failed to approve policy change');
    },
  });

  const reject = useMutation({
    mutationFn: () => aiPolicyApi.rejectPending(org!.id),
    onSuccess: async (policy) => {
      setError(null);
      setMessage('Pending AI routing policy rejected');
      await queryClient.setQueryData(['ai-policy', org?.id], policy);
      await refreshPolicy();
    },
    onError: (err) => {
      setMessage(null);
      setError(err instanceof ApiError ? err.message : 'Failed to reject policy change');
    },
  });

  const policy = policyQuery.data;
  const pending = policy?.pendingChange;

  return (
    <Stack spacing={3} sx={{ maxWidth: 720 }} data-testid="ai-routing-settings">
      <Box>
        <Typography variant="h4">AI routing policy</Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
          Configure provider chains, token budgets, model routing, and deploy region overrides for
          your organization.
        </Typography>
      </Box>

      {error && <Alert severity="error">{error}</Alert>}
      {message && <Alert severity="success">{message}</Alert>}
      {policyQuery.isError && (
        <Alert severity="error">
          {policyQuery.error instanceof ApiError
            ? policyQuery.error.message
            : 'Failed to load AI routing policy'}
        </Alert>
      )}

      {pending && (
        <Alert severity="warning" data-testid="ai-policy-pending-banner">
          Pending change: provider chain {pending.providerChain ?? '(unchanged)'}, budget{' '}
          {pending.dailyTokenBudget ?? '(unchanged)'}, region {pending.deployRegion ?? '(unchanged)'}.
          {isOwner && (
            <Stack direction="row" spacing={1} sx={{ mt: 1 }}>
              <Button
                size="small"
                variant="contained"
                onClick={() => approve.mutate()}
                disabled={approve.isPending}
                data-testid="ai-policy-approve"
              >
                Approve
              </Button>
              <Button
                size="small"
                variant="outlined"
                onClick={() => reject.mutate()}
                disabled={reject.isPending}
                data-testid="ai-policy-reject"
              >
                Reject
              </Button>
            </Stack>
          )}
        </Alert>
      )}

      {policy && (
        <Stack spacing={2} data-testid="ai-policy-overview">
          <Typography variant="h6">Token budget (today)</Typography>
          <Box>
            <Typography variant="body2" sx={{ mb: 0.5 }}>
              Tokens used: {policy.tokensUsedToday.toLocaleString()} /{' '}
              {policy.effectiveDailyTokenBudget.toLocaleString()}
              {policy.tokenBudgetRemaining != null
                ? ` (${policy.tokenBudgetRemaining.toLocaleString()} remaining)`
                : ''}
            </Typography>
            <LinearProgress
              variant="determinate"
              value={usagePercent(policy.tokensUsedToday, policy.effectiveDailyTokenBudget)}
            />
          </Box>
          {policy.effectiveDeployRegion && (
            <Typography variant="body2" color="text.secondary">
              Effective deploy region: {policy.effectiveDeployRegion}
            </Typography>
          )}
          {policy.changeApprovalRequired && (
            <Typography variant="caption" color="text.secondary">
              Admin changes require owner approval before they apply.
            </Typography>
          )}
        </Stack>
      )}

      <Stack spacing={2}>
        <TextField
          label="Provider chain"
          placeholder="mock,openai,anthropic"
          value={providerChain}
          onChange={(e) => setProviderChain(e.target.value)}
          disabled={!canEdit}
          helperText="Comma-separated provider ids. Empty clears the org override."
          fullWidth
          slotProps={{ htmlInput: { 'data-testid': 'ai-policy-provider-chain' } }}
        />
        <TextField
          label="Daily token budget override"
          placeholder="500000"
          value={dailyTokenBudget}
          onChange={(e) => setDailyTokenBudget(e.target.value)}
          disabled={!canEdit}
          helperText="Optional org override. Empty uses plan default."
          fullWidth
          slotProps={{ htmlInput: { 'data-testid': 'ai-policy-token-budget' } }}
        />
        <TextField
          label="Model map"
          placeholder="DEVELOPER=openai:gpt-4o-mini,QA_ENGINEER=anthropic:claude-sonnet-4-20250514"
          value={modelMap}
          onChange={(e) => setModelMap(e.target.value)}
          disabled={!canEdit}
          helperText="ASSISTANT_ROLE=provider:model pairs, comma-separated."
          fullWidth
          slotProps={{ htmlInput: { 'data-testid': 'ai-policy-model-map' } }}
        />
        <TextField
          label="Deploy region override"
          placeholder="eu-west"
          value={deployRegion}
          onChange={(e) => setDeployRegion(e.target.value)}
          disabled={!canEdit}
          helperText="Overrides platform AISTUDIO_DEPLOY_REGION for cross-region routing."
          fullWidth
          slotProps={{ htmlInput: { 'data-testid': 'ai-policy-deploy-region' } }}
        />
        {canEdit ? (
          <Button
            variant="contained"
            onClick={() => save.mutate()}
            disabled={save.isPending || !org?.id}
            data-testid="ai-policy-save"
          >
            {save.isPending ? 'Saving…' : 'Save policy'}
          </Button>
        ) : (
          <Alert severity="info">Only organization owners and admins can edit AI routing policy.</Alert>
        )}
      </Stack>

      {changesQuery.data && changesQuery.data.length > 0 && (
        <Stack spacing={1} data-testid="ai-policy-change-history">
          <Typography variant="h6">Change history</Typography>
          {changesQuery.data.map((change) => (
            <Box key={change.id} sx={{ py: 1, borderTop: 1, borderColor: 'divider' }}>
              <Typography variant="body2">
                {change.status} · {new Date(change.createdAt).toLocaleString()}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                chain={change.providerChain ?? '—'} · budget={change.dailyTokenBudget ?? '—'} ·
                region={change.deployRegion ?? '—'}
              </Typography>
            </Box>
          ))}
        </Stack>
      )}
    </Stack>
  );
}
