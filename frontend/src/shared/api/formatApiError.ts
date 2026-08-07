import { ApiError } from './types';

/** Prefer field details when present (e.g. Bean Validation failures). */
export function formatApiError(err: unknown, fallback: string): string {
  if (!(err instanceof ApiError)) {
    return fallback;
  }
  if (err.details?.length) {
    return err.details.map((d) => (d.field ? `${d.field}: ${d.message}` : d.message)).join('; ');
  }
  return err.message || fallback;
}
