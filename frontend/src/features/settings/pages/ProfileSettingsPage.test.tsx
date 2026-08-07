import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import React, { type ReactElement } from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '../../../shared/api/types';
import { authApi } from '../../auth/api/authApi';
import { useAuthStore } from '../../auth/store/authStore';
import { ProfileSettingsPage } from './ProfileSettingsPage';

void React;

vi.mock('../../auth/api/authApi', () => ({
  authApi: {
    me: vi.fn(),
    updateProfile: vi.fn(),
    changePassword: vi.fn(),
  },
}));

function renderPage(ui: ReactElement) {
  return render(<ThemeProvider theme={createTheme()}>{ui}</ThemeProvider>);
}

describe('ProfileSettingsPage', () => {
  beforeEach(() => {
    useAuthStore.getState().clearSession();
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
    vi.mocked(authApi.me).mockResolvedValue({
      id: 'u1',
      email: 'ada@example.com',
      displayName: 'Ada',
      theme: 'SYSTEM',
      organizations: [{ id: 'o1', name: "Ada's Workspace", slug: 'ada-workspace', role: 'OWNER' }],
    });
    vi.mocked(authApi.changePassword).mockReset();
  });

  it('shows email read-only and changes password', async () => {
    const user = userEvent.setup();
    vi.mocked(authApi.changePassword).mockResolvedValue(undefined);

    renderPage(<ProfileSettingsPage />);

    await waitFor(() => expect(screen.getByTestId('profile-email')).toHaveValue('ada@example.com'));
    expect(screen.getByTestId('profile-email')).toBeDisabled();

    await user.type(screen.getByTestId('profile-current-password'), 'Str0ngPass!');
    await user.type(screen.getByTestId('profile-new-password'), 'NewStr0ngPass!');
    await user.type(screen.getByTestId('profile-confirm-password'), 'NewStr0ngPass!');
    await user.click(screen.getByTestId('profile-change-password'));

    await waitFor(() =>
      expect(authApi.changePassword).toHaveBeenCalledWith({
        currentPassword: 'Str0ngPass!',
        newPassword: 'NewStr0ngPass!',
      }),
    );
    expect(await screen.findByText('Password updated')).toBeInTheDocument();
  });

  it('rejects mismatched passwords without calling API', async () => {
    const user = userEvent.setup();

    renderPage(<ProfileSettingsPage />);
    await waitFor(() => expect(screen.getByTestId('profile-email')).toHaveValue('ada@example.com'));

    await user.type(screen.getByTestId('profile-current-password'), 'Str0ngPass!');
    await user.type(screen.getByTestId('profile-new-password'), 'NewStr0ngPass!');
    await user.type(screen.getByTestId('profile-confirm-password'), 'OtherPass1234');
    await user.click(screen.getByTestId('profile-change-password'));

    expect(screen.getByText('Passwords do not match')).toBeInTheDocument();
    expect(authApi.changePassword).not.toHaveBeenCalled();
  });

  it('surfaces API errors from change password', async () => {
    const user = userEvent.setup();
    vi.mocked(authApi.changePassword).mockRejectedValue(
      new ApiError({ status: 401, code: 'INVALID_CREDENTIALS', message: 'Current password is incorrect' }),
    );

    renderPage(<ProfileSettingsPage />);
    await waitFor(() => expect(screen.getByTestId('profile-email')).toHaveValue('ada@example.com'));

    await user.type(screen.getByTestId('profile-current-password'), 'WrongPass12');
    await user.type(screen.getByTestId('profile-new-password'), 'NewStr0ngPass!');
    await user.type(screen.getByTestId('profile-confirm-password'), 'NewStr0ngPass!');
    await user.click(screen.getByTestId('profile-change-password'));

    expect(await screen.findByText('Current password is incorrect')).toBeInTheDocument();
  });
});
