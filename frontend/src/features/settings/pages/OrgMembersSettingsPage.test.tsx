import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import React, { type ReactElement } from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { organizationsApi } from '../../projects/api/organizationsApi';
import { useAuthStore } from '../../auth/store/authStore';
import { OrgMembersSettingsPage } from './OrgMembersSettingsPage';

void React;

vi.mock('../../projects/api/organizationsApi', () => ({
  organizationsApi: {
    get: vi.fn(),
    listMembers: vi.fn(),
    addMember: vi.fn(),
  },
}));

function renderPage(ui: ReactElement) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>
      <ThemeProvider theme={createTheme()}>{ui}</ThemeProvider>
    </QueryClientProvider>,
  );
}

describe('OrgMembersSettingsPage', () => {
  beforeEach(() => {
    useAuthStore.getState().clearSession();
    useAuthStore.getState().setSession({
      user: {
        id: 'u1',
        email: 'owner@example.com',
        displayName: 'Owner',
        theme: 'SYSTEM',
      },
      organization: { id: 'o1', name: 'Acme', slug: 'acme' },
      accessToken: 'access',
      refreshToken: 'refresh',
    });
    vi.mocked(organizationsApi.get).mockResolvedValue({
      id: 'o1',
      name: 'Acme',
      slug: 'acme',
      role: 'OWNER',
      createdAt: '2026-01-01T00:00:00Z',
    });
    vi.mocked(organizationsApi.listMembers).mockResolvedValue([
      { userId: 'u1', email: 'owner@example.com', displayName: 'Owner', role: 'OWNER' },
    ]);
    vi.mocked(organizationsApi.addMember).mockReset();
  });

  it('lists members and adds an existing user', async () => {
    const user = userEvent.setup();
    vi.mocked(organizationsApi.addMember).mockResolvedValue({
      userId: 'u2',
      email: 'member@example.com',
      displayName: 'Member',
      role: 'MEMBER',
    });

    renderPage(<OrgMembersSettingsPage />);

    await waitFor(() => expect(screen.getByTestId('org-members-table')).toBeInTheDocument());
    expect(screen.getByText('owner@example.com')).toBeInTheDocument();

    await user.type(screen.getByTestId('org-member-email'), 'member@example.com');
    await user.click(screen.getByTestId('org-member-add-submit'));

    await waitFor(() =>
      expect(organizationsApi.addMember).toHaveBeenCalledWith('o1', {
        email: 'member@example.com',
        role: 'MEMBER',
      }),
    );
    expect(await screen.findByText(/Added Member as MEMBER/i)).toBeInTheDocument();
  });

  it('hides invite for non-admin members', async () => {
    vi.mocked(organizationsApi.get).mockResolvedValue({
      id: 'o1',
      name: 'Acme',
      slug: 'acme',
      role: 'MEMBER',
      createdAt: '2026-01-01T00:00:00Z',
    });

    renderPage(<OrgMembersSettingsPage />);
    await waitFor(() => expect(screen.getByTestId('org-member-invite-restricted')).toBeInTheDocument());
    expect(screen.getByTestId('org-member-add-submit')).toBeDisabled();
  });
});
