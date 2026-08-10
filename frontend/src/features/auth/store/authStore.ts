import { create } from 'zustand';
import type { Organization, User } from '../../../shared/api/types';

const REFRESH_KEY = 'aistudio.refreshToken';

export type AuthStatus = 'unknown' | 'authenticated' | 'unauthenticated';

function readStoredRefreshToken(): string | null {
  try {
    return localStorage.getItem(REFRESH_KEY);
  } catch {
    return null;
  }
}

function writeStoredRefreshToken(token: string) {
  try {
    localStorage.setItem(REFRESH_KEY, token);
  } catch {
    // private browsing
  }
}

function removeStoredRefreshToken() {
  try {
    localStorage.removeItem(REFRESH_KEY);
  } catch {
    // private browsing
  }
}

interface AuthState {
  user: User | null;
  organization: Organization | null;
  accessToken: string | null;
  refreshToken: string | null;
  authStatus: AuthStatus;
  setSession: (payload: {
    user: User;
    organization: Organization;
    accessToken: string;
    refreshToken: string;
  }) => void;
  setAccessToken: (token: string) => void;
  setAuthStatus: (status: AuthStatus) => void;
  clearSession: () => void;
  hydrateRefreshToken: () => void;
}

const initialRefreshToken = readStoredRefreshToken();

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  organization: null,
  accessToken: null,
  refreshToken: initialRefreshToken,
  authStatus: initialRefreshToken ? 'unknown' : 'unauthenticated',
  setSession: ({ user, organization, accessToken, refreshToken }) => {
    writeStoredRefreshToken(refreshToken);
    set({ user, organization, accessToken, refreshToken, authStatus: 'authenticated' });
  },
  setAccessToken: (accessToken) => set({ accessToken }),
  setAuthStatus: (authStatus) => set({ authStatus }),
  clearSession: () => {
    removeStoredRefreshToken();
    set({
      user: null,
      organization: null,
      accessToken: null,
      refreshToken: null,
      authStatus: 'unauthenticated',
    });
  },
  hydrateRefreshToken: () => {
    const refreshToken = readStoredRefreshToken();
    if (refreshToken) {
      set({ refreshToken, authStatus: 'unknown' });
    } else {
      set({ authStatus: 'unauthenticated' });
    }
  },
}));
