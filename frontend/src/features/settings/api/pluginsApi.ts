import { http } from '../../../shared/api/httpClient';

export interface Plugin {
  id: string;
  name: string;
  version: string;
  type: 'ASSISTANT' | 'TOOL' | string;
  description: string;
  builtin: boolean;
}

export interface OrgPlugin {
  plugin: Plugin;
  enabled: boolean;
  canDisable: boolean;
}

export interface InvokeToolResult {
  toolId: string;
  toolName: string;
  success: boolean;
  output: string;
  metadata: Record<string, unknown>;
}

export const pluginsApi = {
  catalog: () => http.get<Plugin[]>('/plugins').then((r) => r.data),
  listOrg: (orgId: string) =>
    http.get<OrgPlugin[]>(`/organizations/${orgId}/plugins`).then((r) => r.data),
  setEnabled: (orgId: string, pluginId: string, enabled: boolean) =>
    http
      .put<OrgPlugin>(`/organizations/${orgId}/plugins/${encodeURIComponent(pluginId)}`, { enabled })
      .then((r) => r.data),
  invokeTool: (projectId: string, toolId: string, arguments_: Record<string, unknown> = {}) =>
    http
      .post<InvokeToolResult>(`/projects/${projectId}/tools/${encodeURIComponent(toolId)}/invoke`, {
        arguments: arguments_,
      })
      .then((r) => r.data),
};
