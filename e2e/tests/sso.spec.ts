import { expect, test } from '@playwright/test';

/**
 * Mock SSO sign-in (Phase 6 growth).
 * Independent from smoke journey — uses mock provider redirect to SPA callback.
 */
test('mock SSO login completes and reaches dashboard', async ({ page }) => {
  const stamp = Date.now();
  const emailHint = `sso.e2e.${stamp}@example.com`;

  await page.goto('/login');
  await page.getByTestId('login-email').fill(emailHint);
  await page.getByTestId('login-sso-mock').click();

  await expect(page).toHaveURL(/\/auth\/sso\/callback/, { timeout: 15_000 });
  await expect(page).toHaveURL(/\/dashboard/, { timeout: 30_000 });
  await expect(page.getByTestId('nav-projects')).toBeVisible();
});
