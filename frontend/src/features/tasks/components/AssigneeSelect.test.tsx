import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import React from 'react';
import { describe, expect, it, vi } from 'vitest';
import type { OrgMember } from '../../projects/api/organizationsApi';
import { AssigneeSelect } from './AssigneeSelect';

void React;

const members: OrgMember[] = [
  { userId: 'u1', email: 'ada@example.com', displayName: 'Ada', role: 'OWNER' },
  { userId: 'u2', email: 'grace@example.com', displayName: 'Grace', role: 'MEMBER' },
];

describe('AssigneeSelect', () => {
  it('selects an assignee id', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();

    render(
      <ThemeProvider theme={createTheme()}>
        <AssigneeSelect members={members} value="" onChange={onChange} />
      </ThemeProvider>,
    );

    await user.click(screen.getByRole('combobox'));
    const listbox = await screen.findByRole('listbox');
    await user.click(within(listbox).getByText(/Grace/i));
    expect(onChange).toHaveBeenCalledWith('u2');
  });

  it('allows clearing the assignee', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();

    render(
      <ThemeProvider theme={createTheme()}>
        <AssigneeSelect members={members} value="u1" onChange={onChange} />
      </ThemeProvider>,
    );

    await user.click(screen.getByRole('combobox'));
    const listbox = await screen.findByRole('listbox');
    await user.click(within(listbox).getByTestId('assignee-select-none'));
    expect(onChange).toHaveBeenCalledWith('');
  });
});
