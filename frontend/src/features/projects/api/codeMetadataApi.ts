import { http } from '../../../shared/api/httpClient';

export interface CodeFile {
  id: string;
  projectId: string;
  path: string;
  language: string;
  snippet: string;
  sizeBytes: number;
  updatedAt: string;
}

export interface CodeMetadataSummary {
  fileCount: number;
  maxFilesPerProject: number;
  files: CodeFile[];
}

export interface CodeFileInput {
  path: string;
  language?: string;
  snippet?: string;
  sizeBytes?: number;
}

export const codeMetadataApi = {
  summary: (projectId: string) =>
    http.get<CodeMetadataSummary>(`/projects/${projectId}/code-metadata`).then((r) => r.data),
  replace: (projectId: string, files: CodeFileInput[]) =>
    http.put<CodeMetadataSummary>(`/projects/${projectId}/code-metadata`, { files }).then((r) => r.data),
};
