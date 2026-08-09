import { http } from '../../../shared/api/httpClient';

export interface CreateContactInquiryPayload {
  name: string;
  email: string;
  topic: string;
  message: string;
}

export interface ContactInquiryResult {
  id: string;
}

export const contactApi = {
  createInquiry: (payload: CreateContactInquiryPayload) =>
    http.post<ContactInquiryResult>('/contact/inquiries', payload).then((r) => r.data),
};
