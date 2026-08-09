import { Link } from 'react-router-dom';
import { MarketingShell } from '../components/MarketingShell';
import { usePageMeta } from '../hooks/usePageMeta';
import { CONTACT_EMAIL } from '../lib/contact';

export function PrivacyPage() {
  usePageMeta({
    title: 'Privacy — Deshmukh Technology',
    description:
      'How Deshmukh Technology handles information submitted through the official site and AI Studio.',
    path: '/privacy',
  });

  return (
    <MarketingShell>
      <section className="dt-page-hero" aria-labelledby="privacy-title">
        <p className="dt-section__label">Legal</p>
        <h1 className="dt-page-hero__title" id="privacy-title">
          Privacy
        </h1>
        <p className="dt-section__copy">
          This notice explains what Deshmukh Technology collects on the official website and how contact
          inquiries are handled. AI Studio product accounts have additional in-app privacy controls.
        </p>
      </section>

      <section className="dt-section" aria-labelledby="privacy-site-title">
        <h2 className="dt-section__title" id="privacy-site-title">
          Website and contact form
        </h2>
        <p className="dt-section__copy">
          When you use the contact form, we store your name, email, topic, message, and a coarse source
          IP to prevent abuse. We use that information only to respond to your inquiry and operate the
          site securely. Messages are delivered to our team mailbox and retained in our application
          database.
        </p>
      </section>

      <section className="dt-section" aria-labelledby="privacy-ai-title">
        <h2 className="dt-section__title" id="privacy-ai-title">
          AI Studio accounts
        </h2>
        <p className="dt-section__copy">
          If you create an AI Studio workspace, account data (such as email, organization membership,
          project content, and usage needed for billing or security) is processed to provide the
          service. Provider routing and retention settings may apply inside your organization.
        </p>
      </section>

      <section className="dt-section" aria-labelledby="privacy-rights-title">
        <h2 className="dt-section__title" id="privacy-rights-title">
          Requests
        </h2>
        <p className="dt-section__copy">
          To ask a question about stored contact inquiries or account data, email{' '}
          <a href={`mailto:${CONTACT_EMAIL}`}>{CONTACT_EMAIL}</a> or use the{' '}
          <Link to="/contact">contact form</Link>.
        </p>
      </section>
    </MarketingShell>
  );
}
