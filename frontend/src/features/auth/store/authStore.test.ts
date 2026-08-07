import { beforeEach, describe, expect, it } from 'vitest';
import { useAuthStore } from './authStore';

describe('authStore', () => {
  beforeEach(() => {
    useAuthStore.getState().clearSession();
  });

  it('stores session and refresh token', () => {
    useAuthStore.getState().setSession({
      user: {
        id: 'u1',
        email: 'ada@example.com',
        displayName: 'Ada',
        theme: 'SYSTEM',
      },
      organization: { id: 'o1', name: "Ada's Workspace", slug: 'ada-workspace' },
      accessToken: 'access',
      refreshToken: 'refresh',
    });

    const state = useAuthStore.getState();
    expect(state.accessToken).toBe('access');
    expect(state.refreshToken).toBe('refresh');
    expect(state.user?.email).toBe('ada@example.com');
    expect(localStorage.getItem('aistudio.refreshToken')).toBe('refresh');
  });

  it('clears session and local storage', () => {
    useAuthStore.getState().setSession({
      user: {
        id: 'u1',
        email: 'ada@example.com',
        displayName: 'Ada',
        theme: 'SYSTEM',
      },
      organization: { id: 'o1', name: "Ada's Workspace", slug: 'ada-workspace' },
      accessToken: 'access',
      refreshToken: 'refresh',
    });
    useAuthStore.getState().clearSession();
    const state = useAuthStore.getState();
    expect(state.accessToken).toBeNull();
    expect(state.refreshToken).toBeNull();
    expect(localStorage.getItem('aistudio.refreshToken')).toBeNull();
  });

  it('hydrates refresh token from local storage', () => {
    localStorage.setItem('aistudio.refreshToken', 'persisted');
    useAuthStore.getState().hydrateRefreshToken();
    expect(useAuthStore.getState().refreshToken).toBe('persisted');
  });
});
