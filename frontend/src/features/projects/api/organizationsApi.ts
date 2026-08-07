import { http } from '../../../shared/api/httpClient';

export interface OrgMember {
  userId: string;
  email: string;
  displayName: string;
  role: string;
}

export const organizationsApi = {
  listMembers: (orgId: string) =>
    http.get<OrgMember[]>(`/organizations/${orgId}/members`).then((r) => r.data),
};
