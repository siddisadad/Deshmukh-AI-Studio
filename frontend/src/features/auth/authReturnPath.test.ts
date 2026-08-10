import { beforeEach, describe, expect, it } from 'vitest';
import { consumeAuthReturnPath, saveAuthReturnPath } from './authReturnPath';

describe('authReturnPath', () => {
  beforeEach(() => {
    sessionStorage.clear();
  });

  it('saves and consumes a return path', () => {
    saveAuthReturnPath({ pathname: '/settings/git', search: '?linked=linked', hash: '#org-git-sync-runs' });
    expect(consumeAuthReturnPath()).toBe('/settings/git?linked=linked#org-git-sync-runs');
  });

  it('does not save login or auth callback paths', () => {
    saveAuthReturnPath({ pathname: '/login', search: '', hash: '' });
    saveAuthReturnPath({ pathname: '/auth/sso/callback', search: '?code=1', hash: '' });
    expect(consumeAuthReturnPath('/dashboard')).toBe('/dashboard');
  });

  it('returns fallback when nothing stored', () => {
    expect(consumeAuthReturnPath('/projects')).toBe('/projects');
  });
});
