import { useEffect } from 'react';
import { Link } from 'react-router-dom';
import { MAIN_CONTENT_ID, SkipToContent } from '../../../shared/ui/SkipToContent';
import { useAuthStore } from '../../auth/store/authStore';
import '../styles/marketing.css';

const YEAR = new Date().getFullYear();

export function HomePage() {
  const accessToken = useAuthStore((s) => s.accessToken);
  const signedIn = Boolean(accessToken);

  useEffect(() => {
    const previous = document.title;
    document.title = 'Deshmukh Technology — Official Site';
    return () => {
      document.title = previous;
    };
  }, []);

  return (
    <div className="dt-site">
      <SkipToContent />
      <header className="dt-nav" aria-label="Primary">
        <a className="dt-nav__brand" href="#top">
          Deshmukh Technology
        </a>
        <nav>
          <ul className="dt-nav__links">
            <li>
              <a href="#product">Product</a>
            </li>
            <li>
              <a href="#approach">Approach</a>
            </li>
            <li>
              <a href="#contact">Contact</a>
            </li>
            <li>
              {signedIn ? (
                <Link className="dt-nav__cta" to="/dashboard">
                  Open workspace
                </Link>
              ) : (
                <Link className="dt-nav__cta" to="/login">
                  Sign in
                </Link>
              )}
            </li>
          </ul>
        </nav>
      </header>

      <main id={MAIN_CONTENT_ID}>
        <section className="dt-hero" id="top" aria-label="Deshmukh Technology">
          <div className="dt-hero__media" aria-hidden="true" />
          <div className="dt-hero__content">
            <h1 className="dt-brand">
              Deshmukh
              <span>Technology</span>
            </h1>
            <p className="dt-hero__headline">Engineering clarity for teams who ship software.</p>
            <p className="dt-hero__support">
              We build AI Studio — a shared workspace where specialized assistants help move work from idea to
              release without losing context.
            </p>
            <div className="dt-hero__actions">
              {signedIn ? (
                <Link className="dt-btn dt-btn--primary" to="/dashboard">
                  Continue to workspace
                </Link>
              ) : (
                <>
                  <Link className="dt-btn dt-btn--primary" to="/register">
                    Start with AI Studio
                  </Link>
                  <Link className="dt-btn dt-btn--ghost" to="/login">
                    Sign in
                  </Link>
                </>
              )}
            </div>
          </div>
        </section>

        <section className="dt-section" id="product" aria-labelledby="product-title">
          <p className="dt-section__label">Product</p>
          <h2 className="dt-section__title" id="product-title">
            AI Studio for software engineering
          </h2>
          <p className="dt-section__copy">
            Not a chatbot. Not an IDE. A project workspace where requirements, tasks, documentation, and
            role-aware assistants share one source of truth — so freelancers and small teams can work with the
            discipline of a full engineering org.
          </p>
        </section>

        <section className="dt-section" id="approach" aria-labelledby="approach-title">
          <p className="dt-section__label">Approach</p>
          <h2 className="dt-section__title" id="approach-title">
            One context through the full SDLC
          </h2>
          <p className="dt-section__copy">
            Humans stay in control of decisions. AI accelerates the craft around them — from clarifying
            requirements to drafting docs — always grounded in the same project.
          </p>
          <ol className="dt-flow">
            <li>
              <strong>Idea</strong>
              <span>Capture intent and constraints in a shared project.</span>
            </li>
            <li>
              <strong>Requirements</strong>
              <span>Shape stories and acceptance criteria with a BA assistant.</span>
            </li>
            <li>
              <strong>Build &amp; verify</strong>
              <span>Track work on a board while developer and QA assistants stay aligned.</span>
            </li>
            <li>
              <strong>Document</strong>
              <span>Produce release-ready artifacts without starting from a blank page.</span>
            </li>
          </ol>
        </section>

        <section className="dt-section dt-contact" id="contact" aria-labelledby="contact-title">
          <div>
            <p className="dt-section__label">Contact</p>
            <h2 className="dt-section__title" id="contact-title">
              Talk with Deshmukh Technology
            </h2>
            <p className="dt-section__copy">
              Interested in AI Studio for your team, a partnership, or a deployment conversation? Reach us and
              we will follow up.
            </p>
          </div>
          <div>
            <a className="dt-btn dt-btn--primary" href="mailto:hello@deshmukh.tech">
              Email hello@deshmukh.tech
            </a>
          </div>
        </section>
      </main>

      <footer className="dt-footer">
        <div className="dt-footer__inner">
          <span>© {YEAR} Deshmukh Technology</span>
          <span>Builders of AI Studio</span>
        </div>
      </footer>
    </div>
  );
}
