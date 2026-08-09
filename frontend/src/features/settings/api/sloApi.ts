import { http } from '../../../shared/api/httpClient';

export interface OrgSloSettings {
  availabilityTarget: number;
  latencyTarget: number;
  latencyThresholdSeconds: number;
}

export interface UpdateOrgSloSettingsRequest {
  availabilityTarget: number;
  latencyTarget: number;
  latencyThresholdSeconds: number;
}

export const sloApi = {
  get: (orgId: string) =>
    http.get<OrgSloSettings>(`/organizations/${orgId}/slo`).then((r) => r.data),
  update: (orgId: string, body: UpdateOrgSloSettingsRequest) =>
    http.put<OrgSloSettings>(`/organizations/${orgId}/slo`, body).then((r) => r.data),
};
