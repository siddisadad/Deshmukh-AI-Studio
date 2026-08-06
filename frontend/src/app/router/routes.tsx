import { createBrowserRouter, Navigate } from 'react-router-dom';
import { AppShell } from '../layout/AppShell';
import { ForgotPasswordPage } from '../../features/auth/pages/ForgotPasswordPage';
import { LoginPage } from '../../features/auth/pages/LoginPage';
import { RegisterPage } from '../../features/auth/pages/RegisterPage';
import { SsoCallbackPage } from '../../features/auth/pages/SsoCallbackPage';
import { DashboardPage } from '../../features/dashboard/pages/DashboardPage';
import { ProjectOverviewPage } from '../../features/projects/pages/ProjectOverviewPage';
import { ProjectSettingsPage } from '../../features/projects/pages/ProjectSettingsPage';
import { ProjectsPage } from '../../features/projects/pages/ProjectsPage';
import { RequirementsPage } from '../../features/requirements/pages/RequirementsPage';
import { TasksPage } from '../../features/tasks/pages/TasksPage';
import { AiChatPage } from '../../features/chat/pages/AiChatPage';
import { DocumentsPage } from '../../features/documents/pages/DocumentsPage';
import { ProfileSettingsPage } from '../../features/settings/pages/ProfileSettingsPage';
import { BillingSettingsPage } from '../../features/settings/pages/BillingSettingsPage';
import { GuestRoute, ProtectedRoute } from './ProtectedRoute';

export const router = createBrowserRouter([
  {
    element: <GuestRoute />,
    children: [
      { path: '/login', element: <LoginPage /> },
      { path: '/register', element: <RegisterPage /> },
      { path: '/forgot-password', element: <ForgotPasswordPage /> },
      { path: '/auth/sso/callback', element: <SsoCallbackPage /> },
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
          { path: '/projects/:projectId/chat', element: <AiChatPage /> },
          { path: '/projects/:projectId/documents', element: <DocumentsPage /> },
          { path: '/projects/:projectId/settings', element: <ProjectSettingsPage /> },
          { path: '/settings/profile', element: <ProfileSettingsPage /> },
          { path: '/settings/billing', element: <BillingSettingsPage /> },
        ],
      },
    ],
  },
  { path: '/', element: <Navigate to="/dashboard" replace /> },
  { path: '*', element: <Navigate to="/dashboard" replace /> },
]);
