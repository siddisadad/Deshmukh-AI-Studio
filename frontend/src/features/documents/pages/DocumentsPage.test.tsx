import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import React, { type ReactElement } from 'react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { projectsApi } from '../../projects/api/projectsApi';
import { documentsApi } from '../api/documentsApi';
import { DocumentsPage } from './DocumentsPage';

void React;

vi.mock('../../projects/api/projectsApi', () => ({
  projectsApi: {
    getProject: vi.fn(),
  },
}));

vi.mock('../api/documentsApi', () => ({
  documentsApi: {
    list: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    remove: vi.fn(),
    generate: vi.fn(),
  },
}));

function renderPage(ui: ReactElement) {
  return render(
    <ThemeProvider theme={createTheme()}>
      <MemoryRouter initialEntries={['/projects/p1/documents']}>
        <Routes>
          <Route path="/projects/:projectId/documents" element={ui} />
        </Routes>
      </MemoryRouter>
    </ThemeProvider>,
  );
}

describe('DocumentsPage', () => {
  beforeEach(() => {
    vi.mocked(projectsApi.getProject).mockResolvedValue({
      id: 'p1',
      organizationId: 'o1',
      name: 'Docs Proj',
      projectKey: 'DP',
      status: 'ACTIVE',
      role: 'OWNER',
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    });
    vi.mocked(documentsApi.list).mockResolvedValue([
      {
        id: 'd1',
        projectId: 'p1',
        title: 'README',
        docType: 'README',
        contentMd: '# Hello',
        createdAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-01T00:00:00Z',
      },
    ]);
    vi.mocked(documentsApi.remove).mockReset();
    vi.spyOn(window, 'confirm').mockReturnValue(true);
  });

  it('deletes the selected document after confirm', async () => {
    const user = userEvent.setup();
    vi.mocked(documentsApi.remove).mockResolvedValue(undefined);

    renderPage(<DocumentsPage />);

    await waitFor(() => expect(screen.getByTestId('document-delete-button')).toBeInTheDocument());
    await user.click(screen.getByTestId('document-delete-button'));

    await waitFor(() => expect(documentsApi.remove).toHaveBeenCalledWith('d1'));
    expect(await screen.findByText('Document deleted')).toBeInTheDocument();
    expect(screen.getByText(/Select a document/i)).toBeInTheDocument();
  });

  it('does not delete when confirm is cancelled', async () => {
    const user = userEvent.setup();
    vi.mocked(window.confirm).mockReturnValue(false);

    renderPage(<DocumentsPage />);
    await waitFor(() => expect(screen.getByTestId('document-delete-button')).toBeInTheDocument());
    await user.click(screen.getByTestId('document-delete-button'));

    expect(documentsApi.remove).not.toHaveBeenCalled();
  });
});
