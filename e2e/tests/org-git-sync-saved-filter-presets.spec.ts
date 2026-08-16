import { expect, test, type Locator, type Page } from '@playwright/test';

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
  await expect(page.getByTestId('git-sync-save-preset-dialog')).toHaveCount(0);
}

async function saveRunPreset(page: Page, name: string, shareWithOrg = false) {
  await page.getByTestId('org-git-sync-runs-save-preset').click();
  await expect(page.getByTestId('git-sync-save-preset-dialog')).toBeVisible();
  await page.getByTestId('git-sync-save-preset-name-input').fill(name);
  if (shareWithOrg) {
    await page.getByTestId('git-sync-save-preset-share-org').check();
  }
  await page.getByTestId('git-sync-save-preset-confirm').click();
  await expect(page.getByTestId('git-sync-save-preset-dialog')).toHaveCount(0);
}

async function clearOverviewLinkedFilter(page: Page) {
  await page.getByTestId('git-sync-overview').scrollIntoViewIfNeeded();
  await page.getByRole('combobox', { name: 'Linked' }).click();
  await page.getByRole('option', { name: 'All projects' }).click();
}

async function clearRunStatusFilter(page: Page) {
  await page.getByTestId('org-git-sync-runs').scrollIntoViewIfNeeded();
  await page.getByRole('combobox', { name: 'Status' }).click();
  await page.getByRole('option', { name: 'All statuses' }).click();
}

async function deleteSavedChip(chip: Locator) {
  await chip.locator('.MuiChip-deleteIcon').click();
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

  await clearOverviewLinkedFilter(page);
  await expect(page.getByTestId('git-sync-overview-active-filter-linked')).toHaveCount(0);
  await expect(page).not.toHaveURL(/linked=/);

  await savedChip.click();
  await expect(page.getByTestId('git-sync-overview-active-filter-linked')).toBeVisible();
  await expect(page).toHaveURL(/linked=unlinked/);

  await deleteSavedChip(savedChip);
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

  await clearRunStatusFilter(page);
  await expect(page.getByTestId('org-git-sync-runs-active-filter-runStatus')).toHaveCount(0);
  await expect(page).not.toHaveURL(/runStatus=/);

  await savedChip.click();
  await expect(page.getByTestId('org-git-sync-runs-active-filter-runStatus')).toBeVisible();
  await expect(page).toHaveURL(/runStatus=failed/);

  await deleteSavedChip(savedChip);
  await expect(savedChip).toHaveCount(0);
});

test('overview saved filter preset can be renamed', async ({ page }) => {
  test.setTimeout(180_000);

  const stamp = Date.now();
  const presetName = `E2E Rename ${stamp}`;
  const renamed = `E2E Renamed ${stamp}`;

  await register(page, stamp + 7, `Git Rename Preset ${stamp}`);
  await page.goto('/settings/git');
  await waitForGitOverview(page);

  await page.getByTestId('git-sync-overview-preset-unlinked').click();
  await saveOverviewPreset(page, presetName);

  const savedChip = page.locator('[data-testid^="git-sync-overview-saved-preset-"]', { hasText: presetName });
  await expect(savedChip).toBeVisible();

  const renameButton = page.locator('[data-testid^="git-sync-overview-rename-preset-"]');
  await renameButton.click();
  await expect(page.getByTestId('git-sync-rename-preset-dialog')).toBeVisible();
  await page.getByTestId('git-sync-rename-preset-name-input').fill(renamed);
  await page.getByTestId('git-sync-rename-preset-confirm').click();
  await expect(page.getByTestId('git-sync-rename-preset-dialog')).toHaveCount(0);
  await expect(page.getByText(`Overview preset renamed to "${renamed}"`)).toBeVisible();

  const renamedChip = page.locator('[data-testid^="git-sync-overview-saved-preset-"]', { hasText: renamed });
  await expect(renamedChip).toBeVisible();
  await expect(savedChip).toHaveCount(0);
});

test('overview saved filter preset filters can be updated', async ({ page }) => {
  test.setTimeout(180_000);

  const stamp = Date.now();
  const presetName = `E2E Update ${stamp}`;

  await register(page, stamp + 11, `Git Update Preset ${stamp}`);
  await page.goto('/settings/git');
  await waitForGitOverview(page);

  await page.getByTestId('git-sync-overview-preset-unlinked').click();
  await saveOverviewPreset(page, presetName);
  const savedChip = page.locator('[data-testid^="git-sync-overview-saved-preset-"]', { hasText: presetName });
  await expect(savedChip).toBeVisible();

  await page.getByRole('combobox', { name: 'Provider' }).click();
  await page.getByRole('option', { name: 'GitHub' }).click();
  await expect(page.getByTestId('git-sync-overview-active-filter-provider')).toBeVisible();

  const updateButton = page.locator('[data-testid^="git-sync-overview-update-preset-"]');
  await expect(updateButton).toBeVisible();
  await updateButton.click();
  await expect(page.getByText(`Overview preset "${presetName}" updated to current filters`)).toBeVisible();

  await clearOverviewLinkedFilter(page);
  await page.getByRole('combobox', { name: 'Provider' }).click();
  await page.getByRole('option', { name: 'All providers' }).click();
  await expect(page.getByTestId('git-sync-overview-active-filter-linked')).toHaveCount(0);
  await expect(page.getByTestId('git-sync-overview-active-filter-provider')).toHaveCount(0);

  await savedChip.click();
  await expect(page.getByTestId('git-sync-overview-active-filter-linked')).toBeVisible();
  await expect(page.getByTestId('git-sync-overview-active-filter-provider')).toBeVisible();
  await expect(page).toHaveURL(/linked=unlinked/);
  await expect(page).toHaveURL(/provider=github/);
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
