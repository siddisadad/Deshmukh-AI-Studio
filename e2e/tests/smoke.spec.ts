import { expect, test, type Page } from '@playwright/test';

/**
 * Phase 5–6 smoke journeys (docs/12-TESTING-STRATEGY.md):
 * 1. Register → create project → add requirement
 * 2. Create task → move to IN_PROGRESS
 * 3. Add document
 * 4. Streaming chat (mock AI) + multi-thread isolation
 * 5. RAG knowledge search after context asset save
 * 6. Billing overview (FREE plan)
 * 7. Plugins — disable sample Echo tool
 * 8. Logout
 */
test.describe.configure({ mode: 'serial' });

const stamp = Date.now();
const email = `e2e.${stamp}@example.com`;
const password = 'TestPass1234';
const displayName = `E2E User ${stamp}`;
const projectName = `E2E Project ${stamp}`;
const projectKey = `E${String(stamp).slice(-4)}`;
const requirementTitle = `E2E requirement ${stamp}`;
const taskTitle = `E2E task ${stamp}`;
const chatPrompt = 'Summarize open requirements for this project.';
const chatPromptSecond = `List kanban workflow columns for project ${stamp}`;
const ragMarker = `E2E_RAG_MARKER_${stamp}`;

async function register(page: Page) {
  await page.goto('/register');
  await page.getByTestId('register-display-name').fill(displayName);
  await page.getByTestId('register-email').fill(email);
  await page.getByTestId('register-password').fill(password);
  await page.getByTestId('register-submit').click();
  await expect(page).toHaveURL(/\/dashboard/);
}

async function createProject(page: Page) {
  await page.getByTestId('nav-projects').click();
  await expect(page).toHaveURL(/\/projects/);
  await page.getByTestId('new-project-button').click();
  await page.getByTestId('project-name').fill(projectName);
  await page.getByTestId('project-key').fill(projectKey);
  await page.getByTestId('project-description').fill('Playwright smoke project');
  await page.getByTestId('project-create-submit').click();
  await expect(page).toHaveURL(/\/projects\/[0-9a-f-]+$/i);
  await expect(page.getByRole('heading', { name: projectName })).toBeVisible();
}

