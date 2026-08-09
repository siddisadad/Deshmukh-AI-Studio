import {
  Alert,
  Box,
  Button,
  Checkbox,
  FormControlLabel,
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
  const [canaryProviderChain, setCanaryProviderChain] = useState('');
  const [canaryPercent, setCanaryPercent] = useState('10');
  const [canaryAutoPromoteEnabled, setCanaryAutoPromoteEnabled] = useState(false);
  const [canaryAutoAbortEnabled, setCanaryAutoAbortEnabled] = useState(false);
  const [canaryHookWebhookUrl, setCanaryHookWebhookUrl] = useState('');
  const [canaryMinSamples, setCanaryMinSamples] = useState('20');
  const [canaryAbortErrorRatePercent, setCanaryAbortErrorRatePercent] = useState('25');
  const [canaryPromoteMinSamples, setCanaryPromoteMinSamples] = useState('50');
  const [canaryPromoteMaxErrorRatePercent, setCanaryPromoteMaxErrorRatePercent] = useState('5');
  const [preview, setPreview] = useState<Awaited<ReturnType<typeof aiPolicyApi.simulate>> | null>(
    null,
  );

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

  const simulationsQuery = useQuery({
    queryKey: ['ai-policy-simulations', org?.id],
    queryFn: () => aiPolicyApi.listSimulations(org!.id, 20),
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
    setCanaryProviderChain(policy.canaryProviderChain ?? '');
    setCanaryPercent(
      policy.canaryPercent != null ? String(policy.canaryPercent) : '10',
    );
    setCanaryAutoPromoteEnabled(policy.canaryAutoPromoteEnabled);
    setCanaryAutoAbortEnabled(policy.canaryAutoAbortEnabled);
    setCanaryHookWebhookUrl(policy.canaryHookWebhookUrl ?? '');
    setCanaryMinSamples(String(policy.canaryMinSamples));
    setCanaryAbortErrorRatePercent(String(policy.canaryAbortErrorRatePercent));
    setCanaryPromoteMinSamples(String(policy.canaryPromoteMinSamples));
    setCanaryPromoteMaxErrorRatePercent(String(policy.canaryPromoteMaxErrorRatePercent));
    setPreview(null);
  }, [policyQuery.data]);

  function clearPreview() {
    setPreview(null);
  }

  async function refreshPolicy() {
    await queryClient.invalidateQueries({ queryKey: ['ai-policy', org?.id] });
    await queryClient.invalidateQueries({ queryKey: ['ai-policy-changes', org?.id] });
    await queryClient.invalidateQueries({ queryKey: ['ai-policy-simulations', org?.id] });
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
        simulationId: preview?.simulationId ?? null,
      }),
    onSuccess: async (policy) => {
      setError(null);
      setPreview(null);
      if (policy.changeApprovalRequired && !isOwner && policy.pendingChange) {
        setMessage('Change submitted for owner approval');
      } else {
        setMessage('AI routing policy saved');
      }
      await queryClient.setQueryData(['ai-policy', org?.id], policy);
      await queryClient.invalidateQueries({ queryKey: ['ai-policy-changes', org?.id] });
      await queryClient.invalidateQueries({ queryKey: ['ai-policy-simulations', org?.id] });
    },
    onError: (err) => {
      setMessage(null);
      setError(err instanceof ApiError ? err.message : 'Failed to save AI routing policy');
    },
  });

  const simulate = useMutation({
    mutationFn: () =>
      aiPolicyApi.simulate(org!.id, {
        providerChain: providerChain.trim(),
        dailyTokenBudget: dailyTokenBudget.trim() ? Number(dailyTokenBudget) : null,
        modelMap: modelMap.trim(),
        deployRegion: deployRegion.trim(),
      }),
    onSuccess: (result) => {
      setError(null);
      setMessage(null);
      setPreview(result);
      queryClient.invalidateQueries({ queryKey: ['ai-policy-simulations', org?.id] });
    },
    onError: (err) => {
      setPreview(null);
      setError(err instanceof ApiError ? err.message : 'Failed to preview AI routing policy');
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

  const updateCanary = useMutation({
    mutationFn: () =>
      aiPolicyApi.updateCanary(org!.id, {
        providerChain: canaryProviderChain.trim(),
        percent: Number(canaryPercent),
      }),
    onSuccess: async (policy) => {
      setError(null);
      setMessage('Canary rollout updated');
      await queryClient.setQueryData(['ai-policy', org?.id], policy);
    },
    onError: (err) => {
      setMessage(null);
      setError(err instanceof ApiError ? err.message : 'Failed to update canary rollout');
    },
  });

  const promoteCanary = useMutation({
    mutationFn: () => aiPolicyApi.promoteCanary(org!.id),
    onSuccess: async (policy) => {
      setError(null);
      setMessage('Canary provider chain promoted to stable policy');
      await queryClient.setQueryData(['ai-policy', org?.id], policy);
    },
    onError: (err) => {
      setMessage(null);
      setError(err instanceof ApiError ? err.message : 'Failed to promote canary');
    },
  });

  const abortCanary = useMutation({
    mutationFn: () => aiPolicyApi.abortCanary(org!.id),
    onSuccess: async (policy) => {
      setError(null);
      setMessage('Canary rollout aborted');
      await queryClient.setQueryData(['ai-policy', org?.id], policy);
    },
    onError: (err) => {
      setMessage(null);
      setError(err instanceof ApiError ? err.message : 'Failed to abort canary');
    },
  });

  const updateCanaryHooks = useMutation({
    mutationFn: () =>
      aiPolicyApi.updateCanaryHooks(org!.id, {
        autoPromoteEnabled: canaryAutoPromoteEnabled,
        autoAbortEnabled: canaryAutoAbortEnabled,
        hookWebhookUrl: canaryHookWebhookUrl.trim() || null,
        minSamples: Number(canaryMinSamples),
        abortErrorRatePercent: Number(canaryAbortErrorRatePercent),
        promoteMinSamples: Number(canaryPromoteMinSamples),
        promoteMaxErrorRatePercent: Number(canaryPromoteMaxErrorRatePercent),
      }),
    onSuccess: async (policy) => {
      setError(null);
      setMessage('Canary automation hooks saved');
      await queryClient.setQueryData(['ai-policy', org?.id], policy);
    },
    onError: (err) => {
      setMessage(null);
      setError(err instanceof ApiError ? err.message : 'Failed to save canary hooks');
    },
  });

  const evaluateCanaryHooks = useMutation({
    mutationFn: () => aiPolicyApi.evaluateCanaryHooks(org!.id),
    onSuccess: async (result) => {
      setError(null);
      if (result.action === 'PROMOTED') {
        setMessage(`Canary auto-promoted: ${result.reason}`);
      } else if (result.action === 'ABORTED') {
        setMessage(`Canary auto-aborted: ${result.reason}`);
      } else {
        setMessage(`Canary evaluation: ${result.reason}`);
      }
      await refreshPolicy();
    },
    onError: (err) => {
      setMessage(null);
      setError(err instanceof ApiError ? err.message : 'Failed to evaluate canary hooks');
    },
  });

  const policy = policyQuery.data;
  const pending = policy?.pendingChange;
  const saveRequiresGate = policy?.simulationGateEnabled && !preview?.gatePassed;

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
          {policy.simulationGateEnabled && (
            <Typography variant="caption" color="text.secondary" data-testid="ai-policy-gate-note">
              Simulation gate enabled: preview changes and pass rollout gates before saving.
            </Typography>
          )}
        </Stack>
      )}

      <Stack spacing={2}>
        <TextField
          label="Provider chain"
          placeholder="mock,openai,anthropic"
          value={providerChain}
          onChange={(e) => {
            setProviderChain(e.target.value);
            clearPreview();
          }}
          disabled={!canEdit}
          helperText="Comma-separated provider ids. Empty clears the org override."
          fullWidth
          slotProps={{ htmlInput: { 'data-testid': 'ai-policy-provider-chain' } }}
        />
        <TextField
          label="Daily token budget override"
          placeholder="500000"
          value={dailyTokenBudget}
          onChange={(e) => {
            setDailyTokenBudget(e.target.value);
            clearPreview();
          }}
          disabled={!canEdit}
          helperText="Optional org override. Empty uses plan default."
          fullWidth
          slotProps={{ htmlInput: { 'data-testid': 'ai-policy-token-budget' } }}
        />
        <TextField
          label="Model map"
          placeholder="DEVELOPER=openai:gpt-4o-mini,QA_ENGINEER=anthropic:claude-sonnet-4-20250514"
          value={modelMap}
          onChange={(e) => {
            setModelMap(e.target.value);
            clearPreview();
          }}
          disabled={!canEdit}
          helperText="ASSISTANT_ROLE=provider:model pairs, comma-separated."
          fullWidth
          slotProps={{ htmlInput: { 'data-testid': 'ai-policy-model-map' } }}
        />
        <TextField
          label="Deploy region override"
          placeholder="eu-west"
          value={deployRegion}
          onChange={(e) => {
            setDeployRegion(e.target.value);
            clearPreview();
          }}
          disabled={!canEdit}
          helperText="Overrides platform AISTUDIO_DEPLOY_REGION for cross-region routing."
          fullWidth
          slotProps={{ htmlInput: { 'data-testid': 'ai-policy-deploy-region' } }}
        />
        {canEdit ? (
          <Stack direction="row" spacing={1}>
            <Button
              variant="outlined"
              onClick={() => simulate.mutate()}
              disabled={simulate.isPending || !org?.id}
              data-testid="ai-policy-preview"
            >
              {simulate.isPending ? 'Previewing…' : 'Preview changes'}
            </Button>
            <Button
              variant="contained"
              onClick={() => save.mutate()}
              disabled={save.isPending || !org?.id || saveRequiresGate}
              data-testid="ai-policy-save"
            >
              {save.isPending ? 'Saving…' : 'Save policy'}
            </Button>
          </Stack>
        ) : (
          <Alert severity="info">Only organization owners and admins can edit AI routing policy.</Alert>
        )}
      </Stack>

      {canEdit && (
        <Stack spacing={2} data-testid="ai-policy-canary">
          <Typography variant="h6">Canary rollout</Typography>
          <Typography variant="body2" color="text.secondary">
            Route a percentage of conversation traffic to a candidate provider chain before full
            promotion. Selection is sticky per conversation thread.
          </Typography>
          {policy?.canaryProviderChain && policy.canaryPercent != null && (
            <Alert severity="info">
              Active canary: {policy.canaryProviderChain} at {policy.canaryPercent}% of threads.
            </Alert>
          )}
          <TextField
            label="Canary provider chain"
            placeholder="mock,openai"
            value={canaryProviderChain}
            onChange={(e) => setCanaryProviderChain(e.target.value)}
            fullWidth
            slotProps={{ htmlInput: { 'data-testid': 'ai-policy-canary-chain' } }}
          />
          <TextField
            label="Canary traffic percent"
            placeholder="10"
            value={canaryPercent}
            onChange={(e) => setCanaryPercent(e.target.value)}
            helperText="1–100 percent of conversation threads use the canary chain."
            fullWidth
            slotProps={{ htmlInput: { 'data-testid': 'ai-policy-canary-percent' } }}
          />
          <Stack direction="row" spacing={1}>
            <Button
              variant="outlined"
              onClick={() => updateCanary.mutate()}
              disabled={updateCanary.isPending || !org?.id}
              data-testid="ai-policy-canary-start"
            >
              {updateCanary.isPending ? 'Updating…' : 'Start / update canary'}
            </Button>
            {policy?.canaryProviderChain && (
              <>
                <Button
                  variant="contained"
                  onClick={() => promoteCanary.mutate()}
                  disabled={promoteCanary.isPending}
                  data-testid="ai-policy-canary-promote"
                >
                  Promote
                </Button>
                <Button
                  variant="outlined"
                  color="warning"
                  onClick={() => abortCanary.mutate()}
                  disabled={abortCanary.isPending}
                  data-testid="ai-policy-canary-abort"
                >
                  Abort
                </Button>
              </>
            )}
          </Stack>
        </Stack>
      )}

      {canEdit && (
        <Stack spacing={2} data-testid="ai-policy-canary-hooks">
          <Typography variant="h6">Canary automation hooks</Typography>
          <Typography variant="body2" color="text.secondary">
            Automatically promote or abort canary rollouts when chat success/failure metrics cross
            configured thresholds. A background scheduler evaluates hooks when enabled.
          </Typography>
          {policy?.canaryMetrics && (
            <Typography variant="body2" data-testid="ai-policy-canary-metrics">
              Canary outcomes: {policy.canaryMetrics.canarySuccessCount} ok /{' '}
              {policy.canaryMetrics.canaryFailureCount} fail — stable:{' '}
              {policy.canaryMetrics.stableSuccessCount} ok /{' '}
              {policy.canaryMetrics.stableFailureCount} fail
            </Typography>
          )}
          <FormControlLabel
            control={
              <Checkbox
                checked={canaryAutoPromoteEnabled}
                onChange={(e) => setCanaryAutoPromoteEnabled(e.target.checked)}
                data-testid="ai-policy-canary-auto-promote"
              />
            }
            label="Auto-promote when canary error rate is within threshold"
          />
          <FormControlLabel
            control={
              <Checkbox
                checked={canaryAutoAbortEnabled}
                onChange={(e) => setCanaryAutoAbortEnabled(e.target.checked)}
                data-testid="ai-policy-canary-auto-abort"
              />
            }
            label="Auto-abort when canary error rate exceeds threshold"
          />
          <TextField
            label="Hook webhook URL"
            placeholder="https://hooks.example.com/canary"
            value={canaryHookWebhookUrl}
            onChange={(e) => setCanaryHookWebhookUrl(e.target.value)}
            fullWidth
            slotProps={{ htmlInput: { 'data-testid': 'ai-policy-canary-hook-webhook' } }}
          />
          <TextField
            label="Abort min samples"
            value={canaryMinSamples}
            onChange={(e) => setCanaryMinSamples(e.target.value)}
            helperText="Minimum canary outcomes before auto-abort can fire."
            fullWidth
            slotProps={{ htmlInput: { 'data-testid': 'ai-policy-canary-min-samples' } }}
          />
          <TextField
            label="Abort error rate percent"
            value={canaryAbortErrorRatePercent}
            onChange={(e) => setCanaryAbortErrorRatePercent(e.target.value)}
            fullWidth
            slotProps={{ htmlInput: { 'data-testid': 'ai-policy-canary-abort-rate' } }}
          />
          <TextField
            label="Promote min samples"
            value={canaryPromoteMinSamples}
            onChange={(e) => setCanaryPromoteMinSamples(e.target.value)}
            fullWidth
            slotProps={{ htmlInput: { 'data-testid': 'ai-policy-canary-promote-samples' } }}
          />
          <TextField
            label="Promote max error rate percent"
            value={canaryPromoteMaxErrorRatePercent}
            onChange={(e) => setCanaryPromoteMaxErrorRatePercent(e.target.value)}
            fullWidth
            slotProps={{ htmlInput: { 'data-testid': 'ai-policy-canary-promote-rate' } }}
          />
          <Stack direction="row" spacing={1}>
            <Button
              variant="outlined"
              onClick={() => updateCanaryHooks.mutate()}
              disabled={updateCanaryHooks.isPending || !org?.id}
              data-testid="ai-policy-canary-hooks-save"
            >
              {updateCanaryHooks.isPending ? 'Saving…' : 'Save hook settings'}
            </Button>
            <Button
              variant="contained"
              onClick={() => evaluateCanaryHooks.mutate()}
              disabled={evaluateCanaryHooks.isPending || !org?.id}
              data-testid="ai-policy-canary-evaluate"
            >
              {evaluateCanaryHooks.isPending ? 'Evaluating…' : 'Evaluate now'}
            </Button>
          </Stack>
        </Stack>
      )}

      {preview && (
        <Stack spacing={1} data-testid="ai-policy-preview-panel">
          <Typography variant="h6">Policy preview (dry-run)</Typography>
          {preview.gatePassed ? (
            <Alert severity="success">Rollout gate passed — ready to save.</Alert>
          ) : (
            <Alert severity="error">Rollout gate failed — fix missing providers before saving.</Alert>
          )}
          {preview.wouldRequireApproval && (
            <Alert severity="info">
              This change would require owner approval before it applies.
            </Alert>
          )}
          {preview.missingProviders.length > 0 && (
            <Alert severity="warning">
              Missing providers in simulated chain: {preview.missingProviders.join(', ')}
            </Alert>
          )}
          <Typography variant="body2">
            Current effective chain: {preview.currentEffectiveProviderChain.join(' → ') || '—'}
          </Typography>
          <Typography variant="body2">
            Simulated effective chain:{' '}
            {preview.simulatedEffectiveProviderChain.join(' → ') || '—'}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            Simulated budget: {preview.simulated.effectiveDailyTokenBudget.toLocaleString()} tokens
            ({preview.simulated.tokenBudgetRemaining?.toLocaleString() ?? '—'} remaining today)
          </Typography>
          {preview.simulated.effectiveDeployRegion && (
            <Typography variant="caption" color="text.secondary">
              Simulated deploy region: {preview.simulated.effectiveDeployRegion}
            </Typography>
          )}
        </Stack>
      )}

      {simulationsQuery.data && simulationsQuery.data.length > 0 && (
        <Stack spacing={1} data-testid="ai-policy-simulation-history">
          <Typography variant="h6">Simulation history</Typography>
          {simulationsQuery.data.map((simulation) => (
            <Box key={simulation.id} sx={{ py: 1, borderTop: 1, borderColor: 'divider' }}>
              <Typography variant="body2">
                {simulation.gatePassed ? 'PASSED' : 'FAILED'} ·{' '}
                {new Date(simulation.createdAt).toLocaleString()}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                chain={simulation.providerChain ?? '—'} · budget={simulation.dailyTokenBudget ?? '—'}{' '}
                · region={simulation.deployRegion ?? '—'}
                {simulation.appliedChangeId ? ' · applied' : ''}
              </Typography>
            </Box>
          ))}
        </Stack>
      )}

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
