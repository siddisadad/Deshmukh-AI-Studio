import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { AppShell } from './AppShell';

vi.mock('../../features/auth/api/authApi', () => ({
  authApi: { logout: vi.fn() },
}));

vi.mock('../../features/settings/api/contactInboxApi', () => ({
  contactInboxApi: {
    access: vi.fn().mockResolvedValue({ canAccessInbox: false }),
  },
}));

vi.mock('../../features/auth/store/authStore', () => ({
  useAuthStore: (selector: (state: object) => unknown) =>
    selector({
      user: { displayName: 'Ada Lovelace', id: 'u1', email: 'ada@example.com', theme: 'SYSTEM' },
      organization: { id: 'o1', name: 'Org', slug: 'org', role: 'OWNER' },
      accessToken: 'at',
      refreshToken: 'rt',
      clearSession: vi.fn(),
    }),
}));

function renderShell() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={['/dashboard']}>
        <Routes>
          <Route element={<AppShell />}>
            <Route path="/dashboard" element={<p>Dashboard body</p>} />
          </Route>
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('AppShell accessibility', () => {
  it('exposes skip link, navigation landmark, and main region', () => {
    renderShell();

    expect(screen.getByTestId('skip-to-content')).toBeInTheDocument();
    expect(screen.getByRole('navigation', { name: 'Primary' })).toBeInTheDocument();
    expect(screen.getByRole('main')).toHaveAttribute('id', 'main-content');
    expect(screen.getByText('Dashboard body')).toBeInTheDocument();
  });
});
