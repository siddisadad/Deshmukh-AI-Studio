import { Link } from 'react-router-dom';
import { MarketingShell } from '../components/MarketingShell';
import { usePageMeta } from '../hooks/usePageMeta';
import { CONTACT_EMAIL } from '../lib/contact';

export function TermsPage() {
  usePageMeta({
    title: 'Terms — Deshmukh Technology',
    description:
      'Terms of use for the Deshmukh Technology website and AI Studio product workspace.',
    path: '/terms',
  });

  return (
    <MarketingShell>
      <section className="dt-page-hero" aria-labelledby="terms-title">
        <p className="dt-section__label">Legal</p>
        <h1 className="dt-page-hero__title" id="terms-title">
          Terms of use
        </h1>
        <p className="dt-section__copy">
          These terms cover use of the Deshmukh Technology website and AI Studio. By using the site or
          creating an account, you agree to them.
        </p>
      </section>

      <section className="dt-section" aria-labelledby="terms-site-title">
        <h2 className="dt-section__title" id="terms-site-title">
          Website
        </h2>
        <p className="dt-section__copy">
          Marketing pages are provided for information about Deshmukh Technology and AI Studio. Content
          may change without notice. Do not misuse the contact form (spam, automated abuse, or false
          identity).
        </p>
      </section>

      <section className="dt-section" aria-labelledby="terms-product-title">
        <h2 className="dt-section__title" id="terms-product-title">
          AI Studio
        </h2>
        <p className="dt-section__copy">
          AI Studio is an engineering workspace. You are responsible for content you submit, how you use
          AI outputs, and compliance with your organization&apos;s policies. Service availability, plans,
          and features may evolve; paid plans are governed by the billing terms shown in-product.
        </p>
      </section>

      <section className="dt-section" aria-labelledby="terms-liability-title">
        <h2 className="dt-section__title" id="terms-liability-title">
          Liability
        </h2>
        <p className="dt-section__copy">
          The website and product are provided as available. To the fullest extent permitted by law,
          Deshmukh Technology is not liable for indirect or consequential damages arising from use of the
          site or AI Studio. Nothing here limits rights that cannot be waived under applicable law.
        </p>
      </section>

      <section className="dt-section" aria-labelledby="terms-contact-title">
        <h2 className="dt-section__title" id="terms-contact-title">
          Questions
        </h2>
        <p className="dt-section__copy">
          Contact us at <a href={`mailto:${CONTACT_EMAIL}`}>{CONTACT_EMAIL}</a> or via the{' '}
          <Link to="/contact">contact form</Link>. See also our <Link to="/privacy">privacy notice</Link>.
        </p>
      </section>
    </MarketingShell>
  );
}
