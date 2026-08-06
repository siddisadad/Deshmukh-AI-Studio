import { createTheme } from '@mui/material/styles';

export function createAppTheme(mode: 'light' | 'dark') {
  return createTheme({
    palette: {
      mode,
      primary: {
        main: mode === 'light' ? '#0F766E' : '#2DD4BF',
      },
      secondary: {
        main: mode === 'light' ? '#1E293B' : '#94A3B8',
      },
      background: {
        default: mode === 'light' ? '#F1F5F9' : '#0B1220',
        paper: mode === 'light' ? '#FFFFFF' : '#111827',
      },
    },
    typography: {
      fontFamily: '"Source Sans 3", "Segoe UI", sans-serif',
      h4: { fontWeight: 700, letterSpacing: '-0.02em' },
      h5: { fontWeight: 650 },
      button: { textTransform: 'none', fontWeight: 600 },
    },
    shape: { borderRadius: 8 },
    components: {
      MuiButton: {
        defaultProps: { disableElevation: true },
      },
      MuiPaper: {
        styleOverrides: {
          root: { backgroundImage: 'none' },
        },
      },
    },
  });
}
