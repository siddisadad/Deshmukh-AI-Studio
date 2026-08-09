import { useState, type FormEvent } from 'react';
import { MarketingShell } from '../components/MarketingShell';
import { usePageMeta } from '../hooks/usePageMeta';
import { contactApi } from '../api/contactApi';
import { buildContactMailto, CONTACT_EMAIL } from '../lib/contact';
import { ApiError } from '../../../shared/api/types';

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
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitted, setSubmitted] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      await contactApi.createInquiry({ name, email, topic, message });
      setSubmitted(true);
      setName('');
      setEmail('');
      setTopic(TOPICS[0]);
      setMessage('');
    } catch (err) {
      // API unavailable or offline — fall back to the visitor's mail client.
      if (!(err instanceof ApiError) || err.status >= 500 || err.status === 0) {
        window.location.assign(buildContactMailto({ name, email, topic, message }));
        setSubmitted(true);
        return;
      }
      setError(err.message);
    } finally {
      setLoading(false);
    }
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
        {submitted ? (
          <div className="dt-form-success" role="status">
            <h2>Message received</h2>
            <p>
              Thanks — your inquiry is with the Deshmukh Technology team. We typically reply within two
              business days.
            </p>
            <button
              type="button"
              className="dt-btn dt-btn--ghost"
              onClick={() => setSubmitted(false)}
            >
              Send another message
            </button>
          </div>
        ) : (
          <form className="dt-form" onSubmit={(e) => void onSubmit(e)}>
            <label className="dt-field">
              <span>Name</span>
              <input
                name="name"
                type="text"
                required
                autoComplete="name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                disabled={loading}
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
                disabled={loading}
              />
            </label>
            <label className="dt-field">
              <span>Topic</span>
              <select
                name="topic"
                value={topic}
                onChange={(e) => setTopic(e.target.value)}
                disabled={loading}
              >
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
                disabled={loading}
              />
            </label>
            {error && (
              <p className="dt-form__error" role="alert">
                {error}
              </p>
            )}
            <button className="dt-btn dt-btn--primary" type="submit" disabled={loading}>
              {loading ? 'Sending…' : 'Send message'}
            </button>
          </form>
        )}

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
