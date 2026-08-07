import { Box } from '@mui/material';

export const MAIN_CONTENT_ID = 'main-content';

/**
 * Keyboard-first skip link; visible when focused (WCAG 2.4.1).
 */
export function SkipToContent() {
  return (
    <Box
      component="a"
      href={`#${MAIN_CONTENT_ID}`}
      sx={{
        position: 'fixed',
        top: 8,
        left: 8,
        zIndex: (theme) => theme.zIndex.tooltip + 1,
        px: 2,
        py: 1,
        bgcolor: 'primary.main',
        color: 'primary.contrastText',
        borderRadius: 1,
        textDecoration: 'none',
        fontWeight: 600,
        fontSize: 14,
        transform: 'translateY(-200%)',
        transition: 'transform 0.15s ease',
        '&:focus': {
          transform: 'translateY(0)',
          outline: '2px solid',
          outlineColor: 'primary.light',
          outlineOffset: 2,
        },
      }}
      data-testid="skip-to-content"
    >
      Skip to main content
    </Box>
  );
}
