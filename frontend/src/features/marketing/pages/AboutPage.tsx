import { useEffect } from 'react';
import { Link } from 'react-router-dom';
import { MarketingShell } from '../components/MarketingShell';

export function AboutPage() {
  useEffect(() => {
    const previous = document.title;
    document.title = 'About — Deshmukh Technology';
    return () => {
      document.title = previous;
    };
  }, []);

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
          <a className="dt-btn dt-btn--primary" href="mailto:hello@deshmukh.tech">
            Email hello@deshmukh.tech
          </a>
          <Link className="dt-btn dt-btn--ghost" to="/services">
            View services
          </Link>
        </div>
      </section>
    </MarketingShell>
  );
}
