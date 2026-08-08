import { http } from '../../../shared/api/httpClient';

export interface Plan {
  code: string;
  name: string;
  priceCentsMonthly: number;
  maxProjects: number;
  maxAiActionsPerDay: number;
  maxSeats: number;
  priceCentsPerSeatMonthly: number;
  priceCentsPerAiActionOverage: number;
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
  activeMemberCount: number;
  maxSeats: number;
  aiActionsUsedToday: number;
  aiActionsOverageToday: number;
  maxAiActionsPerDay: number;
  periodOverageActions: number;
  estimatedSeatCentsMonthly: number;
  estimatedOverageCentsThisPeriod: number;
}

export interface CheckoutSession {
  sessionId: string;
  checkoutUrl: string;
  provider: string;
}

export interface UsageDay {
  date: string;
  actionCount: number;
  overageCount: number;
}

export interface Invoice {
  id: string;
  number: string | null;
  status: string;
  amountDueCents: number;
  currency: string;
  createdAt: string | null;
  hostedInvoiceUrl: string | null;
  invoicePdfUrl: string | null;
}

export const billingApi = {
  listPlans: () => http.get<Plan[]>('/billing/plans').then((r) => r.data),
  overview: (orgId: string) =>
    http.get<BillingOverview>(`/organizations/${orgId}/billing`).then((r) => r.data),
  usageHistory: (orgId: string, days = 30) =>
    http
      .get<UsageDay[]>(`/organizations/${orgId}/billing/usage`, { params: { days } })
      .then((r) => r.data),
  listInvoices: (orgId: string, limit = 12) =>
    http
      .get<Invoice[]>(`/organizations/${orgId}/billing/invoices`, { params: { limit } })
      .then((r) => r.data),
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
