import type { ReactNode } from 'react';
import { Link, NavLink } from 'react-router-dom';
import { MAIN_CONTENT_ID, SkipToContent } from '../../../shared/ui/SkipToContent';
import { useAuthStore } from '../../auth/store/authStore';
import '../styles/marketing.css';

const YEAR = new Date().getFullYear();

export function MarketingShell({
  children,
  homeAnchors = false,
}: {
  children: ReactNode;
  homeAnchors?: boolean;
}) {
  const accessToken = useAuthStore((s) => s.accessToken);
  const signedIn = Boolean(accessToken);
  const navClassName = homeAnchors ? 'dt-nav' : 'dt-nav dt-nav--static';

  return (
    <div className="dt-site">
      <SkipToContent />
      <header className={navClassName} aria-label="Primary">
        <Link className="dt-nav__brand" to="/">
          Deshmukh Technology
        </Link>
        <nav>
          <ul className="dt-nav__links">
            <li>
              {homeAnchors ? (
                <a href="#product">Product</a>
              ) : (
                <NavLink to="/#product">Product</NavLink>
              )}
            </li>
            <li>
              <NavLink to="/services">Services</NavLink>
            </li>
            <li>
              <NavLink to="/about">About</NavLink>
            </li>
            <li>
              <NavLink to="/contact">Contact</NavLink>
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

      <main id={MAIN_CONTENT_ID}>{children}</main>

      <footer className="dt-footer">
        <div className="dt-footer__inner">
          <span>© {YEAR} Deshmukh Technology</span>
          <nav className="dt-footer__links" aria-label="Footer">
            <Link to="/privacy">Privacy</Link>
            <Link to="/contact">Contact</Link>
            <span>Builders of AI Studio</span>
          </nav>
        </div>
      </footer>
    </div>
  );
}
