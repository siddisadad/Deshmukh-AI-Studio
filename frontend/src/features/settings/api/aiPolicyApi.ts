import { http } from '../../../shared/api/httpClient';

export interface OrgAiPolicyChange {
  id: string;
  status: string;
  proposedByUserId: string;
  reviewedByUserId: string | null;
  providerChain: string | null;
  dailyTokenBudget: number | null;
  modelMap: string | null;
  deployRegion: string | null;
  previousPolicy: string;
  createdAt: string;
  reviewedAt: string | null;
}

export interface OrgAiPolicy {
  providerChain: string | null;
  dailyTokenBudget: number | null;
  effectiveDailyTokenBudget: number;
  tokensUsedToday: number;
  tokenBudgetRemaining: number | null;
  modelMap: string | null;
  deployRegion: string | null;
  effectiveDeployRegion: string | null;
  changeApprovalRequired: boolean;
  pendingChange: OrgAiPolicyChange | null;
}

export interface UpdateOrgAiPolicyRequest {
  providerChain?: string | null;
  dailyTokenBudget?: number | null;
  modelMap?: string | null;
  deployRegion?: string | null;
}

export const aiPolicyApi = {
  get: (orgId: string) =>
    http.get<OrgAiPolicy>(`/organizations/${orgId}/ai-policy`).then((r) => r.data),
  update: (orgId: string, body: UpdateOrgAiPolicyRequest) =>
    http.put<OrgAiPolicy>(`/organizations/${orgId}/ai-policy`, body).then((r) => r.data),
  listChanges: (orgId: string, limit = 50) =>
    http
      .get<OrgAiPolicyChange[]>(`/organizations/${orgId}/ai-policy/changes`, {
        params: { limit },
      })
      .then((r) => r.data),
  approvePending: (orgId: string) =>
    http
      .post<OrgAiPolicy>(`/organizations/${orgId}/ai-policy/pending/approve`)
      .then((r) => r.data),
  rejectPending: (orgId: string) =>
    http
      .post<OrgAiPolicy>(`/organizations/${orgId}/ai-policy/pending/reject`)
      .then((r) => r.data),
};
