import { describe, expect, it } from 'vitest';
import type { Task } from '../api/tasksApi';
import { applyTaskMove, resolveDropTarget, toReorderUpdates } from './applyTaskMove';

function task(partial: Partial<Task> & Pick<Task, 'id' | 'status' | 'sortOrder'>): Task {
  return {
    projectId: 'p1',
    title: partial.id,
    priority: 'MEDIUM',
    labels: [],
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    ...partial,
  };
}

const board: Task[] = [
  task({ id: 'a', status: 'TODO', sortOrder: 0 }),
  task({ id: 'b', status: 'TODO', sortOrder: 1 }),
  task({ id: 'c', status: 'IN_PROGRESS', sortOrder: 0 }),
];

describe('applyTaskMove', () => {
  it('moves a task across columns and renumbers', () => {
    const next = applyTaskMove(board, 'a', 'IN_PROGRESS', 0);
    expect(next.find((t) => t.id === 'a')).toMatchObject({ status: 'IN_PROGRESS', sortOrder: 0 });
    expect(next.find((t) => t.id === 'c')).toMatchObject({ status: 'IN_PROGRESS', sortOrder: 1 });
    expect(next.find((t) => t.id === 'b')).toMatchObject({ status: 'TODO', sortOrder: 0 });
  });

  it('reorders within the same column', () => {
    const next = applyTaskMove(board, 'a', 'TODO', 1);
    expect(next.find((t) => t.id === 'b')).toMatchObject({ sortOrder: 0 });
    expect(next.find((t) => t.id === 'a')).toMatchObject({ status: 'TODO', sortOrder: 1 });
  });
});

describe('resolveDropTarget', () => {
  const statuses = ['TODO', 'IN_PROGRESS', 'REVIEW', 'DONE'] as const;

  it('drops on a column appends to the end', () => {
    expect(resolveDropTarget(board, 'a', 'REVIEW', [...statuses])).toEqual({
      status: 'REVIEW',
      index: 0,
    });
    expect(resolveDropTarget(board, 'a', 'TODO', [...statuses])).toEqual({
      status: 'TODO',
      index: 1, // only b remains in TODO
    });
  });

  it('drops on a task inserts at that task index', () => {
    expect(resolveDropTarget(board, 'a', 'c', [...statuses])).toEqual({
      status: 'IN_PROGRESS',
      index: 0,
    });
  });
});

describe('toReorderUpdates', () => {
  it('maps task fields for the reorder API', () => {
    expect(toReorderUpdates([board[0]])).toEqual([
      { taskId: 'a', status: 'TODO', sortOrder: 0 },
    ]);
  });
});
