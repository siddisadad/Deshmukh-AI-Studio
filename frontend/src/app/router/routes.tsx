import { createBrowserRouter, Navigate } from 'react-router-dom';
import { AppShell } from '../layout/AppShell';
import { ForgotPasswordPage } from '../../features/auth/pages/ForgotPasswordPage';
import { LoginPage } from '../../features/auth/pages/LoginPage';
import { RegisterPage } from '../../features/auth/pages/RegisterPage';
import { DashboardPage } from '../../features/dashboard/pages/DashboardPage';
import { ProjectOverviewPage } from '../../features/projects/pages/ProjectOverviewPage';
import { ProjectSettingsPage } from '../../features/projects/pages/ProjectSettingsPage';
import { ProjectsPage } from '../../features/projects/pages/ProjectsPage';
import { RequirementsPage } from '../../features/requirements/pages/RequirementsPage';
import { TasksPage } from '../../features/tasks/pages/TasksPage';
import { ProfileSettingsPage } from '../../features/settings/pages/ProfileSettingsPage';
import { GuestRoute, ProtectedRoute } from './ProtectedRoute';

export const router = createBrowserRouter([
  {
    element: <GuestRoute />,
    children: [
      { path: '/login', element: <LoginPage /> },
      { path: '/register', element: <RegisterPage /> },
      { path: '/forgot-password', element: <ForgotPasswordPage /> },
    ],
  },
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <AppShell />,
        children: [
          { path: '/dashboard', element: <DashboardPage /> },
          { path: '/projects', element: <ProjectsPage /> },
          { path: '/projects/:projectId', element: <ProjectOverviewPage /> },
          { path: '/projects/:projectId/requirements', element: <RequirementsPage /> },
          { path: '/projects/:projectId/tasks', element: <TasksPage /> },
          { path: '/projects/:projectId/settings', element: <ProjectSettingsPage /> },
          { path: '/settings/profile', element: <ProfileSettingsPage /> },
        ],
      },
    ],
  },
  { path: '/', element: <Navigate to="/dashboard" replace /> },
  { path: '*', element: <Navigate to="/dashboard" replace /> },
]);
