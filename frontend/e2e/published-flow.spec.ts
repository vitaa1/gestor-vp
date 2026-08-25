import { expect, Page, test } from '@playwright/test';

const username = process.env['E2E_USERNAME'] ?? 'operador';
const password = process.env['E2E_PASSWORD'] ?? 'troque-esta-senha-local';

interface TestExpirationDate {
  input: string;
  display: string;
}

function expirationDate(year: number, month: number, day: number): TestExpirationDate {
  const dayText = String(day).padStart(2, '0');
  const monthText = String(month).padStart(2, '0');
  const yearText = String(year).padStart(4, '0');

  return {
    input: `${dayText}${monthText}${yearText}`,
    display: `${dayText}/${monthText}/${yearText}`,
  };
}

async function login(page: Page): Promise<void> {
  await page.getByLabel('Usuário').fill(username);
  await page.getByLabel('Senha', { exact: true }).fill(password);
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(
    page.getByRole('heading', { name: 'O que vence primeiro aparece primeiro.' }),
  ).toBeVisible();
}

async function expirationDateFromToday(
  page: Page,
  daysFromToday: number,
): Promise<TestExpirationDate> {
  const today = await page.evaluate(() => {
    const currentDate = new Date();
    return {
      year: currentDate.getFullYear(),
      month: currentDate.getMonth(),
      day: currentDate.getDate(),
    };
  });
  const expirationDate = new Date(Date.UTC(today.year, today.month, today.day + daysFromToday));
  const day = String(expirationDate.getUTCDate()).padStart(2, '0');
  const month = String(expirationDate.getUTCMonth() + 1).padStart(2, '0');
  const year = expirationDate.getUTCFullYear();

  return {
    input: `${day}${month}${year}`,
    display: `${day}/${month}/${year}`,
  };
}

async function expirationDatesBeforeCurrentSummary(page: Page): Promise<TestExpirationDate[]> {
  const stockSummary = page.locator('.stock-summary');
  await expect(stockSummary.getByText('Carregando produtos…')).toHaveCount(0);
  const visibleDates = await stockSummary.locator('.entry-card__date strong').allTextContents();
  const earliestVisibleTime = visibleDates.reduce((earliestTime, visibleDate) => {
    const [day, month, year] = visibleDate.trim().split('/').map(Number);
    const parsedDate = new Date(0);
    parsedDate.setUTCHours(0, 0, 0, 0);
    parsedDate.setUTCFullYear(year, month - 1, day);
    return Math.min(earliestTime, parsedDate.getTime());
  }, Number.POSITIVE_INFINITY);
  const historicalReferenceTime = Date.UTC(1900, 0, 6);
  const referenceTime = Number.isFinite(earliestVisibleTime)
    ? Math.min(earliestVisibleTime, historicalReferenceTime)
    : historicalReferenceTime;

  return Array.from({ length: 5 }, (_, index) => {
    const date = new Date(referenceTime - (5 - index) * 24 * 60 * 60 * 1000);
    return expirationDate(date.getUTCFullYear(), date.getUTCMonth() + 1, date.getUTCDate());
  });
}

async function addBasicProduct(
  page: Page,
  productName: string,
  quantity: number,
  expirationDate: TestExpirationDate,
): Promise<void> {
  const productNameField = page.getByLabel('Nome do produto');
  await productNameField.fill(productName);
  await page.getByLabel('Quantidade').fill(String(quantity));
  await page.getByLabel('Data de validade').fill(expirationDate.input);
  await page.getByRole('button', { name: 'Adicionar produto' }).click();

  await expect(productNameField).toHaveValue('');
  await expect(page.getByText('Produto adicionado!')).toBeVisible();
}

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
  await expect(page.getByText('Versão em desenvolvimento')).toHaveCount(0);
  if (testInfo.project.name === 'desktop-chromium') {
    await page.getByLabel('Usuário').fill(`invalid-operator-${Date.now()}`);
    await page.getByLabel('Senha', { exact: true }).fill('invalid-password');
    await page.getByRole('button', { name: 'Entrar' }).click();
    await expect(page.getByText('Usuário ou senha incorretos.')).toBeVisible();
  }

  await login(page);
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

