import { http } from '../../../shared/api/httpClient';

export interface OrgDlpConnector {
  id: string;
  slug: string;
  connectorType: string;
  displayName: string;
  webhookUrl: string;
  enabled: boolean;
  blockOnMatch: boolean;
  customPatternsJson: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface DlpEvent {
  id: string;
  projectId: string;
  conversationId: string | null;
  exportId: string;
  exportedByUserId: string;
  matchCategories: string;
  blocked: boolean;
  siemExportedAt: string | null;
  createdAt: string;
}

export interface CreateDlpConnectorRequest {
  slug: string;
  connectorType: string;
  displayName: string;
  webhookUrl: string;
  enabled: boolean;
  blockOnMatch: boolean;
  customPatternsJson?: string;
}

export const dlpApi = {
  listConnectors: (orgId: string) =>
    http.get<OrgDlpConnector[]>(`/organizations/${orgId}/dlp/connectors`).then((r) => r.data),
  createConnector: (orgId: string, body: CreateDlpConnectorRequest) =>
    http.post<OrgDlpConnector>(`/organizations/${orgId}/dlp/connectors`, body).then((r) => r.data),
  deleteConnector: (orgId: string, connectorId: string) =>
    http.delete(`/organizations/${orgId}/dlp/connectors/${connectorId}`).then(() => undefined),
  listEvents: (orgId: string) =>
    http.get<DlpEvent[]>(`/organizations/${orgId}/dlp/events`).then((r) => r.data),
};
