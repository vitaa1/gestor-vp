import { expect, Page, test } from '@playwright/test';

const username = process.env['E2E_USERNAME'] ?? 'operador';
const password = process.env['E2E_PASSWORD'] ?? 'troque-esta-senha-local';

async function expectResponsiveLayoutToFit(page: Page): Promise<void> {
  const layout = await page.evaluate(() => {
    const clientWidth = document.documentElement.clientWidth;
    const overflowingElements = [...document.body.querySelectorAll<HTMLElement>('*')]
      .filter((element) => {
        const bounds = element.getBoundingClientRect();
        return bounds.width > 0 && (bounds.left < -1 || bounds.right > clientWidth + 1);
      })
      .map((element) => `${element.tagName.toLowerCase()}#${element.id}.${element.className}`)
      .slice(0, 10);

    return {
      clientWidth,
      scrollWidth: document.documentElement.scrollWidth,
      overflowingElements,
    };
  });

  expect(layout.scrollWidth).toBe(layout.clientWidth);
  expect(layout.overflowingElements).toEqual([]);
}

test('completes the inventory and history flow through the published application', async ({
  page,
}, testInfo) => {
  const isMobile = testInfo.project.name === 'mobile-chromium';
  const isResponsive = testInfo.project.name !== 'desktop-chromium';
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
  if (isResponsive) {
    await expectResponsiveLayoutToFit(page);
  }

  const runId = Date.now().toString();
  const productName = `Produto E2E ${testInfo.project.name} ${runId}`;
  const projectSuffix = isMobile ? '2' : testInfo.project.name === 'tablet-chromium' ? '3' : '1';
  const barcode = `${runId}${projectSuffix}`;
  if (isMobile) {
    const formCardBounds = await page.locator('.form-card').boundingBox();
    const expirationDateBounds = await page.getByLabel('Data de validade').boundingBox();

    expect(formCardBounds).not.toBeNull();
    expect(expirationDateBounds).not.toBeNull();
    expect(expirationDateBounds!.x).toBeGreaterThanOrEqual(formCardBounds!.x);
    expect(expirationDateBounds!.x + expirationDateBounds!.width).toBeLessThanOrEqual(
      formCardBounds!.x + formCardBounds!.width,
    );
  }
  await page.getByLabel('Nome do produto').fill(productName);
  await page.getByLabel('Quantidade').fill('3');
  await page.getByLabel('Data de validade').fill('31122035');
  await expect(page.getByLabel('Data de validade')).toHaveValue('31/12/2035');
  await page.locator('details.optional-details').getByText('Mais detalhes').click();
  if (isResponsive) {
    await expectResponsiveLayoutToFit(page);
  }
  await page.getByLabel('Código de barras').fill(barcode);
  await page.getByLabel('Categoria').fill('Teste E2E');
  await page.getByLabel('Preço de custo unitário').fill('18.75');
  await page.getByLabel('Fornecedor').fill('Fornecedor E2E');
  await page.getByLabel('Número do lote').fill('LOTE-E2E');
  await page.getByRole('button', { name: 'Adicionar produto' }).click();

  await expect(page.getByText('Produto adicionado!')).toBeVisible();

  await page.getByRole('button', { name: 'Produtos', exact: true }).click();
  const productSearch = page.getByRole('form', { name: 'Buscar produtos' });
  await productSearch.getByLabel('Nome do produto').fill(productName);
  await productSearch.getByLabel('Situação').selectOption('OK');
  await productSearch.getByRole('button', { name: 'Buscar' }).click();
  const entryCard = page.locator('.entry-card').filter({ hasText: productName });
  await expect(entryCard).toBeVisible();
  if (isResponsive) {
    await expectResponsiveLayoutToFit(page);
  }

  await entryCard.getByRole('button', { name: 'Ver detalhes' }).click();
  const activeDetailsDialog = page.getByRole('dialog');
  await expect(activeDetailsDialog.getByText(barcode)).toBeVisible();
  await expect(activeDetailsDialog.getByText('Teste E2E')).toBeVisible();
  await expect(activeDetailsDialog.getByText('R$ 18,75')).toBeVisible();
  await expect(activeDetailsDialog.getByText('Fornecedor E2E')).toBeVisible();
  await expect(activeDetailsDialog.getByText('LOTE-E2E')).toBeVisible();
  if (isResponsive) {
    await expectResponsiveLayoutToFit(page);
  }
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
  await expect(productMovements.first()).toContainText('Uso');
  if (isResponsive) {
    await expectResponsiveLayoutToFit(page);
  }
  if (testInfo.project.name === 'tablet-chromium') {
    const factPositions = await productMovements
      .first()
      .locator('.movement-row__facts > div')
      .evaluateAll((facts) =>
        facts.map((fact) => {
          const bounds = fact.getBoundingClientRect();
          return { left: Math.round(bounds.left), top: Math.round(bounds.top) };
        }),
      );
    expect(factPositions).toHaveLength(4);
    expect(new Set(factPositions.map(({ top }) => top)).size).toBe(1);
    expect(new Set(factPositions.map(({ left }) => left)).size).toBe(4);
  }

  await productMovements
    .first()
    .getByRole('button', { name: `Ver detalhes da entrada encerrada de ${productName}` })
    .click();
  const detailsDialog = page.getByRole('dialog');
  await expect(
    detailsDialog.getByText('Esta entrada está encerrada e disponível somente para consulta.'),
  ).toBeVisible();
  await expect(detailsDialog.getByRole('heading', { name: productName })).toBeVisible();
  await expect(detailsDialog.getByRole('heading', { name: 'Retirar unidades' })).toHaveCount(0);
  if (isResponsive) {
    await expectResponsiveLayoutToFit(page);
  }

  const closeButton = detailsDialog.getByRole('button', { name: 'Fechar detalhes' });
  const iconOffset = await closeButton.evaluate((button) => {
    const buttonBounds = button.getBoundingClientRect();
    const iconBounds = button.querySelector('svg')?.getBoundingClientRect();
    if (!iconBounds) {
      return null;
    }
    return {
      x: Math.abs(
        buttonBounds.left + buttonBounds.width / 2 - (iconBounds.left + iconBounds.width / 2),
      ),
      y: Math.abs(
        buttonBounds.top + buttonBounds.height / 2 - (iconBounds.top + iconBounds.height / 2),
      ),
    };
  });
  expect(iconOffset).not.toBeNull();
  expect(iconOffset?.x).toBeLessThan(0.5);
  expect(iconOffset?.y).toBeLessThan(0.5);

  await page.keyboard.press('Tab');
  await page.keyboard.press('Shift+Tab');
  await expect(closeButton).toBeFocused();
  await expect(closeButton).toHaveCSS('outline-width', '3px');
  await expect(closeButton).toHaveCSS('outline-color', 'rgb(23, 59, 99)');

  await closeButton.hover();
  await expect(closeButton).toHaveCSS('color', 'rgb(23, 59, 99)');
  await page.mouse.down();
  await expect(closeButton).toHaveCSS('background-color', 'rgb(23, 59, 99)');
  await expect(closeButton).toHaveCSS('color', 'rgb(255, 255, 255)');
  await page.mouse.up();
});
