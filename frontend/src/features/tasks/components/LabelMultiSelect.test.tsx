import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import React from 'react';
import { describe, expect, it, vi } from 'vitest';
import type { Label } from '../api/tasksApi';
import { LabelMultiSelect } from './LabelMultiSelect';

void React;

const labels: Label[] = [
  { id: 'l1', projectId: 'p1', name: 'auth', color: '#0D9488' },
  { id: 'l2', projectId: 'p1', name: 'api', color: '#2563EB' },
];

describe('LabelMultiSelect', () => {
  it('toggles label selection', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();

    render(
      <ThemeProvider theme={createTheme()}>
        <LabelMultiSelect labels={labels} value={['l1']} onChange={onChange} />
      </ThemeProvider>,
    );

    await user.click(screen.getByRole('combobox'));
    const listbox = await screen.findByRole('listbox');
    await user.click(within(listbox).getByText('api'));
    expect(onChange).toHaveBeenCalledWith(['l1', 'l2']);
  });

  it('shows empty helper when no labels exist', () => {
    render(
      <ThemeProvider theme={createTheme()}>
        <LabelMultiSelect labels={[]} value={[]} onChange={vi.fn()} />
      </ThemeProvider>,
    );

    expect(screen.getByTestId('label-multi-select-empty')).toHaveTextContent(/No labels yet/i);
    expect(screen.getByRole('combobox')).toHaveAttribute('aria-disabled', 'true');
  });
});
