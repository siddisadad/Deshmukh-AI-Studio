import { useState, type FormEvent } from 'react';
import { MarketingShell } from '../components/MarketingShell';
import { usePageMeta } from '../hooks/usePageMeta';
import { buildContactMailto, CONTACT_EMAIL } from '../lib/contact';

const TOPICS = ['AI Studio', 'Deployment', 'Partnership', 'General inquiry'] as const;

export function ContactPage() {
  usePageMeta({
    title: 'Contact — Deshmukh Technology',
    description:
      'Contact Deshmukh Technology about AI Studio, deployment, or partnership conversations.',
    path: '/contact',
  });

  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [topic, setTopic] = useState<string>(TOPICS[0]);
  const [message, setMessage] = useState('');
  const [submitted, setSubmitted] = useState(false);

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    const href = buildContactMailto({ name, email, topic, message });
    setSubmitted(true);
    window.location.assign(href);
  }

  return (
    <MarketingShell>
      <section className="dt-page-hero" aria-labelledby="contact-page-title">
        <p className="dt-section__label">Contact</p>
        <h1 className="dt-page-hero__title" id="contact-page-title">
          Talk with Deshmukh Technology
        </h1>
        <p className="dt-section__copy">
          Tell us what you are exploring — AI Studio, deployment, or a partnership. We will follow up
          from {CONTACT_EMAIL}.
        </p>
      </section>

      <section className="dt-section dt-contact-layout" aria-label="Contact form">
        <form className="dt-form" onSubmit={onSubmit} noValidate={false}>
          <label className="dt-field">
            <span>Name</span>
            <input
              name="name"
              type="text"
              required
              autoComplete="name"
              value={name}
              onChange={(e) => setName(e.target.value)}
            />
          </label>
          <label className="dt-field">
            <span>Email</span>
            <input
              name="email"
              type="email"
              required
              autoComplete="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </label>
          <label className="dt-field">
            <span>Topic</span>
            <select name="topic" value={topic} onChange={(e) => setTopic(e.target.value)}>
              {TOPICS.map((item) => (
                <option key={item} value={item}>
                  {item}
                </option>
              ))}
            </select>
          </label>
          <label className="dt-field">
            <span>Message</span>
            <textarea
              name="message"
              required
              rows={6}
              value={message}
              onChange={(e) => setMessage(e.target.value)}
            />
          </label>
          <button className="dt-btn dt-btn--primary" type="submit">
            Send message
          </button>
          {submitted && (
            <p className="dt-form__note" role="status">
              Opening your email client to send the message…
            </p>
          )}
        </form>

        <aside className="dt-contact-aside">
          <h2>Prefer email?</h2>
          <p>
            Write us directly at{' '}
            <a href={`mailto:${CONTACT_EMAIL}`}>{CONTACT_EMAIL}</a>. We typically reply within two
            business days.
          </p>
        </aside>
      </section>
    </MarketingShell>
  );
}
