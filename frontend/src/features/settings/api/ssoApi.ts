import { http } from '../../../shared/api/httpClient';

export interface OrgSsoIdp {
  id: string;
  slug: string;
  protocol: string;
  displayName: string;
  enabled: boolean;
  issuerUri: string | null;
  clientId: string | null;
  clientSecretConfigured: boolean;
  scopes: string | null;
  metadataUrl: string | null;
  entityId: string | null;
  acsUrl: string | null;
  spSigningConfigured: boolean;
  wantEncryptedAssertions: boolean;
  metadataFetchedAt: string | null;
  metadataRefreshError: string | null;
}

export interface CreateOrgSsoIdpRequest {
  slug: string;
  protocol: string;
  displayName: string;
  enabled: boolean;
  issuerUri?: string;
  clientId?: string;
  clientSecret?: string;
  scopes?: string;
  metadataUrl?: string;
  entityId?: string;
  acsUrl?: string;
  spPrivateKey?: string;
  spCertificate?: string;
  wantEncryptedAssertions?: boolean;
}

export const ssoApi = {
  list: (orgId: string) =>
    http.get<OrgSsoIdp[]>(`/organizations/${orgId}/sso/idps`).then((r) => r.data),
  create: (orgId: string, body: CreateOrgSsoIdpRequest) =>
    http.post<OrgSsoIdp>(`/organizations/${orgId}/sso/idps`, body).then((r) => r.data),
  refreshMetadata: (orgId: string, idpId: string) =>
    http
      .post<OrgSsoIdp>(`/organizations/${orgId}/sso/idps/${idpId}/refresh-metadata`)
      .then((r) => r.data),
  delete: (orgId: string, idpId: string) =>
    http.delete(`/organizations/${orgId}/sso/idps/${idpId}`).then(() => undefined),
};
