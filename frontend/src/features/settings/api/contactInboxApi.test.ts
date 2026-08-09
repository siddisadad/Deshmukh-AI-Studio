import { describe, expect, it } from 'vitest';
import { buildInquiryReplyMailto } from './contactInboxApi';

describe('buildInquiryReplyMailto', () => {
  it('builds a reply mailto with original message context', () => {
    const href = buildInquiryReplyMailto({
      email: 'ada@example.com',
      name: 'Ada',
      topic: 'Partnership',
      message: 'Hello there',
    });
    expect(href.startsWith('mailto:ada%40example.com?')).toBe(true);
    expect(href).toContain(encodeURIComponent('Re: Deshmukh Technology — Partnership'));
    expect(href).toContain(encodeURIComponent('Original message:'));
    expect(href).toContain(encodeURIComponent('Hello there'));
  });
});
