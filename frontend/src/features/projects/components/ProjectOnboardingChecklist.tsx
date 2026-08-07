import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import RadioButtonUncheckedIcon from '@mui/icons-material/RadioButtonUnchecked';
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  LinearProgress,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  Paper,
  Stack,
  Typography,
} from '@mui/material';
import { useEffect, useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import {
  onboardingDismissKey,
  useProjectOnboarding,
  type ProjectOnboardingState,
} from '../hooks/useProjectOnboarding';

interface Step {
  key: keyof Pick<
    ProjectOnboardingState,
    'hasRequirement' | 'hasTask' | 'hasChat' | 'hasDocument'
  >;
  title: string;
  description: string;
  to: string;
  testId: string;
}

const STEPS = (projectId: string): Step[] => [
  {
    key: 'hasRequirement',
    title: 'Capture a requirement',
    description: 'Describe what the team needs to build.',
    to: `/projects/${projectId}/requirements`,
    testId: 'onboarding-step-requirements',
  },
  {
    key: 'hasTask',
    title: 'Create a Kanban task',
    description: 'Break work into trackable cards on the board.',
    to: `/projects/${projectId}/tasks`,
    testId: 'onboarding-step-tasks',
  },
  {
    key: 'hasChat',
    title: 'Chat with an assistant',
    description: 'Ask the BA or Developer assistant using shared project context.',
    to: `/projects/${projectId}/chat`,
    testId: 'onboarding-step-chat',
  },
  {
    key: 'hasDocument',
    title: 'Add a document',
    description: 'Store specs, runbooks, or generated documentation.',
    to: `/projects/${projectId}/documents`,
    testId: 'onboarding-step-documents',
  },
];

interface Props {
  projectId: string;
}

export function ProjectOnboardingChecklist({ projectId }: Props) {
  const { state, loading, error } = useProjectOnboarding(projectId);
  const [dismissed, setDismissed] = useState(false);

  useEffect(() => {
    try {
      setDismissed(localStorage.getItem(onboardingDismissKey(projectId)) === '1');
    } catch {
      setDismissed(false);
    }
  }, [projectId]);

  function dismiss() {
    try {
      localStorage.setItem(onboardingDismissKey(projectId), '1');
    } catch {
      // ignore private mode / quota errors
    }
    setDismissed(true);
  }

  if (dismissed || (state.isComplete && !loading)) {
    if (state.isComplete && !dismissed) {
      return (
        <Alert severity="success" data-testid="onboarding-complete">
          Workspace setup complete — requirements, tasks, chat, and documents are in place.
        </Alert>
      );
    }
    return null;
  }

  if (loading) {
    return (
      <Paper variant="outlined" sx={{ p: 3 }} data-testid="onboarding-checklist">
        <Stack spacing={2} sx={{ alignItems: 'center' }}>
          <CircularProgress size={28} />
          <Typography variant="body2" color="text.secondary">Loading setup checklist…</Typography>
        </Stack>
      </Paper>
    );
  }

  const steps = STEPS(projectId);
  const progress = state.totalSteps > 0 ? (state.completedCount / state.totalSteps) * 100 : 0;

  return (
    <Paper variant="outlined" sx={{ p: 3 }} data-testid="onboarding-checklist">
      <Stack spacing={2}>
        <Box>
          <Typography variant="h6" gutterBottom>First-run checklist</Typography>
          <Typography variant="body2" color="text.secondary">
            Complete these steps to get value from AI Studio without a guided tour.
          </Typography>
        </Box>

        {error && <Alert severity="warning">{error}</Alert>}

        <Box>
          <Stack direction="row" spacing={1} sx={{ alignItems: 'center', mb: 1 }}>
            <Typography variant="body2" color="text.secondary">
              {state.completedCount} of {state.totalSteps} complete
            </Typography>
          </Stack>
          <LinearProgress
            variant="determinate"
            value={progress}
            sx={{ height: 8, borderRadius: 1 }}
            data-testid="onboarding-progress"
          />
        </Box>

        <List disablePadding>
          {steps.map((step) => {
            const done = state[step.key];
            return (
              <ListItem
                key={step.key}
                disableGutters
                secondaryAction={
                  !done ? (
                    <Button
                      component={RouterLink}
                      to={step.to}
                      size="small"
                      variant="outlined"
                      data-testid={step.testId}
                    >
                      Go
                    </Button>
                  ) : undefined
                }
                data-testid={`onboarding-item-${step.key}`}
              >
                <ListItemIcon sx={{ minWidth: 36 }}>
                  {done ? (
                    <CheckCircleIcon color="success" fontSize="small" />
                  ) : (
                    <RadioButtonUncheckedIcon color="disabled" fontSize="small" />
                  )}
                </ListItemIcon>
                <ListItemText
                  primary={step.title}
                  secondary={step.description}
                  primaryTypographyProps={{ fontWeight: done ? 600 : 400 }}
                />
              </ListItem>
            );
          })}
        </List>

        <Stack direction="row" spacing={1} sx={{ justifyContent: 'flex-end' }}>
          <Button size="small" onClick={dismiss} data-testid="onboarding-dismiss">
            Dismiss
          </Button>
        </Stack>
      </Stack>
    </Paper>
  );
}
