import { http } from '../../../shared/api/httpClient';

export interface Project {
  id: string;
  organizationId: string;
  name: string;
  projectKey: string;
  description?: string | null;
  status: 'ACTIVE' | 'ARCHIVED';
  role: string;
  archivedAt?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface Organization {
  id: string;
  name: string;
  slug: string;
  role: string;
  createdAt: string;
}

export interface DashboardData {
  projects: {
    id: string;
    name: string;
    projectKey: string;
    status: string;
    requirementCount: number;
    openTaskCount: number;
    doneTaskCount: number;
    updatedAt: string;
  }[];
  recentActivity: {
    action: string;
    entityType: string | null;
    entityId: string | null;
    createdAt: string;
  }[];
}

export const projectsApi = {
  listOrgs: () => http.get<Organization[]>('/organizations').then((r) => r.data),
  listProjects: (orgId: string, status = 'ACTIVE') =>
    http.get<Project[]>(`/organizations/${orgId}/projects`, { params: { status } }).then((r) => r.data),
  createProject: (orgId: string, body: { name: string; projectKey: string; description?: string }) =>
    http.post<Project>(`/organizations/${orgId}/projects`, body).then((r) => r.data),
  getProject: (projectId: string) => http.get<Project>(`/projects/${projectId}`).then((r) => r.data),
  updateProject: (projectId: string, body: { name?: string; projectKey?: string; description?: string }) =>
    http.patch<Project>(`/projects/${projectId}`, body).then((r) => r.data),
  archiveProject: (projectId: string) => http.post<Project>(`/projects/${projectId}/archive`).then((r) => r.data),
  unarchiveProject: (projectId: string) => http.post<Project>(`/projects/${projectId}/unarchive`).then((r) => r.data),
  dashboard: () => http.get<DashboardData>('/dashboard').then((r) => r.data),
};
