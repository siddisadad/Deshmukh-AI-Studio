import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useAuthStore } from '../../auth/store/authStore';
import { organizationsApi } from '../../projects/api/organizationsApi';
import { sloApi } from '../api/sloApi';
import { SloSettingsPage } from './SloSettingsPage';

vi.mock('../../projects/api/organizationsApi', () => ({
  organizationsApi: {
    get: vi.fn(),
  },
}));

vi.mock('../api/sloApi', () => ({
  sloApi: {
    get: vi.fn(),
    update: vi.fn(),
  },
}));

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <SloSettingsPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('SloSettingsPage', () => {
  beforeEach(() => {
    vi.mocked(organizationsApi.get).mockResolvedValue({
      id: 'org-1',
      name: 'Test Org',
      slug: 'test',
      role: 'OWNER',
      createdAt: '2026-01-01T00:00:00Z',
    });
    vi.mocked(sloApi.get).mockResolvedValue({
      availabilityTarget: 0.995,
      latencyTarget: 0.95,
      latencyThresholdSeconds: 2,
    });
    useAuthStore.setState({
      organization: { id: 'org-1', name: 'Test Org', slug: 'test' },
    });
  });

  it('loads and saves SLO targets for owners', async () => {
    vi.mocked(sloApi.update).mockResolvedValue({
      availabilityTarget: 0.99,
      latencyTarget: 0.9,
      latencyThresholdSeconds: 5,
    });

    renderPage();

    await waitFor(() => {
      expect(screen.getByTestId('slo-availability-target')).toHaveValue('0.995');
    });

    await userEvent.clear(screen.getByTestId('slo-availability-target'));
    await userEvent.type(screen.getByTestId('slo-availability-target'), '0.99');
    await userEvent.click(screen.getByTestId('slo-save'));

    await waitFor(() => {
      expect(sloApi.update).toHaveBeenCalledWith('org-1', {
        availabilityTarget: 0.99,
        latencyTarget: 0.95,
        latencyThresholdSeconds: 2,
      });
    });
  });
});
