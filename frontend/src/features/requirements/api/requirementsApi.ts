import { http } from '../../../shared/api/httpClient';

export interface Requirement {
  id: string;
  projectId: string;
  title: string;
  description: string;
  improvedDescription?: string | null;
  userStories?: string | null;
  acceptanceCriteria?: string | null;
  status: string;
  priority: string;
  sortOrder: number;
  createdAt: string;
  updatedAt: string;
}

export interface RequirementAiResponse {
  requirement: Requirement;
  assistantRole: string;
  provider: string;
  model: string;
  generatedText: string;
}

export const requirementsApi = {
  list: (projectId: string) =>
    http.get<Requirement[]>(`/projects/${projectId}/requirements`).then((r) => r.data),
  create: (projectId: string, body: { title: string; description?: string; priority?: string; status?: string }) =>
    http.post<Requirement>(`/projects/${projectId}/requirements`, body).then((r) => r.data),
  update: (
    requirementId: string,
    body: Partial<{
      title: string;
      description: string;
      improvedDescription: string;
      userStories: string;
      acceptanceCriteria: string;
      priority: string;
      status: string;
    }>,
  ) => http.patch<Requirement>(`/requirements/${requirementId}`, body).then((r) => r.data),
  remove: (requirementId: string) => http.delete(`/requirements/${requirementId}`).then(() => undefined),
  improve: (requirementId: string, instructions?: string) =>
    http
      .post<RequirementAiResponse>(`/requirements/${requirementId}/ai/improve`, instructions ? { instructions } : {})
      .then((r) => r.data),
  userStories: (requirementId: string, instructions?: string) =>
    http
      .post<RequirementAiResponse>(
        `/requirements/${requirementId}/ai/user-stories`,
        instructions ? { instructions } : {},
      )
      .then((r) => r.data),
  acceptanceCriteria: (requirementId: string, instructions?: string) =>
    http
      .post<RequirementAiResponse>(
        `/requirements/${requirementId}/ai/acceptance-criteria`,
        instructions ? { instructions } : {},
      )
      .then((r) => r.data),
};
