export const CONTACT_EMAIL = 'hello@deshmukh.tech';

export function buildContactMailto(input: {
  name: string;
  email: string;
  topic: string;
  message: string;
}): string {
  const name = input.name.trim();
  const email = input.email.trim();
  const topic = input.topic.trim() || 'General inquiry';
  const message = input.message.trim();

  const subject = `Deshmukh Technology — ${topic}`;
  const body = [
    `Name: ${name}`,
    `Email: ${email}`,
    `Topic: ${topic}`,
    '',
    message,
  ].join('\n');

  return `mailto:${CONTACT_EMAIL}?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(body)}`;
}
