import { render, screen } from '@testing-library/react';
import React from 'react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { ServicesPage } from './ServicesPage';

void React;

describe('ServicesPage', () => {
  it('lists core services and product CTA', () => {
    render(
      <MemoryRouter>
        <ServicesPage />
      </MemoryRouter>,
    );

    expect(screen.getByRole('heading', { level: 1, name: /Product and partnership/i })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'AI Studio' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Start with AI Studio' })).toHaveAttribute('href', '/register');
  });
});
