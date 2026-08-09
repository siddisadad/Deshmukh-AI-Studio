import { afterEach, describe, expect, it, vi } from 'vitest';
import { absoluteSiteUrl, getPublicSiteOrigin } from './siteUrl';

describe('siteUrl', () => {
  afterEach(() => {
    vi.unstubAllEnvs();
  });

  it('uses VITE_PUBLIC_SITE_URL when set', () => {
    vi.stubEnv('VITE_PUBLIC_SITE_URL', 'https://deshmukh.tech/');
    expect(getPublicSiteOrigin()).toBe('https://deshmukh.tech');
    expect(absoluteSiteUrl('/about')).toBe('https://deshmukh.tech/about');
  });

  it('falls back to window origin when env is empty', () => {
    vi.stubEnv('VITE_PUBLIC_SITE_URL', '');
    expect(getPublicSiteOrigin()).toBe(window.location.origin);
  });
});
