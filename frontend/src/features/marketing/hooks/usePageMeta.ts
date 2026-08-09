import { useEffect } from 'react';
import { absoluteSiteUrl, getPublicSiteOrigin } from '../lib/siteUrl';

function upsertMeta(selector: string, attributes: Record<string, string>) {
  let el = document.head.querySelector(selector) as HTMLMetaElement | null;
  if (!el) {
    el = document.createElement('meta');
    document.head.appendChild(el);
  }
  for (const [key, value] of Object.entries(attributes)) {
    el.setAttribute(key, value);
  }
  return el;
}

function upsertCanonical(href: string) {
  let el = document.head.querySelector('link[rel="canonical"]') as HTMLLinkElement | null;
  if (!el) {
    el = document.createElement('link');
    el.setAttribute('rel', 'canonical');
    document.head.appendChild(el);
  }
  el.setAttribute('href', href);
}

/**
 * Sets document title + description/OG/Twitter/canonical tags for marketing pages.
 */
export function usePageMeta({
  title,
  description,
  path = '/',
  image = '/marketing/hero-workspace.webp',
}: {
  title: string;
  description: string;
  path?: string;
  image?: string;
}) {
  useEffect(() => {
    const previousTitle = document.title;
    document.title = title;

    const origin = getPublicSiteOrigin();
    const url = absoluteSiteUrl(path);
    const imageUrl = image.startsWith('http') ? image : `${origin}${image}`;

    upsertCanonical(url);
    upsertMeta('meta[name="description"]', { name: 'description', content: description });
    upsertMeta('meta[property="og:title"]', { property: 'og:title', content: title });
    upsertMeta('meta[property="og:description"]', { property: 'og:description', content: description });
    upsertMeta('meta[property="og:type"]', { property: 'og:type', content: 'website' });
    upsertMeta('meta[property="og:url"]', { property: 'og:url', content: url });
    upsertMeta('meta[property="og:image"]', { property: 'og:image', content: imageUrl });
    upsertMeta('meta[name="twitter:card"]', { name: 'twitter:card', content: 'summary_large_image' });
    upsertMeta('meta[name="twitter:title"]', { name: 'twitter:title', content: title });
    upsertMeta('meta[name="twitter:description"]', {
      name: 'twitter:description',
      content: description,
    });
    upsertMeta('meta[name="twitter:image"]', { name: 'twitter:image', content: imageUrl });

    return () => {
      document.title = previousTitle;
    };
  }, [title, description, path, image]);
}
