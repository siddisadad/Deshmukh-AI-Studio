const RETURN_PATH_KEY = 'aistudio.auth.returnPath';

export function saveAuthReturnPath(location: { pathname: string; search: string; hash: string }) {
  const path = `${location.pathname}${location.search}${location.hash}`;
  if (path === '/login' || path === '/register' || path.startsWith('/auth/')) {
    return;
  }
  try {
    sessionStorage.setItem(RETURN_PATH_KEY, path);
  } catch {
    // private browsing
  }
}

export function consumeAuthReturnPath(fallback = '/dashboard'): string {
  try {
    const stored = sessionStorage.getItem(RETURN_PATH_KEY);
    sessionStorage.removeItem(RETURN_PATH_KEY);
    return stored && stored.startsWith('/') ? stored : fallback;
  } catch {
    return fallback;
  }
}
