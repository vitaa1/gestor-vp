import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { App } from './app';
import { AuthService } from './auth/auth.service';
import { StockMovement, StockMovementPage } from './history/stock-movement.model';
import { StockMovementService } from './history/stock-movement.service';
import {
  StockEntry,
  StockEntryDetailsModel,
  StockEntryPage,
  WithdrawStock,
} from './inventory/stock-entry.model';
import { StockEntryService } from './inventory/stock-entry.service';

const entries: StockEntry[] = [
  {
    id: 1,
    productName: 'Leite Integral',
    quantity: 12,
    expirationDate: '2026-08-20',
    status: 'ATTENTION',
    statusLabel: 'Atenção',
    daysUntilExpiration: 6,
    createdAt: '2026-08-14T12:00:00Z',
  },
];

function stockPage(content: StockEntry[], hasNext = false): StockEntryPage {
  const cursorEntry = hasNext ? content.at(-1)! : null;
  return {
    content,
    size: content.length,
    hasNext,
    nextCursorExpirationDate: cursorEntry?.expirationDate ?? null,
    nextCursorCreatedAt: cursorEntry?.createdAt ?? null,
    nextCursorId: cursorEntry?.id ?? null,
  };
}

const movements: StockMovement[] = [
  {
    id: 2,
    stockEntryId: 1,
    productName: 'Leite Integral',
    expirationDate: '2026-08-20',
    type: 'WITHDRAWAL',
    typeLabel: 'Retirada',
    quantity: 5,
    reason: 'SOLD',
    reasonLabel: 'Vendi',
    createdAt: '2026-08-15T18:30:00Z',
    entryClosed: false,
  },
];

class StockEntryServiceStub {
  list() {
    return of(stockPage(entries));
  }

  search() {
    return of(stockPage(entries));
  }

  create() {
    return of(entries[0]);
  }

  details() {
    return of<StockEntryDetailsModel>({
      ...entries[0],
      initialQuantity: 12,
      availableQuantity: 12,
    });
  }

  withdraw(_entryId: number, _withdrawal: WithdrawStock) {
    return of<StockEntryDetailsModel>({
      ...entries[0],
      initialQuantity: 12,
      availableQuantity: 7,
    });
  }
}

class StockMovementServiceStub {
  list() {
    return of<StockMovementPage>({
      content: movements,
      size: 20,
      hasNext: false,
      nextCursorCreatedAt: null,
      nextCursorId: null,
    });
  }
}

class AuthServiceStub {
  readonly authenticated = signal(true);

