import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React from 'react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ContactPage } from './ContactPage';

void React;

describe('ContactPage', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders the contact form', () => {
    render(
      <MemoryRouter>
        <ContactPage />
      </MemoryRouter>,
    );

    expect(screen.getByRole('heading', { level: 1, name: /Talk with Deshmukh Technology/i })).toBeInTheDocument();
    expect(screen.getByLabelText('Name')).toBeInTheDocument();
    expect(screen.getByLabelText('Email')).toBeInTheDocument();
    expect(screen.getByLabelText('Message')).toBeInTheDocument();
  });

  it('opens a mailto link on submit', async () => {
    const user = userEvent.setup();
    const assign = vi.fn();
    vi.stubGlobal('location', { ...window.location, assign, origin: 'http://localhost' });

    render(
      <MemoryRouter>
        <ContactPage />
      </MemoryRouter>,
    );

    await user.type(screen.getByLabelText('Name'), 'Ada');
    await user.type(screen.getByLabelText('Email'), 'ada@example.com');
    await user.selectOptions(screen.getByLabelText('Topic'), 'Partnership');
    await user.type(screen.getByLabelText('Message'), 'Hello');
    await user.click(screen.getByRole('button', { name: 'Send message' }));

    expect(assign).toHaveBeenCalledTimes(1);
    const href = assign.mock.calls[0][0] as string;
    expect(href.startsWith('mailto:hello@deshmukh.tech?')).toBe(true);
    expect(href).toContain(encodeURIComponent('Partnership'));
  });
});
