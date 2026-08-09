import { http } from '../../../shared/api/httpClient';

export interface OrgAiPolicy {
  providerChain: string | null;
  dailyTokenBudget: number | null;
  effectiveDailyTokenBudget: number;
  tokensUsedToday: number;
  tokenBudgetRemaining: number | null;
  modelMap: string | null;
  deployRegion: string | null;
  effectiveDeployRegion: string | null;
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
};
