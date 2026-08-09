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

  const orgQuery = useQuery({
    queryKey: ['organization', org?.id],
    queryFn: () => organizationsApi.get(org!.id),
    enabled: !!org?.id,
  });

  const isOwner = orgQuery.data?.role === 'OWNER';

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
      setMessage('AI routing policy saved');
      await queryClient.setQueryData(['ai-policy', org?.id], policy);
    },
    onError: (err) => {
      setMessage(null);
      setError(err instanceof ApiError ? err.message : 'Failed to save AI routing policy');
    },
  });

  const policy = policyQuery.data;

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
        </Stack>
      )}

      <Stack spacing={2}>
        <TextField
          label="Provider chain"
          placeholder="mock,openai,anthropic"
          value={providerChain}
          onChange={(e) => setProviderChain(e.target.value)}
          disabled={!isOwner}
          helperText="Comma-separated provider ids. Empty clears the org override."
          fullWidth
          slotProps={{ htmlInput: { 'data-testid': 'ai-policy-provider-chain' } }}
        />
        <TextField
          label="Daily token budget override"
          placeholder="500000"
          value={dailyTokenBudget}
          onChange={(e) => setDailyTokenBudget(e.target.value)}
          disabled={!isOwner}
          helperText="Optional org override. Empty uses plan default."
          fullWidth
          slotProps={{ htmlInput: { 'data-testid': 'ai-policy-token-budget' } }}
        />
        <TextField
          label="Model map"
          placeholder="DEVELOPER=openai:gpt-4o-mini,QA_ENGINEER=anthropic:claude-sonnet-4-20250514"
          value={modelMap}
          onChange={(e) => setModelMap(e.target.value)}
          disabled={!isOwner}
          helperText="ASSISTANT_ROLE=provider:model pairs, comma-separated."
          fullWidth
          slotProps={{ htmlInput: { 'data-testid': 'ai-policy-model-map' } }}
        />
        <TextField
          label="Deploy region override"
          placeholder="eu-west"
          value={deployRegion}
          onChange={(e) => setDeployRegion(e.target.value)}
          disabled={!isOwner}
          helperText="Overrides platform AISTUDIO_DEPLOY_REGION for cross-region routing."
          fullWidth
          slotProps={{ htmlInput: { 'data-testid': 'ai-policy-deploy-region' } }}
        />
        {isOwner ? (
          <Button
            variant="contained"
            onClick={() => save.mutate()}
            disabled={save.isPending || !org?.id}
            data-testid="ai-policy-save"
          >
            {save.isPending ? 'Saving…' : 'Save policy'}
          </Button>
        ) : (
          <Alert severity="info">Only organization owners can edit AI routing policy.</Alert>
        )}
      </Stack>
    </Stack>
  );
}
