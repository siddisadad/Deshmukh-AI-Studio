import {
  Alert,
  Box,
  Button,
  LinearProgress,
  Stack,
  Typography,
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { ApiError } from '../../../shared/api/types';
import { useAuthStore } from '../../auth/store/authStore';
import { billingApi, type Plan } from '../api/billingApi';

function formatPrice(cents: number): string {
  if (cents <= 0) {
    return 'Free';
  }
  return `$${(cents / 100).toFixed(0)}/mo`;
}

function usagePercent(used: number, max: number): number {
  if (max <= 0) {
    return 100;
  }
  return Math.min(100, Math.round((used / max) * 100));
}

export function BillingSettingsPage() {
  const org = useAuthStore((s) => s.organization);
  const queryClient = useQueryClient();
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const overviewQuery = useQuery({
    queryKey: ['billing', org?.id],
    queryFn: () => billingApi.overview(org!.id),
    enabled: !!org?.id,
  });

  const plansQuery = useQuery({
    queryKey: ['billing-plans'],
    queryFn: () => billingApi.listPlans(),
  });

  const changePlan = useMutation({
    mutationFn: (planCode: string) => billingApi.changePlan(org!.id, planCode),
    onSuccess: async (overview) => {
      setError(null);
      setMessage(`Plan updated to ${overview.plan.name}`);
      await queryClient.invalidateQueries({ queryKey: ['billing', org?.id] });
    },
    onError: (err) => {
      setMessage(null);
      setError(err instanceof ApiError ? err.message : 'Failed to change plan');
    },
  });

  const checkout = useMutation({
    mutationFn: (planCode: string) => {
      const returnUrl = `${window.location.origin}/settings/billing`;
      return billingApi.checkout(org!.id, planCode, returnUrl, returnUrl);
    },
    onSuccess: (session) => {
      setError(null);
      setMessage(`Mock checkout ready (${session.sessionId}). Completing via change-plan…`);
      const params = new URL(session.checkoutUrl).searchParams;
      const plan = params.get('plan');
      if (plan) {
        changePlan.mutate(plan);
      }
    },
    onError: (err) => {
      setMessage(null);
      setError(err instanceof ApiError ? err.message : 'Checkout failed');
    },
  });

  const overview = overviewQuery.data;
  const currentCode = overview?.plan.code;

  const sortedPlans = useMemo(() => {
    const list = plansQuery.data;
    if (!list) {
      return [];
    }
    return [...list].sort((a, b) => a.priceCentsMonthly - b.priceCentsMonthly);
  }, [plansQuery.data]);

  function onSelectPlan(plan: Plan) {
    setError(null);
    setMessage(null);
    if (plan.code === 'FREE' || plan.code === currentCode) {
      changePlan.mutate(plan.code);
      return;
    }
    checkout.mutate(plan.code);
  }

  return (
    <Stack spacing={3} sx={{ maxWidth: 720 }}>
      <Box>
        <Typography variant="h4">Billing & plans</Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
          Manage your organization plan, project limits, and daily AI usage.
        </Typography>
      </Box>

      {error && <Alert severity="error">{error}</Alert>}
      {message && <Alert severity="success">{message}</Alert>}
      {overviewQuery.isError && (
        <Alert severity="error">
          {overviewQuery.error instanceof ApiError
            ? overviewQuery.error.message
            : 'Failed to load billing'}
        </Alert>
      )}

      {overview && (
        <Stack spacing={2} data-testid="billing-overview">
          <Typography variant="h6" data-testid="billing-current-plan">
            Current plan: {overview.plan.name}{' '}
            <Typography component="span" variant="body2" color="text.secondary">
              ({overview.subscriptionStatus} · {overview.billingProvider})
            </Typography>
          </Typography>

          <Box>
            <Typography variant="body2" sx={{ mb: 0.5 }}>
              Active projects: {overview.activeProjectCount} / {overview.maxProjects}
            </Typography>
            <LinearProgress
              variant="determinate"
              value={usagePercent(overview.activeProjectCount, overview.maxProjects)}
            />
          </Box>

          <Box>
            <Typography variant="body2" sx={{ mb: 0.5 }}>
              AI actions today: {overview.aiActionsUsedToday} / {overview.maxAiActionsPerDay}
            </Typography>
            <LinearProgress
              variant="determinate"
              color={
                overview.aiActionsUsedToday >= overview.maxAiActionsPerDay ? 'warning' : 'primary'
              }
              value={usagePercent(overview.aiActionsUsedToday, overview.maxAiActionsPerDay)}
            />
          </Box>
        </Stack>
      )}

      <Stack spacing={2}>
        <Typography variant="h6">Available plans</Typography>
        {sortedPlans.map((plan) => {
          const isCurrent = plan.code === currentCode;
          return (
            <Box
              key={plan.code}
              sx={{
                py: 2,
                borderTop: 1,
                borderColor: 'divider',
              }}
            >
              <Stack
                direction={{ xs: 'column', sm: 'row' }}
                spacing={2}
                sx={{ alignItems: { sm: 'center' }, justifyContent: 'space-between' }}
              >
                <Box>
                  <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                    {plan.name} · {formatPrice(plan.priceCentsMonthly)}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {plan.maxProjects} projects · {plan.maxAiActionsPerDay} AI actions/day
                    {plan.features.length > 0 ? ` · ${plan.features.join(', ')}` : ''}
                  </Typography>
                </Box>
                <Button
                  variant={isCurrent ? 'outlined' : 'contained'}
                  disabled={
                    isCurrent || changePlan.isPending || checkout.isPending || !org?.id
                  }
                  onClick={() => onSelectPlan(plan)}
                  data-testid={`billing-plan-${plan.code}`}
                >
                  {isCurrent ? 'Current plan' : plan.code === 'FREE' ? 'Downgrade' : 'Upgrade'}
                </Button>
              </Stack>
            </Box>
          );
        })}
      </Stack>
    </Stack>
  );
}
