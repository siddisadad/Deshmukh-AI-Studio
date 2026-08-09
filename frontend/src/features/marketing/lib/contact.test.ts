import { describe, expect, it } from 'vitest';
import { buildContactMailto, CONTACT_EMAIL } from './contact';

describe('buildContactMailto', () => {
  it('builds a mailto URL with encoded subject and body', () => {
    const href = buildContactMailto({
      name: 'Ada Lovelace',
      email: 'ada@example.com',
      topic: 'Partnership',
      message: 'Hello from Ada',
    });

    expect(href.startsWith(`mailto:${CONTACT_EMAIL}?`)).toBe(true);
    expect(href).toContain(encodeURIComponent('Deshmukh Technology — Partnership'));
    expect(href).toContain(encodeURIComponent('Name: Ada Lovelace'));
    expect(href).toContain(encodeURIComponent('Email: ada@example.com'));
    expect(href).toContain(encodeURIComponent('Hello from Ada'));
  });

  it('falls back to a general topic when empty', () => {
    const href = buildContactMailto({
      name: 'Ada',
      email: 'ada@example.com',
      topic: '  ',
      message: 'Hi',
    });
    expect(href).toContain(encodeURIComponent('Deshmukh Technology — General inquiry'));
  });
});
