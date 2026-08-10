import { expect, test, type BrowserContext, type Page } from '@playwright/test';

/**
 * Phase 47 — org git sync filter URL sharing (docs/115-ORG-GIT-SYNC-FILTER-URL-E2E-GUIDE.md)
 */
test.describe.configure({ mode: 'serial' });

const stamp = Date.now();
const email = `e2e.git.url.${stamp}@example.com`;
const password = 'TestPass1234';

async function grantClipboard(context: BrowserContext) {
  await context.grantPermissions(['clipboard-read', 'clipboard-write']);
}

async function register(page: Page) {
  await page.goto('/register');
  await page.getByTestId('register-display-name').fill(`Git URL E2E ${stamp}`);
  await page.getByTestId('register-email').fill(email);
  await page.getByTestId('register-password').fill(password);
  await page.getByTestId('register-submit').click();
  await expect(page).toHaveURL(/\/dashboard/);
}

async function waitForGitOverview(page: Page) {
  await expect(page.getByTestId('git-sync-overview')).toBeVisible({ timeout: 45_000 });
}

test('org git sync filter URLs apply and copy links round-trip', async ({ page, context }) => {
  test.setTimeout(180_000);

  await grantClipboard(context);
  await register(page);

  await page.goto(
    '/settings/git?linked=linked&lastSync=failed&runSource=manual&runStatus=failed#org-git-sync-runs'
  );
  await expect(page).not.toHaveURL(/\/login/, { timeout: 20_000 });
  await expect(page).toHaveURL(/linked=linked/);
  await expect(page).toHaveURL(/lastSync=failed/);
  await expect(page).toHaveURL(/runSource=manual/);
  await expect(page).toHaveURL(/#org-git-sync-runs/);

  await waitForGitOverview(page);
  await expect(page.getByTestId('git-sync-page-filter-toolbar')).toBeVisible();
  await expect(page.getByTestId('git-sync-overview-active-filter-linked')).toBeVisible();
  await expect(page.getByTestId('git-sync-overview-active-filter-lastSync')).toBeVisible();
  await expect(page.getByTestId('org-git-sync-runs-active-filter-runSource')).toBeVisible();
  await expect(page.getByTestId('org-git-sync-runs-active-filter-runStatus')).toBeVisible();
  await expect(page.getByTestId('org-git-sync-runs')).toBeVisible();

  await page.getByTestId('git-sync-overview-copy-filter-link').click();
  await expect(page.getByText('Filtered overview link copied to clipboard')).toBeVisible();
  const overviewLink = await page.evaluate(() => navigator.clipboard.readText());
  expect(overviewLink).toContain('linked=linked');
  expect(overviewLink).toContain('lastSync=failed');

  await page.getByTestId('git-sync-page-copy-filter-link').click();
  await expect(page.getByText('Page link with overview and run filters copied to clipboard')).toBeVisible();
  const pageLink = await page.evaluate(() => navigator.clipboard.readText());
  expect(pageLink).toContain('linked=linked');
  expect(pageLink).toContain('runSource=manual');
  expect(pageLink).toContain('#org-git-sync-runs');

  await page.goto(pageLink);
  await waitForGitOverview(page);
  await expect(page.getByTestId('git-sync-overview-active-filter-linked')).toBeVisible();
  await expect(page.getByTestId('org-git-sync-runs-active-filter-runSource')).toBeVisible();

  await page.getByTestId('git-sync-page-clear-all-filters').click();
  await expect(page.getByTestId('git-sync-page-filter-toolbar')).toHaveCount(0);
});

test('run filter URL applies on first visit to git settings', async ({ browser }) => {
  test.setTimeout(120_000);

  const context = await browser.newContext();
  await grantClipboard(context);
  const page = await context.newPage();
  const runStamp = Date.now();
  const runEmail = `e2e.git.run.url.${runStamp}@example.com`;

  await page.goto('/register');
  await page.getByTestId('register-display-name').fill(`Git Run URL ${runStamp}`);
  await page.getByTestId('register-email').fill(runEmail);
  await page.getByTestId('register-password').fill(password);
  await page.getByTestId('register-submit').click();
  await expect(page).toHaveURL(/\/dashboard/);

  await page.goto('/settings/git?runSource=scheduled&runStatus=failed#org-git-sync-runs');
  await expect(page.getByTestId('org-git-sync-runs-active-filter-runSource')).toBeVisible();
  await expect(page.getByTestId('org-git-sync-runs-active-filter-runStatus')).toBeVisible();

  await context.close();
});