test('private beta first-run smoke journey', async ({ page }) => {
  test.setTimeout(180_000);

  await register(page);
  await createProject(page);

  // 1) Add requirement
  await page.getByTestId('nav-requirements').click();
  await expect(page).toHaveURL(/\/requirements$/);
  await page.getByTestId('requirement-title-input').fill(requirementTitle);
  await page.getByTestId('requirement-add-button').click();
  await expect(page.getByTestId('requirement-item-title').filter({ hasText: requirementTitle })).toBeVisible();
  await expect(page.getByText('Requirement created')).toBeVisible();

  // 2) Create task and move to IN_PROGRESS
  await page.getByTestId('nav-projects').click();
  await page.getByTestId(`project-card-${projectKey}`).click();
  await page.getByTestId('nav-tasks').click();
  await expect(page).toHaveURL(/\/tasks$/);
  await page.getByTestId('new-task-button').click();
  await page.getByTestId('task-title-input').fill(taskTitle);
  await page.getByTestId('task-create-submit').click();
  await expect(page.getByTestId('task-column-TODO').getByTestId('task-card-title')).toContainText(taskTitle);

  const todoCard = page.getByTestId('task-column-TODO').locator('[data-testid^="task-card-"]').first();
  const taskId = (await todoCard.getAttribute('data-testid'))?.replace('task-card-', '');
  expect(taskId).toBeTruthy();
  await page.getByTestId(`task-status-${taskId}`).click();
  await page.getByRole('option', { name: 'Move to In Progress' }).click();
  await expect(
    page.getByTestId('task-column-IN_PROGRESS').getByTestId('task-card-title').filter({ hasText: taskTitle }),
  ).toBeVisible();

  // 3) Add document
  await page.getByTestId('nav-projects').click();
  await page.getByTestId(`project-card-${projectKey}`).click();
  await page.getByRole('link', { name: 'Documents' }).click();
  await expect(page).toHaveURL(/\/documents$/);
  const documentTitle = `E2E doc ${stamp}`;
  await page.getByTestId('document-new-title').fill(documentTitle);
  await page.getByTestId('document-create-submit').click();
  await expect(page.getByText(documentTitle)).toBeVisible();

  // 4) Streaming chat + multi-thread (mock provider)
  await page.getByTestId('nav-projects').click();
  await page.getByTestId(`project-card-${projectKey}`).click();
  await page.getByTestId('nav-chat').click();
  await expect(page).toHaveURL(/\/chat$/);
  await page.getByTestId('chat-input').fill(chatPrompt);
  await page.getByTestId('chat-send').click();
  await expect(page.getByTestId('chat-message-user').filter({ hasText: chatPrompt })).toBeVisible();
  await expect(page.getByTestId('chat-message-assistant')).toBeVisible({ timeout: 45_000 });

  await page.getByTestId('chat-new-thread').click();
  await page.getByTestId('chat-input').fill(chatPromptSecond);
  await page.getByTestId('chat-send').click();
  await expect(page.getByTestId('chat-message-user').filter({ hasText: chatPromptSecond })).toBeVisible();
  await expect(page.getByTestId('chat-message-assistant')).toBeVisible({ timeout: 45_000 });

  const threadButtons = page.locator('[data-testid^="chat-thread-"]');
  await expect(threadButtons).toHaveCount(2);
  const olderThread = threadButtons.nth(1);
  const olderThreadId = (await olderThread.getAttribute('data-testid'))?.replace('chat-thread-', '');
  expect(olderThreadId).toBeTruthy();
  await page.getByTestId(`chat-thread-${olderThreadId}`).click();
  await expect(page.getByTestId('chat-message-user').filter({ hasText: chatPrompt })).toBeVisible();
  await expect(page.getByTestId('chat-message-user').filter({ hasText: chatPromptSecond })).toHaveCount(0);

  // 5) RAG knowledge search
  await page.getByTestId('nav-projects').click();
  await page.getByTestId(`project-card-${projectKey}`).click();
  await page.getByTestId('nav-project-settings').click();
  await expect(page).toHaveURL(/\/settings$/);
  await page.getByLabel('Title').fill(`E2E API spec ${stamp}`);
  await page.getByTestId('context-asset-content').fill(ragMarker);
  await page.getByTestId('context-asset-save').click();
  await expect(page.getByText('Context asset saved')).toBeVisible();
  await page.getByTestId('knowledge-search-input').fill(ragMarker);
  await page.getByTestId('knowledge-search-submit').click();
  await expect(page.getByTestId('knowledge-hit').first()).toContainText(ragMarker, { timeout: 30_000 });

  // 6) Billing overview
  await page.getByTestId('nav-billing').click();
  await expect(page).toHaveURL(/\/settings\/billing/);
  await expect(page.getByTestId('billing-current-plan')).toContainText('Free');
  await expect(page.getByTestId('billing-plan-FREE')).toHaveText('Current plan');

  // 7) Plugins — sample tool toggle
  await page.getByTestId('nav-plugins').click();
  await expect(page).toHaveURL(/\/settings\/plugins/);
  await expect(page.getByTestId('plugin-row-core.assistant.business_analyst')).toBeVisible();
  const echoToggle = page.getByTestId('plugin-toggle-sample.tool.echo');
  await expect(echoToggle).toBeVisible();
  await echoToggle.click();
  await expect(page.getByText('Echo (Sample) disabled')).toBeVisible();

  // 8) Logout
  await page.getByTestId('logout-button').click();
  await expect(page).toHaveURL(/\/login/);
  await page.goto('/dashboard');
  await expect(page).toHaveURL(/\/login/);
});
