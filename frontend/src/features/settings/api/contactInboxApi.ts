import { http } from '../../../shared/api/httpClient';

export interface ContactInquiryItem {
  id: string;
  name: string;
  email: string;
  topic: string;
  message: string;
  sourceIp: string | null;
  createdAt: string;
  readAt: string | null;
}

export interface ContactInboxAccess {
  canAccessInbox: boolean;
  unreadCount: number;
}

export function buildInquiryReplyMailto(item: Pick<ContactInquiryItem, 'email' | 'name' | 'topic' | 'message'>): string {
  const subject = `Re: Deshmukh Technology — ${item.topic}`;
  const body = [
    `Hi ${item.name},`,
    '',
    '',
    '—',
    'Original message:',
    item.message,
  ].join('\n');
  return `mailto:${encodeURIComponent(item.email)}?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(body)}`;
}

export const contactInboxApi = {
  access: () => http.get<ContactInboxAccess>('/contact/access').then((r) => r.data),
  list: () => http.get<ContactInquiryItem[]>('/contact/inquiries').then((r) => r.data),
  markRead: (id: string) =>
    http.post<ContactInquiryItem>(`/contact/inquiries/${id}/read`).then((r) => r.data),
};
