import { Box, CircularProgress } from '@mui/material';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { consumeAuthReturnPath, saveAuthReturnPath } from '../../features/auth/authReturnPath';
import { useAuthStore } from '../../features/auth/store/authStore';

export function ProtectedRoute() {
  const location = useLocation();
  const authStatus = useAuthStore((s) => s.authStatus);

  if (authStatus === 'unauthenticated') {
    saveAuthReturnPath(location);
    return <Navigate to="/login" replace />;
  }

  if (authStatus === 'unknown') {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
        <CircularProgress size={28} aria-label="Restoring session" />
      </Box>
    );
  }

  return <Outlet />;
}

export function GuestRoute() {
  const accessToken = useAuthStore((s) => s.accessToken);
  const refreshToken = useAuthStore((s) => s.refreshToken);
  const authStatus = useAuthStore((s) => s.authStatus);

  if (accessToken || (refreshToken && authStatus !== 'unauthenticated')) {
    return <Navigate to={consumeAuthReturnPath()} replace />;
  }

  return <Outlet />;
}
