import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React from 'react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { authApi } from '../api/authApi';
import { ResetPasswordPage } from './ResetPasswordPage';

void React;

vi.mock('../api/authApi', () => ({
  authApi: {
    resetPassword: vi.fn(),
  },
}));

describe('ResetPasswordPage', () => {
  beforeEach(() => {
    vi.mocked(authApi.resetPassword).mockReset();
  });

  it('prefills token from query and submits new password', async () => {
    const user = userEvent.setup();
    vi.mocked(authApi.resetPassword).mockResolvedValue(undefined);

    render(
      <MemoryRouter initialEntries={['/reset-password?token=abc-token']}>
        <Routes>
          <Route path="/reset-password" element={<ResetPasswordPage />} />
          <Route path="/login" element={<div>Login Page</div>} />
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.getByTestId('reset-token')).toHaveValue('abc-token');
    await user.type(screen.getByTestId('reset-password'), 'NewPass1234');
    await user.type(screen.getByTestId('reset-password-confirm'), 'NewPass1234');
    await user.click(screen.getByTestId('reset-submit'));

    expect(authApi.resetPassword).toHaveBeenCalledWith({
      token: 'abc-token',
      newPassword: 'NewPass1234',
    });
    expect(await screen.findByText('Login Page')).toBeInTheDocument();
  });

  it('rejects mismatched passwords without calling API', async () => {
    const user = userEvent.setup();

    render(
      <MemoryRouter initialEntries={['/reset-password']}>
        <Routes>
          <Route path="/reset-password" element={<ResetPasswordPage />} />
        </Routes>
      </MemoryRouter>,
    );

    await user.type(screen.getByTestId('reset-token'), 'tok');
    await user.type(screen.getByTestId('reset-password'), 'NewPass1234');
    await user.type(screen.getByTestId('reset-password-confirm'), 'OtherPass1234');
    await user.click(screen.getByTestId('reset-submit'));

    expect(screen.getByText('Passwords do not match')).toBeInTheDocument();
    expect(authApi.resetPassword).not.toHaveBeenCalled();
  });
});
