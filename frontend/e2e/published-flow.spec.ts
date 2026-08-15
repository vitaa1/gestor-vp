import { expect, test } from '@playwright/test';

const username = process.env['E2E_USERNAME'] ?? 'operador';
const password = process.env['E2E_PASSWORD'] ?? 'troque-esta-senha-local';

test('logs in and creates an entry through the published application', async ({
  page,
}, testInfo) => {
  await page.goto('/');

  await expect(page.getByText('Versão em desenvolvimento')).toBeVisible();
  await page.getByLabel('Usuário').fill('invalid-operator');
  await page.getByLabel('Senha').fill('invalid-password');
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page.getByText('Usuário ou senha incorretos.')).toBeVisible();

  await page.getByLabel('Usuário').fill(username);
  await page.getByLabel('Senha').fill(password);
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(
    page.getByRole('heading', { name: 'O que vence primeiro aparece primeiro.' }),
  ).toBeVisible();

  const productName = `Produto E2E ${testInfo.project.name}`;
  await page.getByLabel('Nome do produto').fill(productName);
  await page.getByLabel('Quantidade').fill('3');
  await page.getByLabel('Data de validade').fill('2035-12-31');
  await page.getByRole('button', { name: 'Adicionar produto' }).click();

  await expect(page.getByText('Produto adicionado!')).toBeVisible();
  await expect(page.locator('.entry-card').filter({ hasText: productName })).toBeVisible();

  const viewportMetrics = await page.evaluate(() => ({
    clientWidth: document.documentElement.clientWidth,
    scrollWidth: document.documentElement.scrollWidth,
  }));
  expect(viewportMetrics.scrollWidth).toBe(viewportMetrics.clientWidth);
});
