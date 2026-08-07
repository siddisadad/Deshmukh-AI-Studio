import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import React from 'react';
import { describe, expect, it, vi } from 'vitest';
import type { Requirement } from '../../requirements/api/requirementsApi';
import { RequirementSelect } from './RequirementSelect';

void React;

const requirements: Requirement[] = [
  {
    id: 'r1',
    projectId: 'p1',
    title: 'Login flow',
    description: '',
    status: 'DRAFT',
    priority: 'HIGH',
    sortOrder: 0,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  },
  {
    id: 'r2',
    projectId: 'p1',
    title: 'Billing',
    description: '',
    status: 'DRAFT',
    priority: 'MEDIUM',
    sortOrder: 1,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  },
];

describe('RequirementSelect', () => {
  it('selects a requirement id', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();

    render(
      <ThemeProvider theme={createTheme()}>
        <RequirementSelect requirements={requirements} value="" onChange={onChange} />
      </ThemeProvider>,
    );

    await user.click(screen.getByRole('combobox'));
    const listbox = await screen.findByRole('listbox');
    await user.click(within(listbox).getByText('Billing'));
    expect(onChange).toHaveBeenCalledWith('r2');
  });

  it('allows clearing the link', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();

    render(
      <ThemeProvider theme={createTheme()}>
        <RequirementSelect requirements={requirements} value="r1" onChange={onChange} />
      </ThemeProvider>,
    );

    await user.click(screen.getByRole('combobox'));
    const listbox = await screen.findByRole('listbox');
    await user.click(within(listbox).getByTestId('requirement-select-none'));
    expect(onChange).toHaveBeenCalledWith('');
  });

  it('shows empty helper when no requirements exist', () => {
    render(
      <ThemeProvider theme={createTheme()}>
        <RequirementSelect requirements={[]} value="" onChange={vi.fn()} />
      </ThemeProvider>,
    );

    expect(screen.getByTestId('requirement-select-empty')).toHaveTextContent(/No requirements yet/i);
  });
});
