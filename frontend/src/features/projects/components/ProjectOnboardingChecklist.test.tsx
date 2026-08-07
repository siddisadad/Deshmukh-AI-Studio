import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { chatApi } from '../../chat/api/chatApi';
import { documentsApi } from '../../documents/api/documentsApi';
import { requirementsApi } from '../../requirements/api/requirementsApi';
import { tasksApi } from '../../tasks/api/tasksApi';
import { ProjectOnboardingChecklist } from './ProjectOnboardingChecklist';

vi.mock('../../requirements/api/requirementsApi', () => ({
  requirementsApi: { list: vi.fn() },
}));
vi.mock('../../tasks/api/tasksApi', () => ({
  tasksApi: { list: vi.fn() },
}));
vi.mock('../../documents/api/documentsApi', () => ({
  documentsApi: { list: vi.fn() },
}));
vi.mock('../../chat/api/chatApi', () => ({
  chatApi: { listConversations: vi.fn() },
}));

function renderChecklist() {
  return render(
    <ThemeProvider theme={createTheme()}>
      <MemoryRouter>
        <ProjectOnboardingChecklist projectId="p1" />
      </MemoryRouter>
    </ThemeProvider>,
  );
}

describe('ProjectOnboardingChecklist', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.mocked(requirementsApi.list).mockResolvedValue([]);
    vi.mocked(tasksApi.list).mockResolvedValue([]);
    vi.mocked(documentsApi.list).mockResolvedValue([]);
    vi.mocked(chatApi.listConversations).mockResolvedValue([]);
  });

  it('shows checklist with progress when steps are incomplete', async () => {
    renderChecklist();

    expect(await screen.findByTestId('onboarding-checklist')).toBeInTheDocument();
    expect(screen.getByTestId('onboarding-progress')).toBeInTheDocument();
    expect(screen.getByText('0 of 4 complete')).toBeInTheDocument();
    expect(screen.getByTestId('onboarding-step-requirements')).toBeInTheDocument();
  });

  it('shows success when all steps are complete', async () => {
    vi.mocked(requirementsApi.list).mockResolvedValue([
      {
        id: 'r1',
        projectId: 'p1',
        title: 'Req',
        description: null,
        improvedDescription: null,
        userStories: null,
        acceptanceCriteria: null,
        status: 'DRAFT',
        priority: 'MEDIUM',
        sortOrder: 0,
        createdAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-01T00:00:00Z',
      },
    ]);
    vi.mocked(tasksApi.list).mockResolvedValue([
      {
        id: 't1',
        projectId: 'p1',
        title: 'Task',
        description: null,
        status: 'TODO',
        priority: 'MEDIUM',
        sortOrder: 0,
        requirementId: null,
        assigneeId: null,
        labels: [],
        createdAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-01T00:00:00Z',
      },
    ]);
    vi.mocked(documentsApi.list).mockResolvedValue([
      {
        id: 'd1',
        projectId: 'p1',
        title: 'Doc',
        docType: 'GENERAL',
        contentMd: '',
        createdAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-01T00:00:00Z',
      },
    ]);
    vi.mocked(chatApi.listConversations).mockResolvedValue([
      {
        id: 'c1',
        projectId: 'p1',
        assistantRole: 'BUSINESS_ANALYST',
        title: 'Chat',
        createdAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-01T00:00:00Z',
        messageCount: 2,
      },
    ]);

    renderChecklist();

    expect(await screen.findByTestId('onboarding-complete')).toBeInTheDocument();
  });

  it('hides checklist after dismiss', async () => {
    const user = userEvent.setup();
    renderChecklist();

    await screen.findByTestId('onboarding-checklist');
    await user.click(screen.getByTestId('onboarding-dismiss'));

    await waitFor(() => expect(screen.queryByTestId('onboarding-checklist')).not.toBeInTheDocument());
  });
});
