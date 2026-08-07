import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { MAIN_CONTENT_ID, SkipToContent } from './SkipToContent';

describe('SkipToContent', () => {
  it('links to the main content landmark', () => {
    render(<SkipToContent />);

    const link = screen.getByTestId('skip-to-content');
    expect(link).toHaveAttribute('href', `#${MAIN_CONTENT_ID}`);
    expect(link).toHaveTextContent('Skip to main content');
  });
});
