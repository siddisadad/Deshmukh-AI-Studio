import type { Task, TaskStatus } from '../api/tasksApi';

export interface ReorderUpdate {
  taskId: string;
  status: TaskStatus;
  sortOrder: number;
}

const STATUSES: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'REVIEW', 'DONE'];

function sortTasks(tasks: Task[]): Task[] {
  return [...tasks].sort((a, b) => a.sortOrder - b.sortOrder || a.createdAt.localeCompare(b.createdAt));
}

/** Move a task to a status/index and renumber sortOrder within affected columns. */
export function applyTaskMove(
  tasks: Task[],
  taskId: string,
  toStatus: TaskStatus,
  toIndex: number,
): Task[] {
  const moving = tasks.find((t) => t.id === taskId);
  if (!moving) {
    return tasks;
  }

  const fromStatus = moving.status;
  const without = tasks.filter((t) => t.id !== taskId);
  const destination = sortTasks(without.filter((t) => t.status === toStatus));
  const clamped = Math.max(0, Math.min(toIndex, destination.length));
  const nextDest = [
    ...destination.slice(0, clamped),
    { ...moving, status: toStatus },
    ...destination.slice(clamped),
  ].map((task, index) => ({ ...task, sortOrder: index }));

  const next: Task[] = [];
  for (const status of STATUSES) {
    if (status === toStatus) {
      next.push(...nextDest);
    } else if (status === fromStatus) {
      next.push(
        ...sortTasks(without.filter((t) => t.status === fromStatus)).map((task, index) => ({
          ...task,
          sortOrder: index,
        })),
      );
    } else {
      next.push(...sortTasks(without.filter((t) => t.status === status)));
    }
  }
  return next;
}

/** Resolve drop target: column id or task id → status + index. */
export function resolveDropTarget(
  tasks: Task[],
  activeTaskId: string,
  overId: string,
  columnStatuses: TaskStatus[],
): { status: TaskStatus; index: number } | null {
  const active = tasks.find((t) => t.id === activeTaskId);
  if (!active) {
    return null;
  }

  if ((columnStatuses as string[]).includes(overId)) {
    const status = overId as TaskStatus;
    const count = tasks.filter((t) => t.status === status && t.id !== activeTaskId).length;
    return { status, index: count };
  }

  const overTask = tasks.find((t) => t.id === overId);
  if (!overTask) {
    return null;
  }

  const columnTasks = sortTasks(tasks.filter((t) => t.status === overTask.status && t.id !== activeTaskId));
  const index = columnTasks.findIndex((t) => t.id === overId);
  return {
    status: overTask.status,
    index: index < 0 ? columnTasks.length : index,
  };
}

export function toReorderUpdates(tasks: Task[]): ReorderUpdate[] {
  return tasks.map((task) => ({
    taskId: task.id,
    status: task.status,
    sortOrder: task.sortOrder,
  }));
}
