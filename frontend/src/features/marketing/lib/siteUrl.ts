/** Canonical public origin for marketing SEO (no trailing slash). */
export function getPublicSiteOrigin(): string {
  const configured = import.meta.env.VITE_PUBLIC_SITE_URL as string | undefined;
  if (configured && configured.trim()) {
    return configured.replace(/\/$/, '');
  }
  if (typeof window !== 'undefined' && window.location?.origin) {
    return window.location.origin;
  }
  return 'https://deshmukh.tech';
}

export function absoluteSiteUrl(path = '/'): string {
  const normalized = path.startsWith('/') ? path : `/${path}`;
  return `${getPublicSiteOrigin()}${normalized === '/' ? '/' : normalized}`;
}
