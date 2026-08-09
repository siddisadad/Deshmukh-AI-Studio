import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React from 'react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { contactInboxApi } from '../api/contactInboxApi';
import { ContactInboxSettingsPage } from './ContactInboxSettingsPage';

void React;

vi.mock('../api/contactInboxApi', () => ({
  contactInboxApi: {
    access: vi.fn(),
    list: vi.fn(),
    markRead: vi.fn(),
  },
}));

function renderPage() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={['/settings/contact-inbox']}>
        <Routes>
          <Route path="/settings/contact-inbox" element={<ContactInboxSettingsPage />} />
          <Route path="/dashboard" element={<p>Dashboard</p>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('ContactInboxSettingsPage', () => {
  beforeEach(() => {
    vi.mocked(contactInboxApi.access).mockReset();
    vi.mocked(contactInboxApi.list).mockReset();
    vi.mocked(contactInboxApi.markRead).mockReset();
  });

  it('redirects non-staff users away', async () => {
    vi.mocked(contactInboxApi.access).mockResolvedValue({ canAccessInbox: false });
    renderPage();
    expect(await screen.findByText('Dashboard')).toBeInTheDocument();
  });

  it('lists inquiries and marks one read', async () => {
    const user = userEvent.setup();
    vi.mocked(contactInboxApi.access).mockResolvedValue({ canAccessInbox: true });
    vi.mocked(contactInboxApi.list).mockResolvedValue([
      {
        id: 'inq-1',
        name: 'Ada',
        email: 'ada@example.com',
        topic: 'Partnership',
        message: 'Hello',
        sourceIp: '127.0.0.1',
        createdAt: '2026-08-09T12:00:00Z',
        readAt: null,
      },
    ]);
    vi.mocked(contactInboxApi.markRead).mockResolvedValue({
      id: 'inq-1',
      name: 'Ada',
      email: 'ada@example.com',
      topic: 'Partnership',
      message: 'Hello',
      sourceIp: '127.0.0.1',
      createdAt: '2026-08-09T12:00:00Z',
      readAt: '2026-08-09T13:00:00Z',
    });

    renderPage();

    expect(await screen.findByRole('heading', { name: 'Contact inbox' })).toBeInTheDocument();
    expect(await screen.findByText('Ada')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Mark read' }));
    await waitFor(() => {
      expect(contactInboxApi.markRead).toHaveBeenCalledWith('inq-1');
    });
  });
});