  logout() {
    this.authenticated.set(false);
  }
}

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        { provide: StockEntryService, useClass: StockEntryServiceStub },
        { provide: StockMovementService, useClass: StockMovementServiceStub },
        { provide: AuthService, useClass: AuthServiceStub },
      ],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should show the inventory ordered by expiration', async () => {
    const fixture = TestBed.createComponent(App);
    fixture.componentInstance.loadEntries();
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('O que vence primeiro');
    expect(compiled.textContent).toContain('Leite Integral');
    expect(compiled.textContent).toContain('Atenção');
  });

  it('should open the history from the primary navigation and load recent movements', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    const historyButton = (fixture.nativeElement as HTMLElement).querySelector(
      '[data-view="history"]',
    ) as HTMLButtonElement;

    historyButton.click();
    fixture.detectChanges();

    expect(fixture.componentInstance.activeView()).toBe('history');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Histórico');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Leite Integral');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Vendi');
  });

  it('should search products by name and expiration status from the primary navigation', () => {
    const service = TestBed.inject(StockEntryService);
    const search = vi.spyOn(service, 'search');
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const productsButton = fixture.nativeElement.querySelector(
      '[data-view="products"]',
    ) as HTMLButtonElement;
    productsButton.click();
    fixture.detectChanges();

    const queryInput = fixture.nativeElement.querySelector(
      '.product-search input',
    ) as HTMLInputElement;
    queryInput.value = ' pão ';
    queryInput.dispatchEvent(new Event('input'));
    const statusSelect = fixture.nativeElement.querySelector(
      '.product-search select',
    ) as HTMLSelectElement;
    statusSelect.value = 'WATCH';
    statusSelect.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    expect(fixture.componentInstance.activeView()).toBe('products');
    expect(search).toHaveBeenLastCalledWith(' pão ', 'WATCH');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Leite Integral');
  });

  it('should show a friendly message when a product search has no results', () => {
    const service = TestBed.inject(StockEntryService);
    vi.spyOn(service, 'search').mockReturnValueOnce(of(stockPage([])));
    const fixture = TestBed.createComponent(App);

    fixture.componentInstance.showProducts();
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      'Nenhum produto encontrado.',
    );
  });

  it('should keep pagination tied to the product criteria that produced its cursor', () => {
    const service = TestBed.inject(StockEntryService);
    const search = vi
      .spyOn(service, 'search')
      .mockReturnValueOnce(of(stockPage(entries, true)))
      .mockReturnValueOnce(of(stockPage([])));
    const fixture = TestBed.createComponent(App);
    const component = fixture.componentInstance;

    component.showProducts();
    component.updateProductQuery({ target: { value: 'pão' } } as unknown as Event);
    component.loadMoreProducts();

    expect(search).toHaveBeenNthCalledWith(
      2,
      '',
      '',
      50,
      entries[0].expirationDate,
      entries[0].createdAt,
      entries[0].id,
    );
  });

  it('should invalidate product pagination when a new search fails', () => {
    const service = TestBed.inject(StockEntryService);
    const search = vi
      .spyOn(service, 'search')
      .mockReturnValueOnce(of(stockPage(entries, true)))
      .mockReturnValueOnce(throwError(() => new Error('network error')));
    const fixture = TestBed.createComponent(App);
    const component = fixture.componentInstance;

    component.showProducts();
    component.productQuery.set('arroz');
    component.searchProducts();
    component.loadMoreProducts();

    expect(search).toHaveBeenCalledTimes(2);
    expect(component.productEntries()).toEqual(entries);
    expect(component.productHasMore()).toBe(false);
    expect(component.productError()).not.toBe('');
  });

  it('should clear a product pagination error after a successful retry', () => {
    const service = TestBed.inject(StockEntryService);
    const nextEntry = { ...entries[0], id: 2, productName: 'Arroz Integral' };
    vi.spyOn(service, 'search')
      .mockReturnValueOnce(of(stockPage(entries, true)))
      .mockReturnValueOnce(throwError(() => new Error('network error')))
      .mockReturnValueOnce(of(stockPage([nextEntry])));
    const fixture = TestBed.createComponent(App);
    const component = fixture.componentInstance;

    component.showProducts();
    component.loadMoreProducts();
    expect(component.productError()).not.toBe('');
    component.loadMoreProducts();

    expect(component.productError()).toBe('');
    expect(component.productEntries()).toEqual([...entries, nextEntry]);
  });

  it('should append another history page without replacing recent movements', () => {
    const service = TestBed.inject(StockMovementService);
    const olderMovement: StockMovement = {
      ...movements[0],
      id: 1,
      type: 'ENTRY',
      typeLabel: 'Entrada',
      quantity: 12,
      reason: null,
      reasonLabel: null,
      createdAt: '2026-08-14T12:00:00Z',
    };
    vi.spyOn(service, 'list')
      .mockReturnValueOnce(
        of({
          content: movements,
          size: 1,
          hasNext: true,
          nextCursorCreatedAt: movements[0].createdAt,
          nextCursorId: movements[0].id,
        }),
      )
      .mockReturnValueOnce(
        of({
          content: [olderMovement],
          size: 1,
          hasNext: false,
          nextCursorCreatedAt: null,
          nextCursorId: null,
        }),
      );
    const fixture = TestBed.createComponent(App);
    const component = fixture.componentInstance;

    component.showHistory();
    component.loadMoreMovements();

    expect(component.movements().map((movement) => movement.id)).toEqual([2, 1]);
    expect(component.movementHasMore()).toBe(false);
    expect(component.movementLoadingMore()).toBe(false);
    expect(service.list).toHaveBeenNthCalledWith(2, 20, movements[0].createdAt, movements[0].id);
  });

  it('should disable creation while the initial inventory is loading', () => {
    const service = TestBed.inject(StockEntryService);
    const pendingPage = new Subject<StockEntryPage>();
    vi.spyOn(service, 'list').mockReturnValueOnce(pendingPage);
    const fixture = TestBed.createComponent(App);

    fixture.componentInstance.loadEntries();
    fixture.detectChanges();
    const addButton = fixture.nativeElement.querySelector(
      'app-stock-entry-form button[type="submit"]',
    ) as HTMLButtonElement;
    expect(addButton.disabled).toBe(true);

    pendingPage.next(stockPage([]));
    pendingPage.complete();
    fixture.detectChanges();
    expect(addButton.disabled).toBe(false);
  });

  it('should keep entries ordered after creating and loading another page', () => {
    const service = TestBed.inject(StockEntryService);
    const middleEntry: StockEntry = {
      ...entries[0],
      id: 2,
      productName: 'Arroz Integral',
      expirationDate: '2027-01-10',
    };
    const lateEntry: StockEntry = {
      ...entries[0],
      id: 3,
      productName: 'Azeite',
      expirationDate: '2030-05-20',
    };
    vi.spyOn(service, 'list')
      .mockReturnValueOnce(of(stockPage(entries, true)))
      .mockReturnValueOnce(of(stockPage([middleEntry])));
    const fixture = TestBed.createComponent(App);
    const component = fixture.componentInstance;

    component.loadEntries();
    component.addCreatedEntry(lateEntry);
    component.loadMore();

    expect(component.entries().map((entry) => entry.productName)).toEqual([
      'Leite Integral',
      'Arroz Integral',
      'Azeite',
    ]);
    expect(component.loadingMore()).toBe(false);
    expect(service.list).toHaveBeenNthCalledWith(
      2,
      50,
      entries[0].expirationDate,
      entries[0].createdAt,
      entries[0].id,
    );
  });

  it('should order equal expiration dates by the actual creation instant', () => {
    const fixture = TestBed.createComponent(App);
    const component = fixture.componentInstance;
    const laterEntry = {
      ...entries[0],
      id: 2,
      productName: 'Produto posterior',
      createdAt: '2026-08-22T00:30:00.100Z',
    };
    const exactSecondEntry = {
      ...entries[0],
      id: 3,
      productName: 'Produto no segundo exato',
      createdAt: '2026-08-22T00:30:00Z',
    };

    component.addCreatedEntry(laterEntry);
    component.addCreatedEntry(exactSecondEntry);

    expect(component.entries().map((entry) => entry.productName)).toEqual([
      entries[0].productName,
      'Produto no segundo exato',
      'Produto posterior',
    ]);
  });

  it('should preserve a created entry when recovering from an initial list failure', () => {
    const service = TestBed.inject(StockEntryService);
    const lateEntry: StockEntry = {
      ...entries[0],
      id: 2,
      productName: 'Azeite',
      expirationDate: '2030-05-20',
    };
    vi.spyOn(service, 'list')
      .mockReturnValueOnce(throwError(() => new Error('network error')))
      .mockReturnValueOnce(of(stockPage(entries, true)));
    const fixture = TestBed.createComponent(App);
    const component = fixture.componentInstance;

    component.loadEntries();
    expect(component.errorMessage()).not.toBe('');
    component.addCreatedEntry(lateEntry);

    expect(component.errorMessage()).toBe('');
    expect(component.entries()).toEqual([...entries, lateEntry]);
  });

  it('should update the active list immediately after a withdrawal', () => {
    const fixture = TestBed.createComponent(App);
    const component = fixture.componentInstance;
    component.loadEntries();
    component.openDetails(1);

    component.withdrawSelected({ quantity: 5, reason: 'SOLD' });

    expect(component.entries()[0].quantity).toBe(7);
    expect(component.detailsOpen()).toBe(false);
    expect(component.actionMessage()).toContain('atualizado para 7');
  });

  it('should reconcile the first page when a withdrawal closes an entry', () => {
    const service = TestBed.inject(StockEntryService);
    const firstPage = Array.from({ length: 50 }, (_, index) => ({
      ...entries[0],
      id: index + 1,
      productName: `Produto ${index + 1}`,
    }));
    const shiftedEntry = { ...entries[0], id: 51, productName: 'Produto 51' };
    vi.spyOn(service, 'list')
      .mockReturnValueOnce(of(stockPage(firstPage, true)))
      .mockReturnValueOnce(of(stockPage([...firstPage.slice(1), shiftedEntry])));
    vi.spyOn(service, 'withdraw').mockReturnValueOnce(
      of({
        ...firstPage[0],
        initialQuantity: 12,
        availableQuantity: 0,
      }),
    );
    const fixture = TestBed.createComponent(App);
    const component = fixture.componentInstance;
    component.loadEntries();
    component.openDetails(1);

    component.withdrawSelected({ quantity: 12, reason: 'SOLD' });

    expect(component.entries()).toHaveLength(50);
    expect(component.entries().map((entry) => entry.id)).toContain(51);
    expect(component.hasMore()).toBe(false);
    expect(component.actionMessage()).toContain('saiu do estoque ativo');
  });

  it('should release a pending load-more state when reconciling a closed entry', () => {
    const service = TestBed.inject(StockEntryService);
    const pendingPage = new Subject<StockEntryPage>();
    const remainingEntry = { ...entries[0], id: 2, productName: 'Arroz Integral' };
    vi.spyOn(service, 'list')
      .mockReturnValueOnce(of(stockPage(entries, true)))
      .mockReturnValueOnce(pendingPage)
      .mockReturnValueOnce(of(stockPage([remainingEntry])));
    vi.spyOn(service, 'withdraw').mockReturnValueOnce(
      of({ ...entries[0], initialQuantity: 12, availableQuantity: 0 }),
    );
    const fixture = TestBed.createComponent(App);
    const component = fixture.componentInstance;
    component.loadEntries();
    component.loadMore();
    expect(component.loadingMore()).toBe(true);
    component.openDetails(1);

    component.withdrawSelected({ quantity: 12, reason: 'SOLD' });

    expect(component.loadingMore()).toBe(false);
    expect(component.entries()).toEqual([remainingEntry]);
    pendingPage.next(stockPage([]));
    expect(component.loadingMore()).toBe(false);
  });
});
