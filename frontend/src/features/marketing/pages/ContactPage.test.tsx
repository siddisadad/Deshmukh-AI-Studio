import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React from 'react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '../../../shared/api/types';
import { contactApi } from '../api/contactApi';
import { ContactPage } from './ContactPage';

void React;

vi.mock('../api/contactApi', () => ({
  contactApi: {
    createInquiry: vi.fn(),
  },
}));

describe('ContactPage', () => {
  beforeEach(() => {
    vi.mocked(contactApi.createInquiry).mockReset();
  });

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

  it('submits inquiry through the API and shows success', async () => {
    const user = userEvent.setup();
    vi.mocked(contactApi.createInquiry).mockResolvedValue({ id: 'inq-1' });

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

    await waitFor(() => {
      expect(contactApi.createInquiry).toHaveBeenCalledWith({
        name: 'Ada',
        email: 'ada@example.com',
        topic: 'Partnership',
        message: 'Hello',
      });
    });
    expect(await screen.findByRole('heading', { name: 'Message received' })).toBeInTheDocument();
  });

  it('shows API validation errors without mailto fallback', async () => {
    const user = userEvent.setup();
    vi.mocked(contactApi.createInquiry).mockRejectedValue(
      new ApiError({ status: 429, code: 'RATE_LIMITED', message: 'Too many contact messages' }),
    );

    render(
      <MemoryRouter>
        <ContactPage />
      </MemoryRouter>,
    );

    await user.type(screen.getByLabelText('Name'), 'Ada');
    await user.type(screen.getByLabelText('Email'), 'ada@example.com');
    await user.type(screen.getByLabelText('Message'), 'Hello');
    await user.click(screen.getByRole('button', { name: 'Send message' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Too many contact messages');
  });
});
