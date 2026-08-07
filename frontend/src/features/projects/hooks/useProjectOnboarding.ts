import { useCallback, useEffect, useState } from 'react';
import { chatApi } from '../../chat/api/chatApi';
import { documentsApi } from '../../documents/api/documentsApi';
import { requirementsApi } from '../../requirements/api/requirementsApi';
import { tasksApi } from '../../tasks/api/tasksApi';

export interface ProjectOnboardingState {
  hasRequirement: boolean;
  hasTask: boolean;
  hasChat: boolean;
  hasDocument: boolean;
  completedCount: number;
  totalSteps: number;
  isComplete: boolean;
}

const TOTAL_STEPS = 4;

const EMPTY: ProjectOnboardingState = {
  hasRequirement: false,
  hasTask: false,
  hasChat: false,
  hasDocument: false,
  completedCount: 0,
  totalSteps: TOTAL_STEPS,
  isComplete: false,
};

export function useProjectOnboarding(projectId: string | undefined) {
  const [state, setState] = useState<ProjectOnboardingState>(EMPTY);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    if (!projectId) {
      setState(EMPTY);
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const [requirements, tasks, documents, conversations] = await Promise.all([
        requirementsApi.list(projectId),
        tasksApi.list(projectId),
        documentsApi.list(projectId),
        chatApi.listConversations(projectId),
      ]);

      const hasRequirement = requirements.length > 0;
      const hasTask = tasks.length > 0;
      const hasDocument = documents.length > 0;
      const hasChat = conversations.some((c) => c.messageCount > 0);
      const completedCount = [hasRequirement, hasTask, hasChat, hasDocument].filter(Boolean).length;

      setState({
        hasRequirement,
        hasTask,
        hasChat,
        hasDocument,
        completedCount,
        totalSteps: TOTAL_STEPS,
        isComplete: completedCount === TOTAL_STEPS,
      });
    } catch {
      setError('Could not load onboarding progress');
      setState(EMPTY);
    } finally {
      setLoading(false);
    }
  }, [projectId]);

  useEffect(() => {
    void load();
  }, [load]);

  return { state, loading, error, reload: load };
}

export function onboardingDismissKey(projectId: string) {
  return `aistudio.onboarding.dismissed.${projectId}`;
}
