import { render, screen } from '@testing-library/react';
import React from 'react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it } from 'vitest';
import { useAuthStore } from '../../auth/store/authStore';
import { HomePage } from './HomePage';

void React;

describe('HomePage', () => {
  beforeEach(() => {
    useAuthStore.setState({
      accessToken: null,
      refreshToken: null,
      user: null,
      organization: null,
    });
  });

  it('renders Deshmukh Technology as the brand hero and guest CTAs', () => {
    render(
      <MemoryRouter>
        <HomePage />
      </MemoryRouter>,
    );

    expect(screen.getByRole('heading', { level: 1, name: /Deshmukh\s*Technology/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Start with AI Studio' })).toHaveAttribute('href', '/register');
    expect(screen.getAllByRole('link', { name: 'Sign in' }).length).toBeGreaterThan(0);
    expect(screen.getByRole('heading', { name: /AI Studio for software engineering/i })).toBeInTheDocument();
  });

  it('shows workspace CTA when signed in', () => {
    useAuthStore.setState({
      accessToken: 'token',
      refreshToken: 'refresh',
      user: null,
      organization: null,
    });

    render(
      <MemoryRouter>
        <HomePage />
      </MemoryRouter>,
    );

    expect(screen.getByRole('link', { name: 'Continue to workspace' })).toHaveAttribute('href', '/dashboard');
    expect(screen.getByRole('link', { name: 'Open workspace' })).toHaveAttribute('href', '/dashboard');
  });
});
