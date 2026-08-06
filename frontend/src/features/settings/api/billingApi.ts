import { http } from '../../../shared/api/httpClient';

export interface Plan {
  code: string;
  name: string;
  priceCentsMonthly: number;
  maxProjects: number;
  maxAiActionsPerDay: number;
  features: string[];
}

export interface BillingOverview {
  plan: Plan;
  subscriptionStatus: string;
  billingProvider: string;
  externalCustomerId: string | null;
  externalSubscriptionId: string | null;
  currentPeriodEnd: string | null;
  activeProjectCount: number;
  maxProjects: number;
  aiActionsUsedToday: number;
  maxAiActionsPerDay: number;
}

export interface CheckoutSession {
  sessionId: string;
  checkoutUrl: string;
  provider: string;
}

export const billingApi = {
  listPlans: () => http.get<Plan[]>('/billing/plans').then((r) => r.data),
  overview: (orgId: string) =>
    http.get<BillingOverview>(`/organizations/${orgId}/billing`).then((r) => r.data),
  changePlan: (orgId: string, planCode: string) =>
    http
      .post<BillingOverview>(`/organizations/${orgId}/billing/change-plan`, { planCode })
      .then((r) => r.data),
  checkout: (orgId: string, planCode: string, successUrl: string, cancelUrl: string) =>
    http
      .post<CheckoutSession>(`/organizations/${orgId}/billing/checkout`, {
        planCode,
        successUrl,
        cancelUrl,
      })
      .then((r) => r.data),
  portal: (orgId: string, returnUrl: string) =>
    http
      .post<{ url: string }>(`/organizations/${orgId}/billing/portal`, null, {
        params: { returnUrl },
      })
      .then((r) => r.data),
};
