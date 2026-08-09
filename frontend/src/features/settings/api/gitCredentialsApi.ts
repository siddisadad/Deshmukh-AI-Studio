import { http } from '../../../shared/api/httpClient';

export interface OrgGitCredential {
  id: string | null;
  provider: string;
  displayName: string;
  configured: boolean;
  apiBaseUrl: string | null;
  enabled: boolean;
  credentialSource: string;
  lastTestedAt: string | null;
  lastTestStatus: string | null;
  lastTestError: string | null;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface UpsertOrgGitCredentialBody {
  displayName: string;
  apiToken?: string;
  apiBaseUrl?: string;
  enabled?: boolean;
}

export interface GitConnectionCheck {
  name: string;
  status: string;
  message: string;
}

export interface GitConnectionTestResult {
  ok: boolean;
  message: string;
  checks: GitConnectionCheck[];
}

export const gitCredentialsApi = {
  list: (orgId: string) =>
    http.get<OrgGitCredential[]>(`/organizations/${orgId}/git-credentials`).then((r) => r.data),
  upsert: (orgId: string, provider: string, body: UpsertOrgGitCredentialBody) =>
    http.put<OrgGitCredential>(`/organizations/${orgId}/git-credentials/${provider}`, body).then((r) => r.data),
  delete: (orgId: string, provider: string) =>
    http.delete(`/organizations/${orgId}/git-credentials/${provider}`).then(() => undefined),
  test: (orgId: string, provider: string, repository?: string, branch?: string) => {
    const params = new URLSearchParams();
    if (repository) params.set('repository', repository);
    if (branch) params.set('branch', branch);
    const query = params.toString();
    return http
      .post<GitConnectionTestResult>(
        `/organizations/${orgId}/git-credentials/${provider}/test${query ? `?${query}` : ''}`
      )
      .then((r) => r.data);
  },
};
