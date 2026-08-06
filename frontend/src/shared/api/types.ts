export interface ApiErrorBody {
  timestamp?: string;
  status: number;
  code: string;
  message: string;
  path?: string;
  requestId?: string;
  details?: { field: string; message: string }[];
}

export class ApiError extends Error {
  status: number;
  code: string;
  details?: { field: string; message: string }[];

  constructor(body: ApiErrorBody) {
    super(body.message || 'Request failed');
    this.name = 'ApiError';
    this.status = body.status;
    this.code = body.code;
    this.details = body.details;
  }
}

export interface User {
  id: string;
  email: string;
  displayName: string;
  theme: 'LIGHT' | 'DARK' | 'SYSTEM';
}

export interface Organization {
  id: string;
  name: string;
  slug: string;
}

export interface TokenResponse {
  user: User;
  organization: Organization;
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

export interface MeResponse {
  id: string;
  email: string;
  displayName: string;
  theme: 'LIGHT' | 'DARK' | 'SYSTEM';
  organizations: { id: string; name: string; slug: string; role: string }[];
}
