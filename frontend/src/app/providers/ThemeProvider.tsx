import { CssBaseline, ThemeProvider as MuiThemeProvider } from '@mui/material';
import { useMemo, type ReactNode } from 'react';
import { useAuthStore } from '../../features/auth/store/authStore';
import { createAppTheme } from '../../shared/theme/theme';

export function ThemeProvider({ children }: { children: ReactNode }) {
  const themePref = useAuthStore((s) => s.user?.theme) || 'SYSTEM';
  const mode = useMemo(() => {
    if (themePref === 'LIGHT') return 'light';
    if (themePref === 'DARK') return 'dark';
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  }, [themePref]);

  const theme = useMemo(() => createAppTheme(mode), [mode]);

  return (
    <MuiThemeProvider theme={theme}>
      <CssBaseline />
      {children}
    </MuiThemeProvider>
  );
}
