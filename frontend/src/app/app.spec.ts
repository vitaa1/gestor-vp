import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { App } from './app';
import { AuthService } from './auth/auth.service';
import { StockEntry, StockEntryPage } from './inventory/stock-entry.model';
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

class StockEntryServiceStub {
  list() {
    return of({ content: entries, page: 0, size: 50, totalElements: 1, totalPages: 1 });
  }

  create() {
    return of(entries[0]);
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

    pendingPage.next({ content: [], page: 0, size: 50, totalElements: 0, totalPages: 0 });
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
      .mockReturnValueOnce(
        of({ content: entries, page: 0, size: 1, totalElements: 2, totalPages: 2 }),
      )
      .mockReturnValueOnce(
        of({ content: [middleEntry], page: 1, size: 1, totalElements: 3, totalPages: 3 }),
      );
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
      .mockReturnValueOnce(
        of({ content: entries, page: 0, size: 1, totalElements: 2, totalPages: 2 }),
      );
    const fixture = TestBed.createComponent(App);
    const component = fixture.componentInstance;

    component.loadEntries();
    expect(component.errorMessage()).not.toBe('');
    component.addCreatedEntry(lateEntry);

    expect(component.errorMessage()).toBe('');
    expect(component.entries()).toEqual([...entries, lateEntry]);
  });
});
