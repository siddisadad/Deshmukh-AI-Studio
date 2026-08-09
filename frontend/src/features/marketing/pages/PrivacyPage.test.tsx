import { render, screen } from '@testing-library/react';
import React from 'react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { PrivacyPage } from './PrivacyPage';

void React;

describe('PrivacyPage', () => {
  it('renders privacy headings and contact path', () => {
    render(
      <MemoryRouter>
        <PrivacyPage />
      </MemoryRouter>,
    );

    expect(screen.getByRole('heading', { level: 1, name: 'Privacy' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'contact form' })).toHaveAttribute('href', '/contact');
  });
});
