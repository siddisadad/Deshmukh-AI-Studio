import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import React, { type ReactElement } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { EmptyState } from './EmptyState';

void React; // classic JSX transform under Vitest

function renderWithTheme(ui: ReactElement) {
  return render(<ThemeProvider theme={createTheme()}>{ui}</ThemeProvider>);
}

describe('EmptyState', () => {
  it('renders title and description', () => {
    renderWithTheme(
      <EmptyState title="No projects" description="Create your first project to get started." />,
    );
    expect(screen.getByText('No projects')).toBeInTheDocument();
    expect(screen.getByText('Create your first project to get started.')).toBeInTheDocument();
  });

  it('invokes action callback', async () => {
    const user = userEvent.setup();
    const onAction = vi.fn();
    renderWithTheme(
      <EmptyState
        title="Empty"
        description="Nothing here"
        actionLabel="Add item"
        onAction={onAction}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Add item' }));
    expect(onAction).toHaveBeenCalledTimes(1);
  });
});
