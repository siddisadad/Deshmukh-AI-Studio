import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import React, { type ReactElement } from 'react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { projectsApi } from '../../projects/api/projectsApi';
import { requirementsApi } from '../api/requirementsApi';
import { RequirementsPage } from './RequirementsPage';

void React;

vi.mock('../../projects/api/projectsApi', () => ({
  projectsApi: {
    getProject: vi.fn(),
  },
}));

vi.mock('../api/requirementsApi', () => ({
  requirementsApi: {
    list: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    remove: vi.fn(),
    improve: vi.fn(),
    userStories: vi.fn(),
    acceptanceCriteria: vi.fn(),
  },
}));

function renderPage(ui: ReactElement) {
  return render(
    <ThemeProvider theme={createTheme()}>
      <MemoryRouter initialEntries={['/projects/p1/requirements']}>
        <Routes>
          <Route path="/projects/:projectId/requirements" element={ui} />
        </Routes>
      </MemoryRouter>
    </ThemeProvider>,
  );
}

describe('RequirementsPage', () => {
  beforeEach(() => {
    vi.mocked(projectsApi.getProject).mockResolvedValue({
      id: 'p1',
      organizationId: 'o1',
      name: 'Reqs Proj',
      projectKey: 'RP',
      status: 'ACTIVE',
      role: 'OWNER',
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    });
    vi.mocked(requirementsApi.list).mockResolvedValue([
      {
        id: 'r1',
        projectId: 'p1',
        title: 'Password reset',
        description: 'User can reset password',
        improvedDescription: null,
        userStories: null,
        acceptanceCriteria: null,
        status: 'DRAFT',
        priority: 'HIGH',
        sortOrder: 0,
        createdAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-01T00:00:00Z',
      },
    ]);
    vi.mocked(requirementsApi.remove).mockReset();
    vi.spyOn(window, 'confirm').mockReturnValue(true);
  });

  it('deletes the selected requirement after confirm', async () => {
    const user = userEvent.setup();
    vi.mocked(requirementsApi.remove).mockResolvedValue(undefined);

    renderPage(<RequirementsPage />);

    await waitFor(() => expect(screen.getByTestId('requirement-delete-button')).toBeInTheDocument());
    await user.click(screen.getByTestId('requirement-delete-button'));

    await waitFor(() => expect(requirementsApi.remove).toHaveBeenCalledWith('r1'));
    expect(await screen.findByText('Requirement deleted')).toBeInTheDocument();
    expect(screen.getByText(/Select a requirement/i)).toBeInTheDocument();
  });

  it('does not delete when confirm is cancelled', async () => {
    const user = userEvent.setup();
    vi.mocked(window.confirm).mockReturnValue(false);

    renderPage(<RequirementsPage />);
    await waitFor(() => expect(screen.getByTestId('requirement-delete-button')).toBeInTheDocument());
    await user.click(screen.getByTestId('requirement-delete-button'));

    expect(requirementsApi.remove).not.toHaveBeenCalled();
  });
});
