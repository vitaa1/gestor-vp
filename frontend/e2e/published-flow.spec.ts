import { expect, test } from '@playwright/test';

const username = process.env['E2E_USERNAME'] ?? 'operador';
const password = process.env['E2E_PASSWORD'] ?? 'troque-esta-senha-local';

test('completes the inventory and history flow through the published application', async ({
  page,
}, testInfo) => {
  await page.goto('/');

  await expect(page).toHaveTitle('gestorVP');
  await expect(page.getByText('gestorVP', { exact: true })).toBeVisible();
  await expect(page.getByText('Versão em desenvolvimento')).toBeVisible();
  await page.getByLabel('Usuário').fill('invalid-operator');
  await page.getByLabel('Senha', { exact: true }).fill('invalid-password');
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page.getByText('Usuário ou senha incorretos.')).toBeVisible();

  await page.getByLabel('Usuário').fill(username);
  await page.getByLabel('Senha', { exact: true }).fill(password);
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(
    page.getByRole('heading', { name: 'O que vence primeiro aparece primeiro.' }),
  ).toBeVisible();

  const productName = `Produto E2E ${testInfo.project.name} tentativa ${testInfo.retry}`;
  await page.getByLabel('Nome do produto').fill(productName);
  await page.getByLabel('Quantidade').fill('3');
  await page.getByLabel('Data de validade').fill('2035-12-31');
  await page.getByRole('button', { name: 'Adicionar produto' }).click();

  await expect(page.getByText('Produto adicionado!')).toBeVisible();
  let entryCard = page.locator('.entry-card').filter({ hasText: productName });
  await expect(entryCard).toBeVisible();

  await page.getByRole('button', { name: 'Produtos' }).click();
  const productSearch = page.getByRole('form', { name: 'Buscar produtos' });
  await productSearch.getByLabel('Nome do produto').fill('produto e2e');
  await productSearch.getByLabel('Situação').selectOption('OK');
  await productSearch.getByRole('button', { name: 'Buscar' }).click();
  entryCard = page.locator('.entry-card').filter({ hasText: productName });
  await expect(entryCard).toBeVisible();

  await entryCard.getByRole('button', { name: 'Ver detalhes' }).click();
  await page.locator('#withdrawal-quantity').fill('3');
  await page.locator('#withdrawal-reason').selectOption('USED');
  await page.getByRole('button', { name: 'Revisar retirada' }).click();
  await page.getByRole('button', { name: 'Confirmar retirada' }).click();
  await expect(entryCard).toHaveCount(0);
  await expect(page.getByText('Nenhum produto encontrado.')).toBeVisible();

  await page.getByRole('button', { name: 'Histórico' }).click();
  await expect(page.getByRole('heading', { name: 'Histórico' })).toBeVisible();
  const productMovements = page.locator('.movement-row').filter({ hasText: productName });
  await expect(productMovements).toHaveCount(2);
  await expect(productMovements.first()).toContainText('Retirada');
  await expect(productMovements.first()).toContainText('Usei');

  await productMovements
    .first()
    .getByRole('button', { name: `Consultar entrada encerrada de ${productName}` })
    .click();
  const detailsDialog = page.getByRole('dialog');
  await expect(
    detailsDialog.getByText('Esta entrada está encerrada e disponível somente para consulta.'),
  ).toBeVisible();
  await expect(detailsDialog.getByRole('heading', { name: productName })).toBeVisible();
  await expect(detailsDialog.getByRole('heading', { name: 'Retirar unidades' })).toHaveCount(0);

  const viewportMetrics = await page.evaluate(() => ({
    clientWidth: document.documentElement.clientWidth,
    scrollWidth: document.documentElement.scrollWidth,
  }));
  expect(viewportMetrics.scrollWidth).toBe(viewportMetrics.clientWidth);
});
