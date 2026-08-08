import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '../../../shared/api/types';
import { chatApi, createDeltaBatcher, parseSseStream } from './chatApi';

vi.mock('../../../shared/api/httpClient', () => ({
  http: {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
  refreshAccessToken: vi.fn(),
}));

vi.mock('../../auth/store/authStore', () => ({
  useAuthStore: {
    getState: () => ({ accessToken: 'test-token' }),
  },
}));

import { http, refreshAccessToken } from '../../../shared/api/httpClient';

function sseBody(events: string): ReadableStream<Uint8Array> {
  const encoder = new TextEncoder();
  return new ReadableStream({
    start(controller) {
      controller.enqueue(encoder.encode(events));
      controller.close();
    },
  });
}

function failingStreamAfterFirstChunk(events: string): ReadableStream<Uint8Array> {
  const encoder = new TextEncoder();
  let sent = false;
  return new ReadableStream({
    pull(controller) {
      if (!sent) {
        sent = true;
        controller.enqueue(encoder.encode(events));
      } else {
        controller.error(new TypeError('connection lost'));
      }
    },
  });
}

describe('parseSseStream', () => {
  it('dispatches user, delta, done, and error events', async () => {
    const user = { id: 'u1', sender: 'USER', content: 'hi', createdAt: '2026-01-01T00:00:00Z' };
    const assistant = {
      id: 'a1',
      sender: 'ASSISTANT',
      content: 'hello',
      createdAt: '2026-01-01T00:00:01Z',
    };
    const body = [
      'event: user',
      `data: ${JSON.stringify(user)}`,
      '',
      'event: delta',
      'data: {"text":"hel"}',
      '',
      'event: delta',
      'data: {"text":"lo"}',
      '',
      'event: done',
      `data: ${JSON.stringify({
        assistantMessage: assistant,
        provider: 'mock',
        model: 'mock-v1',
        usage: { inputTokens: 12, outputTokens: 8, streamChars: 5, deltaCount: 2 },
      })}`,
      '',
    ].join('\n');

    const deltas: string[] = [];
    let done: unknown = null;

    await parseSseStream(
      { body: sseBody(body), ok: true } as Response,
      {
        onDelta: (text) => deltas.push(text),
        onDone: (result) => {
          done = result;
        },
      },
    );

    expect(deltas).toEqual(['hel', 'lo']);
    expect(done).toEqual({
      assistantMessage: assistant,
      provider: 'mock',
      model: 'mock-v1',
      usage: { inputTokens: 12, outputTokens: 8, streamChars: 5, deltaCount: 2 },
    });
  });
});

describe('createDeltaBatcher', () => {
  it('flushes accumulated deltas on the next animation frame', async () => {
    const rafCallbacks: FrameRequestCallback[] = [];
    vi.stubGlobal('requestAnimationFrame', (cb: FrameRequestCallback) => {
      rafCallbacks.push(cb);
      return rafCallbacks.length;
    });

    const flushed: string[] = [];
    const batcher = createDeltaBatcher((text) => flushed.push(text));
    batcher('a');
    batcher('b');
    batcher('c');

    expect(flushed).toEqual([]);
    rafCallbacks.forEach((cb) => cb(0));
    expect(flushed).toEqual(['abc']);

    vi.unstubAllGlobals();
  });
});

describe('chatApi.streamMessage', () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    vi.stubGlobal('fetch', fetchMock);
    fetchMock.mockReset();
    vi.mocked(http.get).mockReset();
    vi.mocked(refreshAccessToken).mockReset();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('retries on transient network failure before user event', async () => {
    fetchMock
      .mockRejectedValueOnce(new TypeError('network down'))
      .mockResolvedValueOnce({
        ok: true,
        body: sseBody(
          [
            'event: user',
            'data: {"id":"u1","sender":"USER","content":"q","createdAt":"2026-01-01T00:00:00Z"}',
            '',
            'event: done',
            'data: {"assistantMessage":{"id":"a1","sender":"ASSISTANT","content":"ok","createdAt":"2026-01-01T00:00:01Z"},"provider":"mock","model":"mock-v1"}',
            '',
          ].join('\n'),
        ),
      });

    const reconnects: string[] = [];
    await chatApi.streamMessage(
      'conv-1',
      'question',
      {},
      {
        maxRetries: 2,
        onReconnecting: (_attempt, reason) => reconnects.push(reason),
      },
    );

    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(reconnects).toEqual(['network']);
  });

  it('refreshes token and retries on 401', async () => {
    vi.mocked(refreshAccessToken).mockResolvedValue('fresh-token');
    fetchMock
      .mockResolvedValueOnce({
        ok: false,
        status: 401,
        json: async () => ({ message: 'expired' }),
      })
      .mockResolvedValueOnce({
        ok: true,
        body: sseBody(
          [
            'event: user',
            'data: {"id":"u1","sender":"USER","content":"q","createdAt":"2026-01-01T00:00:00Z"}',
            '',
            'event: done',
            'data: {"assistantMessage":{"id":"a1","sender":"ASSISTANT","content":"ok","createdAt":"2026-01-01T00:00:01Z"},"provider":"mock","model":"mock-v1"}',
            '',
          ].join('\n'),
        ),
      });

    await chatApi.streamMessage('conv-1', 'question', {}, { maxRetries: 2 });

    expect(refreshAccessToken).toHaveBeenCalledOnce();
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });

  it('polls conversation when stream drops after user event', async () => {
    const assistant = {
      id: 'a1',
      sender: 'ASSISTANT' as const,
      content: 'recovered reply',
      createdAt: '2026-01-01T00:00:01Z',
    };
    fetchMock.mockResolvedValueOnce({
      ok: true,
      body: failingStreamAfterFirstChunk(
        [
          'event: user',
          'data: {"id":"u1","sender":"USER","content":"q","createdAt":"2026-01-01T00:00:00Z"}',
          '',
        ].join('\n'),
      ),
    });
    vi.mocked(http.get).mockResolvedValue({
      data: {
        id: 'conv-1',
        projectId: 'p1',
        assistantRole: 'BUSINESS_ANALYST',
        title: null,
        messages: [
          { id: 'u1', sender: 'USER', content: 'q', createdAt: '2026-01-01T00:00:00Z' },
          assistant,
        ],
      },
    });

    let doneAssistant: string | null = null;
    await chatApi.streamMessage('conv-1', 'question', {
      onDone: ({ assistantMessage }) => {
        doneAssistant = assistantMessage.content;
      },
    });

    expect(doneAssistant).toBe('recovered reply');
    expect(http.get).toHaveBeenCalled();
  });

  it('aborts in-flight stream when signal is aborted', async () => {
    const controller = new AbortController();
    fetchMock.mockImplementation((_url: string, init?: RequestInit) =>
      new Promise((_, reject) => {
        init?.signal?.addEventListener('abort', () => {
          reject(new DOMException('Aborted', 'AbortError'));
        }, { once: true });
      }),
    );
    controller.abort();

    await expect(
      chatApi.streamMessage('conv-1', 'question', {}, { signal: controller.signal }),
    ).rejects.toMatchObject({ name: 'AbortError' });
  });

  it('throws ApiError when retries are exhausted', async () => {
    fetchMock.mockResolvedValue({
      ok: false,
      status: 500,
      json: async () => ({ message: 'server error' }),
    });

    await expect(
      chatApi.streamMessage('conv-1', 'question', {}, { maxRetries: 0 }),
    ).rejects.toBeInstanceOf(ApiError);
  });
});
