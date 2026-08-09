import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useAuthStore } from '../../auth/store/authStore';
import { organizationsApi } from '../../projects/api/organizationsApi';
import { aiPolicyApi } from '../api/aiPolicyApi';
import { AiRoutingSettingsPage } from './AiRoutingSettingsPage';

vi.mock('../../projects/api/organizationsApi', () => ({
  organizationsApi: {
    get: vi.fn(),
  },
}));

vi.mock('../api/aiPolicyApi', () => ({
  aiPolicyApi: {
    get: vi.fn(),
    update: vi.fn(),
    listChanges: vi.fn(),
    approvePending: vi.fn(),
    rejectPending: vi.fn(),
  },
}));

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <AiRoutingSettingsPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('AiRoutingSettingsPage', () => {
  beforeEach(() => {
    vi.mocked(organizationsApi.get).mockResolvedValue({
      id: 'org-1',
      name: 'Test Org',
      slug: 'test',
      role: 'OWNER',
      createdAt: '2026-01-01T00:00:00Z',
    });
    vi.mocked(aiPolicyApi.get).mockResolvedValue({
      providerChain: 'mock',
      dailyTokenBudget: 50000,
      effectiveDailyTokenBudget: 50000,
      tokensUsedToday: 1200,
      tokenBudgetRemaining: 48800,
      modelMap: null,
      deployRegion: 'eu-west',
      effectiveDeployRegion: 'eu-west',
      changeApprovalRequired: false,
      pendingChange: null,
    });
    vi.mocked(aiPolicyApi.listChanges).mockResolvedValue([]);
    useAuthStore.setState({
      organization: { id: 'org-1', name: 'Test Org', slug: 'test' },
    });
  });

  it('loads and displays policy fields for owners', async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.getByTestId('ai-policy-provider-chain')).toHaveValue('mock');
    });
    expect(screen.getByTestId('ai-policy-token-budget')).toHaveValue('50000');
    expect(screen.getByTestId('ai-policy-deploy-region')).toHaveValue('eu-west');
    expect(screen.getByTestId('ai-policy-save')).toBeInTheDocument();
  });

  it('saves policy updates', async () => {
    vi.mocked(aiPolicyApi.update).mockResolvedValue({
      providerChain: 'mock,openai',
      dailyTokenBudget: 60000,
      effectiveDailyTokenBudget: 60000,
      tokensUsedToday: 1200,
      tokenBudgetRemaining: 58800,
      modelMap: 'DEVELOPER=mock:mock-1',
      deployRegion: 'us-east',
      effectiveDeployRegion: 'us-east',
      changeApprovalRequired: false,
      pendingChange: null,
    });

    renderPage();
    await waitFor(() => {
      expect(screen.getByTestId('ai-policy-provider-chain')).not.toBeDisabled();
    });

    await userEvent.clear(screen.getByTestId('ai-policy-provider-chain'));
    await userEvent.type(screen.getByTestId('ai-policy-provider-chain'), 'mock,openai');
    await userEvent.click(screen.getByTestId('ai-policy-save'));

    await waitFor(() => {
      expect(aiPolicyApi.update).toHaveBeenCalledWith('org-1', {
        providerChain: 'mock,openai',
        dailyTokenBudget: 50000,
        modelMap: '',
        deployRegion: 'eu-west',
      });
    });
  });
});
