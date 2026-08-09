import { Link } from 'react-router-dom';
import { MarketingShell } from '../components/MarketingShell';
import { usePageMeta } from '../hooks/usePageMeta';

const SERVICES = [
  {
    title: 'AI Studio',
    body: 'Our flagship product — an AI-powered engineering workspace for requirements, tasks, specialized assistants, and documentation under one project context.',
  },
  {
    title: 'Workspace deployment',
    body: 'Help getting AI Studio running for your team — staging dogfood, production-shaped compose, and org configuration that matches how you ship.',
  },
  {
    title: 'Engineering partnerships',
    body: 'Focused engagements where Deshmukh Technology helps define the SDLC loop, AI policy, and tooling so your team keeps ownership of every decision.',
  },
] as const;

export function ServicesPage() {
  usePageMeta({
    title: 'Services — Deshmukh Technology',
    description:
      'AI Studio, workspace deployment, and engineering partnerships from Deshmukh Technology.',
    path: '/services',
  });

  return (
    <MarketingShell>
      <section className="dt-page-hero" aria-labelledby="services-title">
        <p className="dt-section__label">Services</p>
        <h1 className="dt-page-hero__title" id="services-title">
          Product and partnership for engineering teams
        </h1>
        <p className="dt-section__copy">
          From the AI Studio workspace to deployment and focused delivery partnerships — one company
          behind the stack your team relies on.
        </p>
      </section>

      <section className="dt-section" aria-label="Service list">
        <ul className="dt-service-list">
          {SERVICES.map((service) => (
            <li key={service.title}>
              <h2>{service.title}</h2>
              <p>{service.body}</p>
            </li>
          ))}
        </ul>
      </section>

      <section className="dt-section dt-contact" aria-labelledby="services-cta-title">
        <div>
          <p className="dt-section__label">Next step</p>
          <h2 className="dt-section__title" id="services-cta-title">
            Try the product or talk with us
          </h2>
          <p className="dt-section__copy">
            Create an AI Studio account to explore the workspace, or reach out about deployment and
            partnership work.
          </p>
        </div>
        <div className="dt-hero__actions">
          <Link className="dt-btn dt-btn--primary" to="/register">
            Start with AI Studio
          </Link>
          <Link className="dt-btn dt-btn--ghost" to="/contact">
            Contact us
          </Link>
        </div>
      </section>
    </MarketingShell>
  );
}
