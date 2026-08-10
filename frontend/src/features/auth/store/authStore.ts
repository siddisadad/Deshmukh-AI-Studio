import { create } from 'zustand';
import type { Organization, User } from '../../../shared/api/types';

const REFRESH_KEY = 'aistudio.refreshToken';

function readStoredRefreshToken(): string | null {
  try {
    return localStorage.getItem(REFRESH_KEY);
  } catch {
    return null;
  }
}

interface AuthState {
  user: User | null;
  organization: Organization | null;
  accessToken: string | null;
  refreshToken: string | null;
  setSession: (payload: {
    user: User;
    organization: Organization;
    accessToken: string;
    refreshToken: string;
  }) => void;
  setAccessToken: (token: string) => void;
  clearSession: () => void;
  hydrateRefreshToken: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  organization: null,
  accessToken: null,
  refreshToken: readStoredRefreshToken(),
  setSession: ({ user, organization, accessToken, refreshToken }) => {
    localStorage.setItem(REFRESH_KEY, refreshToken);
    set({ user, organization, accessToken, refreshToken });
  },
  setAccessToken: (accessToken) => set({ accessToken }),
  clearSession: () => {
    localStorage.removeItem(REFRESH_KEY);
    set({ user: null, organization: null, accessToken: null, refreshToken: null });
  },
  hydrateRefreshToken: () => {
    const refreshToken = localStorage.getItem(REFRESH_KEY);
    if (refreshToken) {
      set({ refreshToken });
    }
  },
}));
