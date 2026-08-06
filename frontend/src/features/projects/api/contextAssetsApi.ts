import { http } from '../../../shared/api/httpClient';

export type ContextAssetType = 'DATABASE_DESIGN' | 'API_SPEC' | 'SOURCE_METADATA' | 'OTHER';

export interface ContextAsset {
  id: string;
  projectId: string;
  assetType: ContextAssetType;
  title: string;
  content: string;
  metadata: string;
  updatedAt: string;
}

export const contextAssetsApi = {
  list: (projectId: string) =>
    http.get<ContextAsset[]>(`/projects/${projectId}/context-assets`).then((r) => r.data),
  upsert: (
    projectId: string,
    assetType: ContextAssetType,
    body: { title: string; content?: string; metadata?: string },
  ) =>
    http
      .put<ContextAsset>(`/projects/${projectId}/context-assets/${assetType}`, body)
      .then((r) => r.data),
};
