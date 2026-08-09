import { expect, test } from '@playwright/test';

/**
 * Public Deshmukh Technology marketing site smoke (no auth).
 */
test.describe('official marketing site', () => {
  test('home hero and primary sections', async ({ page }) => {
    await page.goto('/');
    await expect(page.getByRole('heading', { level: 1, name: /Deshmukh\s*Technology/i })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Start with AI Studio' })).toBeVisible();
    await expect(page.getByRole('heading', { name: /AI Studio for software engineering/i })).toBeVisible();
  });

  test('about, services, contact, and privacy routes', async ({ page }) => {
    await page.goto('/about');
    await expect(page.getByRole('heading', { level: 1, name: /Built for the way software actually ships/i })).toBeVisible();

    await page.goto('/services');
    await expect(page.getByRole('heading', { level: 1, name: /Product and partnership/i })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'AI Studio' })).toBeVisible();

    await page.goto('/contact');
    await expect(page.getByRole('heading', { level: 1, name: /Talk with Deshmukh Technology/i })).toBeVisible();
    await expect(page.getByLabel('Name')).toBeVisible();
    await expect(page.getByLabel('Message')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Send message' })).toBeVisible();

    await page.goto('/privacy');
    await expect(page.getByRole('heading', { level: 1, name: 'Privacy' })).toBeVisible();

    await page.goto('/terms');
    await expect(page.getByRole('heading', { level: 1, name: 'Terms of use' })).toBeVisible();
  });

  test('nav links reach contact and auth', async ({ page }) => {
    await page.goto('/');
    await page.getByRole('navigation', { name: 'Primary' }).getByRole('link', { name: 'Contact' }).click();
    await expect(page).toHaveURL(/\/contact$/);

    await page.goto('/');
    await page.getByRole('navigation', { name: 'Primary' }).getByRole('link', { name: 'Sign in' }).click();
    await expect(page).toHaveURL(/\/login$/);
    await expect(page.getByRole('link', { name: /Deshmukh Technology/i })).toBeVisible();
  });
});
