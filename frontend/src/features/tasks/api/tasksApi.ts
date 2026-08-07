import { http } from '../../../shared/api/httpClient';

export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'REVIEW' | 'DONE';

export interface Label {
  id: string;
  projectId: string;
  name: string;
  color: string;
}

export interface Task {
  id: string;
  projectId: string;
  requirementId?: string | null;
  title: string;
  description?: string | null;
  status: TaskStatus;
  priority: string;
  assigneeId?: string | null;
  sortOrder: number;
  labels: Label[];
  createdAt: string;
  updatedAt: string;
}

export const tasksApi = {
  list: (projectId: string) => http.get<Task[]>(`/projects/${projectId}/tasks`).then((r) => r.data),
  create: (
    projectId: string,
    body: {
      title: string;
      description?: string;
      priority?: string;
      status?: TaskStatus;
      requirementId?: string;
      labelIds?: string[];
    },
  ) => http.post<Task>(`/projects/${projectId}/tasks`, body).then((r) => r.data),
  update: (
    taskId: string,
    body: Partial<{
      title: string;
      description: string;
      priority: string;
      status: TaskStatus;
      requirementId: string;
      clearRequirementId: boolean;
      labelIds: string[];
      sortOrder: number;
    }>,
  ) => http.patch<Task>(`/tasks/${taskId}`, body).then((r) => r.data),
  remove: (taskId: string) => http.delete(`/tasks/${taskId}`).then(() => undefined),
  listLabels: (projectId: string) => http.get<Label[]>(`/projects/${projectId}/labels`).then((r) => r.data),
  createLabel: (projectId: string, body: { name: string; color?: string }) =>
    http.post<Label>(`/projects/${projectId}/labels`, body).then((r) => r.data),
  deleteLabel: (labelId: string) => http.delete(`/labels/${labelId}`).then(() => undefined),
};
