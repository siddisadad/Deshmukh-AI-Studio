import { Link } from 'react-router-dom';
import { MarketingShell } from '../components/MarketingShell';
import { usePageMeta } from '../hooks/usePageMeta';

export function AboutPage() {
  usePageMeta({
    title: 'About — Deshmukh Technology',
    description:
      'Deshmukh Technology builds tools that keep human judgment at the center of software delivery.',
    path: '/about',
  });

  return (
    <MarketingShell>
      <section className="dt-page-hero" aria-labelledby="about-title">
        <p className="dt-section__label">About</p>
        <h1 className="dt-page-hero__title" id="about-title">
          Built for the way software actually ships
        </h1>
        <p className="dt-section__copy">
          Deshmukh Technology is an engineering company focused on tools that keep human judgment at the
          center — and give small teams the leverage of a larger software organization.
        </p>
      </section>

      <section className="dt-section" aria-labelledby="mission-title">
        <p className="dt-section__label">Mission</p>
        <h2 className="dt-section__title" id="mission-title">
          Clarity over noise
        </h2>
        <p className="dt-section__copy">
          Too many AI products add another chat window. We build shared project context — requirements,
          tasks, assistants, and documentation in one place — so teams move from idea to release without
          losing the thread.
        </p>
      </section>

      <section className="dt-section dt-contact" aria-labelledby="about-cta-title">
        <div>
          <p className="dt-section__label">Work with us</p>
          <h2 className="dt-section__title" id="about-cta-title">
            Start a conversation
          </h2>
          <p className="dt-section__copy">
            Whether you are evaluating AI Studio or exploring a custom engagement, we would like to hear
            what you are building.
          </p>
        </div>
        <div className="dt-hero__actions">
          <Link className="dt-btn dt-btn--primary" to="/contact">
            Contact us
          </Link>
          <Link className="dt-btn dt-btn--ghost" to="/services">
            View services
          </Link>
        </div>
      </section>
    </MarketingShell>
  );
}
