import { http } from '../../../shared/api/httpClient';

export interface OrgMember {
  userId: string;
  email: string;
  displayName: string;
  role: string;
}

export interface OrganizationDetail {
  id: string;
  name: string;
  slug: string;
  role: string;
  createdAt: string;
}

export const organizationsApi = {
  get: (orgId: string) => http.get<OrganizationDetail>(`/organizations/${orgId}`).then((r) => r.data),
  listMembers: (orgId: string) =>
    http.get<OrgMember[]>(`/organizations/${orgId}/members`).then((r) => r.data),
  addMember: (orgId: string, body: { email: string; role: string }) =>
    http.post<OrgMember>(`/organizations/${orgId}/members`, body).then((r) => r.data),
};
