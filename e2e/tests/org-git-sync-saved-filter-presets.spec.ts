import { expect, test, type Page } from '@playwright/test';

/**
 * Phase 49 — org git sync saved filter presets (docs/117-ORG-GIT-SYNC-SAVED-FILTER-PRESETS-E2E-GUIDE.md)
 */
test.describe.configure({ mode: 'serial' });

const password = 'TestPass1234';

async function register(page: Page, stamp: number, label: string) {
  const email = `e2e.git.preset.${stamp}@example.com`;
  await page.goto('/register');
  await page.getByTestId('register-display-name').fill(label);
  await page.getByTestId('register-email').fill(email);
  await page.getByTestId('register-password').fill(password);
  await page.getByTestId('register-submit').click();
  await expect(page).toHaveURL(/\/dashboard/);
}

async function waitForGitOverview(page: Page) {
  await expect(page.getByTestId('git-sync-overview')).toBeVisible({ timeout: 45_000 });
}

async function saveOverviewPreset(page: Page, name: string, shareWithOrg = false) {
  await page.getByTestId('git-sync-overview-save-preset').click();
  await expect(page.getByTestId('git-sync-save-preset-dialog')).toBeVisible();
  await page.getByTestId('git-sync-save-preset-name-input').fill(name);
  if (shareWithOrg) {
    await page.getByTestId('git-sync-save-preset-share-org').check();
  }
  await page.getByTestId('git-sync-save-preset-confirm').click();
}

async function saveRunPreset(page: Page, name: string, shareWithOrg = false) {
  await page.getByTestId('org-git-sync-runs-save-preset').click();
  await expect(page.getByTestId('git-sync-save-preset-dialog')).toBeVisible();
  await page.getByTestId('git-sync-save-preset-name-input').fill(name);
  if (shareWithOrg) {
    await page.getByTestId('git-sync-save-preset-share-org').check();
  }
  await page.getByTestId('git-sync-save-preset-confirm').click();
}

test('overview saved filter preset save apply and delete', async ({ page }) => {
  test.setTimeout(180_000);

  const stamp = Date.now();
  const presetName = `E2E Overview ${stamp}`;

  await register(page, stamp, `Git Preset E2E ${stamp}`);
  await page.goto('/settings/git');
  await waitForGitOverview(page);

  await page.getByTestId('git-sync-overview-preset-unlinked').click();
  await expect(page.getByTestId('git-sync-overview-active-filter-linked')).toBeVisible();
  await expect(page).toHaveURL(/linked=unlinked/);

  await saveOverviewPreset(page, presetName);
  await expect(page.getByText(`Overview preset "${presetName}" saved`)).toBeVisible();
  const savedChip = page.locator('[data-testid^="git-sync-overview-saved-preset-"]', { hasText: presetName });
  await expect(savedChip).toBeVisible();

  await page.getByTestId('git-sync-overview-active-filter-linked').getByRole('button').click();
  await expect(page.getByTestId('git-sync-overview-active-filter-linked')).toHaveCount(0);
  await expect(page).not.toHaveURL(/linked=/);

  await savedChip.click();
  await expect(page.getByTestId('git-sync-overview-active-filter-linked')).toBeVisible();
  await expect(page).toHaveURL(/linked=unlinked/);

  await page
    .locator('[data-testid^="git-sync-overview-saved-preset-"]', { hasText: presetName })
    .getByRole('button')
    .click();
  await expect(savedChip).toHaveCount(0);
});

test('run saved filter preset save apply and delete', async ({ page }) => {
  test.setTimeout(180_000);

  const stamp = Date.now();
  const presetName = `E2E Run ${stamp}`;

  await register(page, stamp, `Git Run Preset ${stamp}`);
  await page.goto('/settings/git');
  await waitForGitOverview(page);

  await page.getByTestId('org-git-sync-runs-preset-failed').click();
  await expect(page.getByTestId('org-git-sync-runs-active-filter-runStatus')).toBeVisible();
  await expect(page).toHaveURL(/runStatus=failed/);

  await saveRunPreset(page, presetName);
  await expect(page.getByText(`Run preset "${presetName}" saved`)).toBeVisible();
  const savedChip = page.locator('[data-testid^="org-git-sync-runs-saved-preset-"]', { hasText: presetName });
  await expect(savedChip).toBeVisible();

  await page.getByTestId('org-git-sync-runs-active-filter-runStatus').getByRole('button').click();
  await expect(page.getByTestId('org-git-sync-runs-active-filter-runStatus')).toHaveCount(0);
  await expect(page).not.toHaveURL(/runStatus=/);

  await savedChip.click();
  await expect(page.getByTestId('org-git-sync-runs-active-filter-runStatus')).toBeVisible();
  await expect(page).toHaveURL(/runStatus=failed/);

  await page
    .locator('[data-testid^="org-git-sync-runs-saved-preset-"]', { hasText: presetName })
    .getByRole('button')
    .click();
  await expect(savedChip).toHaveCount(0);
});

test('org-shared overview preset shows org suffix', async ({ page }) => {
  test.setTimeout(120_000);

  const stamp = Date.now();
  const presetName = `E2E Org Share ${stamp}`;

  await register(page, stamp, `Git Org Preset ${stamp}`);
  await page.goto('/settings/git');
  await waitForGitOverview(page);

  await page.getByTestId('git-sync-overview-preset-unlinked').click();
  await saveOverviewPreset(page, presetName, true);
  await expect(page.getByText(`Overview preset "${presetName}" shared with organization`)).toBeVisible();

  const savedChip = page.locator('[data-testid^="git-sync-overview-saved-preset-"]', { hasText: presetName });
  await expect(savedChip).toContainText('· Org');
});
