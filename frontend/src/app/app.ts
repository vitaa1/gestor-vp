import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from './auth/auth.service';
import { Login } from './auth/login';
import { StockMovement } from './history/stock-movement.model';
import { StockMovementList } from './history/stock-movement-list';
import { StockMovementService } from './history/stock-movement.service';
import { StockEntryDetails } from './inventory/stock-entry-details';
import { StockEntryForm } from './inventory/stock-entry-form';
import { StockEntryList } from './inventory/stock-entry-list';
import {
  StockEntry,
  StockEntryDetailsModel,
  StockEntryPage,
  ExpirationStatus,
  WithdrawStock,
} from './inventory/stock-entry.model';
import { StockEntryService } from './inventory/stock-entry.service';

@Component({
  selector: 'app-root',
  imports: [
    Login,
    RouterLink,
    RouterOutlet,
    StockEntryForm,
    StockEntryList,
    StockEntryDetails,
    StockMovementList,
  ],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  private static readonly HOME_SUMMARY_SIZE = 5;
  private readonly stockEntryService = inject(StockEntryService);
  private readonly stockMovementService = inject(StockMovementService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  readonly authService = inject(AuthService);
  private loadVersion = 0;
  private inventoryLoaded = false;
  private entryCursorExpirationDate: string | null = null;
  private entryCursorCreatedAt: string | null = null;
  private entryCursorId: number | null = null;
  private productLoadVersion = 0;
  private productCursorExpirationDate: string | null = null;
  private productCursorCreatedAt: string | null = null;
  private productCursorId: number | null = null;
  private appliedProductQuery = '';
  private appliedProductStatus: ExpirationStatus | '' = '';
  private detailsLoadVersion = 0;
  private movementLoadVersion = 0;
  private movementCursorCreatedAt: string | null = null;
  private movementCursorId: number | null = null;
  private readonly detailsPanel = viewChild(StockEntryDetails);

  readonly entries = signal<StockEntry[]>([]);
  readonly activeView = signal<'stock' | 'products' | 'history'>('stock');
  readonly loading = signal(false);
  readonly loadingMore = signal(false);
  readonly hasMore = signal(false);
  readonly errorMessage = signal('');
  readonly selectedEntry = signal<StockEntryDetailsModel | null>(null);
  readonly detailsOpen = signal(false);
  readonly detailsLoading = signal(false);
  readonly detailsError = signal('');
  readonly withdrawalPending = signal(false);
  readonly withdrawalError = signal('');
  readonly actionMessage = signal('');
  readonly productQuery = signal('');
  readonly productStatus = signal<ExpirationStatus | ''>('');
  readonly productEntries = signal<StockEntry[]>([]);
  readonly productLoading = signal(false);
  readonly productLoadingMore = signal(false);
  readonly productHasMore = signal(false);
  readonly productError = signal('');
  readonly movements = signal<StockMovement[]>([]);
  readonly movementLoading = signal(false);
  readonly movementLoadingMore = signal(false);
  readonly movementHasMore = signal(false);
  readonly movementError = signal('');

  constructor() {
    this.router.events
      .pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((event) => this.activateRoute(event.urlAfterRedirects));
  }

  showStock(): void {
    this.activateView('stock');
    void this.router.navigateByUrl('/');
  }

  showProducts(): void {
    this.activateView('products', true);
    void this.router.navigateByUrl('/produtos');
  }

  showHistory(): void {
    this.activateView('history', true);
    void this.router.navigateByUrl('/historico');
  }

  loadCurrentView(): void {
    this.activateView(this.activeView(), true);
  }

  updateProductQuery(event: Event): void {
    this.productQuery.set((event.target as HTMLInputElement).value);
  }

  updateProductStatus(event: Event): void {
    this.productStatus.set((event.target as HTMLSelectElement).value as ExpirationStatus | '');
    this.searchProducts();
  }

  submitProductSearch(event: Event): void {
    event.preventDefault();
    this.searchProducts();
  }

  searchProducts(): void {
    const query = this.productQuery();
    const status = this.productStatus();
    const requestVersion = ++this.productLoadVersion;
    this.productLoading.set(true);
    this.productLoadingMore.set(false);
    this.productHasMore.set(false);
    this.clearProductCursor();
    this.productError.set('');
    this.stockEntryService.search(query, status).subscribe({
      next: (result) => {
        if (requestVersion !== this.productLoadVersion) {
          return;
        }
        this.productEntries.set(result.content);
        this.appliedProductQuery = query;
        this.appliedProductStatus = status;
        this.setProductCursor(result);
        this.productHasMore.set(result.hasNext);
        this.productLoading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        if (requestVersion !== this.productLoadVersion) {
          return;
        }
        if (error.status === 401) {
          this.logout();
          return;
        }
        this.productError.set('Não foi possível buscar os produtos.');
        this.productLoading.set(false);
      },
    });
  }

  loadMoreProducts(): void {
    const cursorExpirationDate = this.productCursorExpirationDate;
    const cursorCreatedAt = this.productCursorCreatedAt;
    const cursorId = this.productCursorId;
    if (
      this.productLoadingMore() ||
      !this.productHasMore() ||
      cursorExpirationDate === null ||
      cursorCreatedAt === null ||
      cursorId === null
    ) {
      return;
    }

    const requestVersion = ++this.productLoadVersion;
    this.productLoadingMore.set(true);
    this.productError.set('');
    this.stockEntryService
      .search(
        this.appliedProductQuery,
        this.appliedProductStatus,
        50,
        cursorExpirationDate,
        cursorCreatedAt,
        cursorId,
      )
      .subscribe({
        next: (result) => {
          if (requestVersion !== this.productLoadVersion) {
            return;
          }
          const knownIds = new Set(this.productEntries().map((entry) => entry.id));
          this.productEntries.update((entries) => [
            ...entries,
            ...result.content.filter((entry) => !knownIds.has(entry.id)),
          ]);
          this.setProductCursor(result);
          this.productHasMore.set(result.hasNext);
          this.productLoadingMore.set(false);
        },
        error: (error: HttpErrorResponse) => {
          if (requestVersion !== this.productLoadVersion) {
            return;
          }
          if (error.status === 401) {
            this.logout();
            return;
          }
          this.productError.set('Não foi possível carregar mais produtos.');
          this.productLoadingMore.set(false);
        },
      });
  }

  loadMovements(): void {
    const requestVersion = ++this.movementLoadVersion;
    this.movementLoading.set(true);
    this.movementLoadingMore.set(false);
    this.movementError.set('');
    this.stockMovementService.list().subscribe({
      next: (result) => {
        if (requestVersion !== this.movementLoadVersion) {
          return;
        }
        this.movements.set(result.content);
        this.movementCursorCreatedAt = result.nextCursorCreatedAt;
        this.movementCursorId = result.nextCursorId;
        this.movementHasMore.set(result.hasNext);
        this.movementLoading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        if (requestVersion !== this.movementLoadVersion) {
          return;
        }
        if (error.status === 401) {
          this.logout();
          return;
        }
        this.movementError.set('Não foi possível carregar o histórico.');
        this.movementLoading.set(false);
      },
    });
  }

  loadMoreMovements(): void {
    const cursorCreatedAt = this.movementCursorCreatedAt;
    const cursorId = this.movementCursorId;
    if (
      this.movementLoadingMore() ||
      !this.movementHasMore() ||
      cursorCreatedAt === null ||
      cursorId === null
    ) {
      return;
    }

    const requestVersion = ++this.movementLoadVersion;
    this.movementLoadingMore.set(true);
    this.stockMovementService.list(20, cursorCreatedAt, cursorId).subscribe({
      next: (result) => {
        if (requestVersion !== this.movementLoadVersion) {
          return;
        }
        const knownIds = new Set(this.movements().map((movement) => movement.id));
        this.movements.update((movements) => [
          ...movements,
          ...result.content.filter((movement) => !knownIds.has(movement.id)),
        ]);
        this.movementCursorCreatedAt = result.nextCursorCreatedAt;
        this.movementCursorId = result.nextCursorId;
        this.movementHasMore.set(result.hasNext);
        this.movementLoadingMore.set(false);
      },
      error: (error: HttpErrorResponse) => {
        if (requestVersion !== this.movementLoadVersion) {
          return;
        }
        if (error.status === 401) {
          this.logout();
          return;
        }
        this.movementError.set('Não foi possível carregar mais movimentações.');
        this.movementLoadingMore.set(false);
      },
    });
  }

  loadEntries(entriesToPreserve: StockEntry[] = []): void {
    const requestVersion = ++this.loadVersion;
    this.loading.set(true);
    this.loadingMore.set(false);
    this.errorMessage.set('');
    this.stockEntryService.list(App.HOME_SUMMARY_SIZE).subscribe({
      next: (result) => {
        if (requestVersion !== this.loadVersion) {
          return;
        }
        const pageIds = new Set(result.content.map((entry) => entry.id));
        this.entries.set(
          this.sortEntries([
            ...result.content,
            ...entriesToPreserve.filter((entry) => !pageIds.has(entry.id)),
          ]).slice(0, App.HOME_SUMMARY_SIZE),
        );
        this.inventoryLoaded = true;
        this.setEntryCursor(result);
        this.hasMore.set(result.hasNext);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        if (requestVersion !== this.loadVersion) {
          return;
        }
        if (error.status === 401) {
          this.logout();
          return;
        }
        this.errorMessage.set('Não foi possível carregar os produtos.');
        this.loading.set(false);
      },
    });
  }

  loadMore(): void {
    const cursorExpirationDate = this.entryCursorExpirationDate;
    const cursorCreatedAt = this.entryCursorCreatedAt;
    const cursorId = this.entryCursorId;
    if (
      this.loadingMore() ||
      !this.hasMore() ||
      cursorExpirationDate === null ||
      cursorCreatedAt === null ||
      cursorId === null
    ) {
      return;
    }

    const requestVersion = ++this.loadVersion;
    this.loadingMore.set(true);
    this.stockEntryService.list(50, cursorExpirationDate, cursorCreatedAt, cursorId).subscribe({
      next: (result) => {
        if (requestVersion !== this.loadVersion) {
          return;
        }
        const knownIds = new Set(this.entries().map((entry) => entry.id));
        this.entries.update((entries) =>
          this.sortEntries([
            ...entries,
            ...result.content.filter((entry) => !knownIds.has(entry.id)),
          ]),
        );
        this.setEntryCursor(result);
        this.hasMore.set(result.hasNext);
        this.loadingMore.set(false);
      },
      error: () => {
        if (requestVersion === this.loadVersion) {
          this.errorMessage.set('Não foi possível carregar mais produtos.');
          this.loadingMore.set(false);
        }
      },
    });
  }

  addCreatedEntry(entry: StockEntry): void {
    const needsAuthoritativeReload = !this.inventoryLoaded;
    this.loadVersion += 1;
    this.loading.set(false);
    this.loadingMore.set(false);
    this.errorMessage.set('');
    this.entries.update((entries) =>
      this.sortEntries([...entries, entry]).slice(0, App.HOME_SUMMARY_SIZE),
    );
    this.resetMovements();

    if (needsAuthoritativeReload) {
      this.loadEntries([entry]);
    }
  }

  openDetails(entryId: number): void {
    const requestVersion = ++this.detailsLoadVersion;
    this.detailsOpen.set(true);
    this.detailsLoading.set(true);
    this.detailsError.set('');
    this.withdrawalError.set('');
    this.selectedEntry.set(null);
    this.stockEntryService.details(entryId).subscribe({
      next: (entry) => {
        if (requestVersion !== this.detailsLoadVersion) {
          return;
        }
        this.selectedEntry.set(entry);
        this.detailsLoading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        if (requestVersion !== this.detailsLoadVersion) {
          return;
        }
        if (error.status === 401) {
          this.logout();
          return;
        }
        this.detailsError.set('Não foi possível carregar os detalhes desta entrada.');
        this.detailsLoading.set(false);
      },
    });
  }

  withdrawSelected(withdrawal: WithdrawStock): void {
    const entry = this.selectedEntry();
    if (!entry || this.withdrawalPending()) {
      return;
    }

    this.withdrawalPending.set(true);
    this.withdrawalError.set('');
    this.actionMessage.set('');
    this.stockEntryService.withdraw(entry.id, withdrawal).subscribe({
      next: (updatedEntry) => {
        const entryWasClosed = updatedEntry.availableQuantity === 0;
        this.entries.update((entries) =>
          entryWasClosed
            ? entries.filter((candidate) => candidate.id !== updatedEntry.id)
            : entries.map((candidate) =>
                candidate.id === updatedEntry.id
                  ? { ...candidate, quantity: updatedEntry.availableQuantity }
                  : candidate,
              ),
        );
        this.productEntries.update((entries) =>
          entryWasClosed
            ? entries.filter((candidate) => candidate.id !== updatedEntry.id)
            : entries.map((candidate) =>
                candidate.id === updatedEntry.id
                  ? { ...candidate, quantity: updatedEntry.availableQuantity }
                  : candidate,
              ),
        );
        this.actionMessage.set(
          entryWasClosed
            ? `${updatedEntry.productName} saiu do estoque ativo.`
            : `Saldo de ${updatedEntry.productName} atualizado para ${updatedEntry.availableQuantity}.`,
        );
        this.withdrawalPending.set(false);
        if (this.activeView() === 'history') {
          this.loadMovements();
        } else {
          this.resetMovements();
        }
        const detailsPanel = this.detailsPanel();
        if (detailsPanel) {
          detailsPanel.close();
        } else {
          this.closeDetails();
        }
        if (entryWasClosed) {
          this.loadEntries(this.entries());
          if (this.activeView() === 'products') {
            this.searchProducts();
          }
        }
      },
      error: (error: HttpErrorResponse) => {
        if (error.status === 401) {
          this.logout();
          return;
        }
        this.withdrawalError.set(
          error.error?.detail || 'Não foi possível concluir a retirada. Tente novamente.',
        );
        this.withdrawalPending.set(false);
      },
    });
  }

  closeDetails(): void {
    this.detailsLoadVersion += 1;
    this.detailsOpen.set(false);
    this.selectedEntry.set(null);
    this.detailsLoading.set(false);
    this.detailsError.set('');
    this.withdrawalPending.set(false);
    this.withdrawalError.set('');
  }

  logout(): void {
    this.loadVersion += 1;
    this.authService.logout();
    this.entries.set([]);
    this.loading.set(false);
    this.loadingMore.set(false);
    this.hasMore.set(false);
    this.errorMessage.set('');
    this.inventoryLoaded = false;
    this.entryCursorExpirationDate = null;
    this.entryCursorCreatedAt = null;
    this.entryCursorId = null;
    this.activeView.set('stock');
    void this.router.navigateByUrl('/');
    this.resetProducts();
    this.resetMovements();
    this.closeDetails();
    this.actionMessage.set('');
  }

  private resetMovements(): void {
    this.movementLoadVersion += 1;
    this.movements.set([]);
    this.movementLoading.set(false);
    this.movementLoadingMore.set(false);
    this.movementHasMore.set(false);
    this.movementError.set('');
    this.movementCursorCreatedAt = null;
    this.movementCursorId = null;
  }

  private resetProducts(): void {
    this.productLoadVersion += 1;
    this.productEntries.set([]);
    this.productLoading.set(false);
    this.productLoadingMore.set(false);
    this.productHasMore.set(false);
    this.productError.set('');
    this.productQuery.set('');
    this.productStatus.set('');
    this.productCursorExpirationDate = null;
    this.productCursorCreatedAt = null;
    this.productCursorId = null;
    this.appliedProductQuery = '';
    this.appliedProductStatus = '';
  }

  private activateRoute(url: string): void {
    const path = url.split(/[?#]/, 1)[0].replace(/^\/+|\/+$/g, '');
    const view = path === 'produtos' ? 'products' : path === 'historico' ? 'history' : 'stock';
    if (view !== this.activeView()) {
      this.activateView(view);
    }
  }

  private activateView(view: 'stock' | 'products' | 'history', forceLoad = false): void {
    const changed = view !== this.activeView();
    this.activeView.set(view);
    if (!this.authService.authenticated() || (!changed && !forceLoad)) {
      return;
    }
    if (view === 'products') {
      this.searchProducts();
    } else if (view === 'history') {
      this.loadMovements();
    } else if (!this.inventoryLoaded) {
      this.loadEntries();
    }
  }

  private setEntryCursor(result: StockEntryPage): void {
    this.entryCursorExpirationDate = result.nextCursorExpirationDate;
    this.entryCursorCreatedAt = result.nextCursorCreatedAt;
    this.entryCursorId = result.nextCursorId;
  }

  private setProductCursor(result: StockEntryPage): void {
    this.productCursorExpirationDate = result.nextCursorExpirationDate;
    this.productCursorCreatedAt = result.nextCursorCreatedAt;
    this.productCursorId = result.nextCursorId;
  }

  private clearProductCursor(): void {
    this.productCursorExpirationDate = null;
    this.productCursorCreatedAt = null;
    this.productCursorId = null;
  }

  private sortEntries(entriesToSort: StockEntry[]): StockEntry[] {
    return [...entriesToSort].sort(
      (first, second) =>
        first.expirationDate.localeCompare(second.expirationDate) ||
        Date.parse(first.createdAt) - Date.parse(second.createdAt) ||
        first.id - second.id,
    );
  }
}