test('proves the remaining MVP acceptance criteria through the published application', async ({
  page,
}, testInfo) => {
  test.skip(
    testInfo.project.name !== 'desktop-chromium',
    'Os critérios de domínio usam um único projeto para manter o banco compartilhado determinístico.',
  );
  await page.goto('/');
  await login(page);

  const optionalDetails = page.locator('details.optional-details');
  expect(await optionalDetails.getAttribute('open')).toBeNull();

  const runId = Date.now().toString();
  const testPrefix = `Critério MVP ${runId}`;
  const priorityExpirationDates = await expirationDatesBeforeCurrentSummary(page);
  const priorityCases = Array.from({ length: 5 }, (_, index) => ({
    name: `${testPrefix} prioridade ${index + 1}`,
    quantity: index + 1,
    expirationDate: priorityExpirationDates[index],
  }));

  for (const priorityCase of priorityCases) {
    await addBasicProduct(
      page,
      priorityCase.name,
      priorityCase.quantity,
      priorityCase.expirationDate,
    );
  }

  const summaryCards = page.locator('.stock-summary .entry-card');
  await expect(summaryCards).toHaveCount(5);
  await expect(summaryCards.locator('h3')).toHaveText(priorityCases.map(({ name }) => name));
  for (const priorityCase of priorityCases) {
    const summaryCard = summaryCards.filter({ hasText: priorityCase.name });
    await expect(summaryCard).toContainText(
      `${priorityCase.quantity} ${priorityCase.quantity === 1 ? 'unidade' : 'unidades'}`,
    );
    await expect(summaryCard).toContainText(priorityCase.expirationDate.display);
    await expect(summaryCard).toContainText('Vencido');
    await expect(summaryCard.getByRole('button', { name: 'Ver detalhes' })).toBeVisible();
  }
  await expect(page.getByRole('button', { name: 'Ver todos os produtos' })).toBeVisible();

  const productPrefix = `${testPrefix} faixa`;
  const expirationCases = [
    {
      name: `${productPrefix} vencido`,
      status: 'EXPIRED',
      statusLabel: 'Vencido',
      expirationDate: expirationDate(1900, 1, 10),
    },
    {
      name: `${productPrefix} atenção`,
      status: 'ATTENTION',
      statusLabel: 'Atenção',
      expirationDate: await expirationDateFromToday(page, 3),
    },
    {
      name: `${productPrefix} observação`,
      status: 'WATCH',
      statusLabel: 'Fique de olho',
      expirationDate: await expirationDateFromToday(page, 15),
    },
    {
      name: `${productPrefix} regular`,
      status: 'OK',
      statusLabel: 'Tudo certo',
      expirationDate: await expirationDateFromToday(page, 45),
    },
  ];

  for (const expirationCase of expirationCases) {
    await addBasicProduct(page, expirationCase.name, 3, expirationCase.expirationDate);
  }

  await expect(page.locator('.feedback__success-icon')).toBeVisible();
  await expect(summaryCards).toHaveCount(5);
  await page.getByRole('button', { name: 'Ver todos os produtos' }).click();

  const productSearch = page.getByRole('form', { name: 'Buscar produtos' });
  const productQuery = productSearch.getByLabel('Nome do produto');
  const statusFilter = productSearch.getByLabel('Situação');
  await expect(statusFilter.locator('option')).toHaveText([
    'Todos',
    'Vencido',
    'Atenção',
    'Fique de olho',
    'Tudo certo',
  ]);

  await productQuery.fill(productPrefix);
  await productSearch.getByRole('button', { name: 'Buscar' }).click();
  const matchingCards = page.locator('.entry-card').filter({ hasText: productPrefix });
  await expect(matchingCards).toHaveCount(4);
  await expect(matchingCards.locator('h3')).toHaveText(expirationCases.map(({ name }) => name));

  for (const expirationCase of expirationCases) {
    await statusFilter.selectOption(expirationCase.status);
    await expect(matchingCards).toHaveCount(1);
    await expect(matchingCards).toContainText(expirationCase.name);
    await expect(matchingCards).toContainText('3 unidades');
    await expect(matchingCards).toContainText(expirationCase.statusLabel);
    await expect(matchingCards).toContainText(expirationCase.expirationDate.display);
  }

  await statusFilter.selectOption('');
  await expect(matchingCards).toHaveCount(4);
  await productQuery.fill(`${productPrefix} ausente`);
  await productSearch.getByRole('button', { name: 'Buscar' }).click();
  await expect(page.getByText('Nenhum produto encontrado.')).toBeVisible();

  await productQuery.fill(productPrefix);
  await productSearch.getByRole('button', { name: 'Buscar' }).click();
  await expect(matchingCards).toHaveCount(4);

  const expiredEntry = expirationCases[0];
  const expiredCard = matchingCards.filter({ hasText: expiredEntry.name });
  await expiredCard.getByRole('button', { name: 'Ver detalhes' }).click();
  let detailsDialog = page.getByRole('dialog');
  await expect(detailsDialog.locator('#withdrawal-reason option')).toHaveText([
    'Perda',
    'Vencimento',
  ]);
  await detailsDialog.locator('#withdrawal-quantity').fill('1');
  await detailsDialog.locator('#withdrawal-reason').selectOption('LOST');
  await detailsDialog.getByRole('button', { name: 'Revisar retirada' }).click();
  await detailsDialog.getByRole('button', { name: 'Confirmar retirada' }).click();
  await expect(expiredCard).toContainText('2 unidades');

  await expiredCard.getByRole('button', { name: 'Ver detalhes' }).click();
  detailsDialog = page.getByRole('dialog');
  await detailsDialog.locator('#withdrawal-quantity').fill('1');
  await detailsDialog.locator('#withdrawal-reason').selectOption('EXPIRED');
  await detailsDialog.getByRole('button', { name: 'Revisar retirada' }).click();
  await detailsDialog.getByRole('button', { name: 'Confirmar retirada' }).click();
  await expect(expiredCard).toContainText('1 unidade');

  const attentionEntry = expirationCases[1];
  const attentionCard = matchingCards.filter({ hasText: attentionEntry.name });
  await attentionCard.getByRole('button', { name: 'Ver detalhes' }).click();
  detailsDialog = page.getByRole('dialog');
  await detailsDialog.locator('#withdrawal-quantity').fill('1');
  await detailsDialog.locator('#withdrawal-reason').selectOption('SOLD');
  await detailsDialog.getByRole('button', { name: 'Revisar retirada' }).click();
  await expect(detailsDialog.getByRole('heading', { name: 'Confirme a retirada' })).toBeVisible();
  await detailsDialog.getByRole('button', { name: 'Cancelar' }).click();
  await expect(detailsDialog.getByRole('heading', { name: 'Retirar unidades' })).toBeVisible();
  await expect(attentionCard).toContainText('3 unidades');

  const concurrentPage = await page.context().newPage();
  await concurrentPage.goto('/');
  await login(concurrentPage);
  await concurrentPage.getByRole('button', { name: 'Produtos', exact: true }).click();
  const concurrentSearch = concurrentPage.getByRole('form', { name: 'Buscar produtos' });
  await concurrentSearch.getByLabel('Nome do produto').fill(attentionEntry.name);
  await concurrentSearch.getByRole('button', { name: 'Buscar' }).click();
  const concurrentCard = concurrentPage
    .locator('.entry-card')
    .filter({ hasText: attentionEntry.name });
  await expect(concurrentCard).toHaveCount(1);
  await concurrentCard.getByRole('button', { name: 'Ver detalhes' }).click();
  const concurrentDialog = concurrentPage.getByRole('dialog');
  await concurrentDialog.locator('#withdrawal-quantity').fill('1');
  await concurrentDialog.locator('#withdrawal-reason').selectOption('USED');
  await concurrentDialog.getByRole('button', { name: 'Revisar retirada' }).click();
  await concurrentDialog.getByRole('button', { name: 'Confirmar retirada' }).click();
  await expect(concurrentCard).toContainText('2 unidades');

  await detailsDialog.locator('#withdrawal-quantity').fill('3');
  await detailsDialog.getByRole('button', { name: 'Revisar retirada' }).click();
  await detailsDialog.getByRole('button', { name: 'Confirmar retirada' }).click();
  await expect(
    detailsDialog.getByText('A quantidade informada supera o saldo disponível.'),
  ).toBeVisible();
  await expect(attentionCard).toContainText('3 unidades');
  await concurrentSearch.getByRole('button', { name: 'Buscar' }).click();
  await expect(concurrentCard).toContainText('2 unidades');
  await concurrentPage.close();

  await detailsDialog.getByRole('button', { name: 'Cancelar' }).click();
  await detailsDialog.locator('#withdrawal-quantity').fill('1');
  await detailsDialog.getByRole('button', { name: 'Revisar retirada' }).click();
  await detailsDialog.getByRole('button', { name: 'Confirmar retirada' }).click();
  await expect(attentionCard).toContainText('1 unidade');

  await page.getByRole('button', { name: 'Histórico' }).click();
  const matchingMovements = page.locator('.movement-row').filter({ hasText: productPrefix });
  await expect(matchingMovements).toHaveCount(8);
  for (let index = 0; index < 4; index += 1) {
    await expect(matchingMovements.nth(index)).toContainText('Retirada');
  }
  for (let index = 4; index < 8; index += 1) {
    await expect(matchingMovements.nth(index)).toContainText('Entrada');
  }

  const withdrawalMovements = matchingMovements.filter({ hasText: 'Retirada' });
  await expect(withdrawalMovements).toHaveCount(4);
  const saleMovement = withdrawalMovements.filter({ hasText: 'Venda' });
  const useMovement = withdrawalMovements.filter({ hasText: 'Uso' });
  const lossMovement = withdrawalMovements.filter({ hasText: 'Perda' });
  const expirationMovement = withdrawalMovements.filter({ hasText: 'Vencimento' });
  await expect(saleMovement).toHaveCount(1);
  await expect(useMovement).toHaveCount(1);
  await expect(lossMovement).toHaveCount(1);
  await expect(expirationMovement).toHaveCount(1);
  await expect(saleMovement).toContainText('1 unidade');
  await expect(useMovement).toContainText('1 unidade');
  await expect(lossMovement).toContainText('1 unidade');
  await expect(expirationMovement).toContainText('1 unidade');
  await expect(saleMovement).toContainText(attentionEntry.expirationDate.display);
  await expect(useMovement).toContainText(attentionEntry.expirationDate.display);
  await expect(lossMovement).toContainText(expiredEntry.expirationDate.display);
  await expect(expirationMovement).toContainText(expiredEntry.expirationDate.display);
  await expect(saleMovement.locator('time')).toHaveAttribute('datetime', /.+/);
  await expect(useMovement.locator('time')).toHaveAttribute('datetime', /.+/);
  await expect(lossMovement.locator('time')).toHaveAttribute('datetime', /.+/);
  await expect(expirationMovement.locator('time')).toHaveAttribute('datetime', /.+/);
  const entryMovements = matchingMovements.filter({ hasText: 'Entrada' });
  await expect(entryMovements).toHaveCount(4);
  await expect(entryMovements.locator('h3')).toHaveText([
    expirationCases[3].name,
    expirationCases[2].name,
    attentionEntry.name,
    expiredEntry.name,
  ]);
  for (const expirationCase of expirationCases) {
    const entryMovement = entryMovements.filter({ hasText: expirationCase.name });
    await expect(entryMovement).toHaveCount(1);
    await expect(entryMovement).toContainText('3 unidades');
    await expect(entryMovement).toContainText(expirationCase.expirationDate.display);
    await expect(entryMovement).toContainText('Motivo—');
    await expect(entryMovement.locator('time')).toHaveAttribute('datetime', /.+/);
  }
  expect(
    await matchingMovements
      .locator('.movement-row__facts')
      .evaluateAll((factLists) =>
        factLists.map((factList) => factList.querySelectorAll(':scope > div').length),
      ),
  ).toEqual([4, 4, 4, 4, 4, 4, 4, 4]);
  await expect(matchingMovements.getByRole('button')).toHaveText(
    Array.from({ length: 8 }, () => 'Ver detalhes'),
  );
  await expect(page.getByRole('button', { name: /Editar|Excluir/ })).toHaveCount(0);
  await expect(
    page.locator('.movement-list input, .movement-list select, .movement-list textarea'),
  ).toHaveCount(0);
});
