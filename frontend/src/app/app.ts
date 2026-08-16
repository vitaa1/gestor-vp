import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal, viewChild } from '@angular/core';
import { AuthService } from './auth/auth.service';
import { Login } from './auth/login';
import { StockEntryDetails } from './inventory/stock-entry-details';
import { StockEntryForm } from './inventory/stock-entry-form';
import { StockEntryList } from './inventory/stock-entry-list';
import { StockEntry, StockEntryDetailsModel, WithdrawStock } from './inventory/stock-entry.model';
import { StockEntryService } from './inventory/stock-entry.service';

@Component({
  selector: 'app-root',
  imports: [Login, StockEntryForm, StockEntryList, StockEntryDetails],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  private readonly stockEntryService = inject(StockEntryService);
  readonly authService = inject(AuthService);
  private loadVersion = 0;
  private currentPage = -1;
  private totalElements = 0;
  private detailsLoadVersion = 0;
  private readonly detailsPanel = viewChild(StockEntryDetails);

  readonly entries = signal<StockEntry[]>([]);
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

  loadEntries(entriesToPreserve: StockEntry[] = []): void {
    const requestVersion = ++this.loadVersion;
    this.loading.set(true);
    this.loadingMore.set(false);
    this.errorMessage.set('');
    this.stockEntryService.list().subscribe({
      next: (result) => {
        if (requestVersion !== this.loadVersion) {
          return;
        }
        const pageIds = new Set(result.content.map((entry) => entry.id));
        this.entries.set(
          [...result.content, ...entriesToPreserve.filter((entry) => !pageIds.has(entry.id))].sort(
            (first, second) =>
              first.expirationDate.localeCompare(second.expirationDate) || first.id - second.id,
          ),
        );
        this.currentPage = result.page;
        this.totalElements = result.totalElements;
        this.hasMore.set(this.entries().length < this.totalElements);
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
    if (this.loadingMore() || !this.hasMore()) {
      return;
    }

    const requestVersion = ++this.loadVersion;
    this.loadingMore.set(true);
    this.stockEntryService.list(this.currentPage + 1).subscribe({
      next: (result) => {
        if (requestVersion !== this.loadVersion) {
          return;
        }
        const knownIds = new Set(this.entries().map((entry) => entry.id));
        this.entries.update((entries) =>
          [...entries, ...result.content.filter((entry) => !knownIds.has(entry.id))].sort(
            (first, second) =>
              first.expirationDate.localeCompare(second.expirationDate) || first.id - second.id,
          ),
        );
        this.currentPage = result.page;
        this.totalElements = result.totalElements;
        this.hasMore.set(this.entries().length < this.totalElements);
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
    const needsAuthoritativeReload = this.currentPage < 0;
    this.loadVersion += 1;
    this.loading.set(false);
    this.loadingMore.set(false);
    this.errorMessage.set('');
    this.totalElements += 1;
    this.entries.update((entries) =>
      [...entries, entry].sort(
        (first, second) =>
          first.expirationDate.localeCompare(second.expirationDate) || first.id - second.id,
      ),
    );
    this.hasMore.set(this.entries().length < this.totalElements);

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
        if (entryWasClosed) {
          this.totalElements = Math.max(0, this.totalElements - 1);
        }
        this.hasMore.set(this.entries().length < this.totalElements);
        this.actionMessage.set(
          entryWasClosed
            ? `${updatedEntry.productName} saiu do estoque ativo.`
            : `Saldo de ${updatedEntry.productName} atualizado para ${updatedEntry.availableQuantity}.`,
        );
        this.withdrawalPending.set(false);
        const detailsPanel = this.detailsPanel();
        if (detailsPanel) {
          detailsPanel.close();
        } else {
          this.closeDetails();
        }
        if (entryWasClosed) {
          this.loadEntries(this.entries());
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
    this.currentPage = -1;
    this.totalElements = 0;
    this.closeDetails();
    this.actionMessage.set('');
  }
}
