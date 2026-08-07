import { describe, expect, it } from 'vitest';
import { formatApiError } from './formatApiError';
import { ApiError } from './types';

describe('formatApiError', () => {
  it('joins field details when present', () => {
    const err = new ApiError({
      status: 400,
      code: 'VALIDATION_ERROR',
      message: 'Request validation failed',
      details: [
        { field: 'newPassword', message: 'too short' },
        { field: 'email', message: 'invalid' },
      ],
    });
    expect(formatApiError(err, 'fallback')).toBe('newPassword: too short; email: invalid');
  });

  it('falls back to message then default', () => {
    expect(formatApiError(new ApiError({ status: 400, code: 'X', message: 'Nope' }), 'fallback')).toBe('Nope');
    expect(formatApiError(new Error('x'), 'fallback')).toBe('fallback');
  });
});
