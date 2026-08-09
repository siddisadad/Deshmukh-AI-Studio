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

export const contactInboxApi = {
  access: () => http.get<{ canAccessInbox: boolean }>('/contact/access').then((r) => r.data),
  list: () => http.get<ContactInquiryItem[]>('/contact/inquiries').then((r) => r.data),
  markRead: (id: string) =>
    http.post<ContactInquiryItem>(`/contact/inquiries/${id}/read`).then((r) => r.data),
};
