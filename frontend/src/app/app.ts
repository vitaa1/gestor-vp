import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { AuthService } from './auth/auth.service';
import { Login } from './auth/login';
import { StockEntryForm } from './inventory/stock-entry-form';
import { StockEntryList } from './inventory/stock-entry-list';
import { StockEntry } from './inventory/stock-entry.model';
import { StockEntryService } from './inventory/stock-entry.service';

@Component({
  selector: 'app-root',
  imports: [Login, StockEntryForm, StockEntryList],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  private readonly stockEntryService = inject(StockEntryService);
  readonly authService = inject(AuthService);
  private loadVersion = 0;
  private currentPage = -1;
  private totalElements = 0;

  readonly entries = signal<StockEntry[]>([]);
  readonly loading = signal(false);
  readonly loadingMore = signal(false);
  readonly hasMore = signal(false);
  readonly errorMessage = signal('');

  loadEntries(entriesToPreserve: StockEntry[] = []): void {
    const requestVersion = ++this.loadVersion;
    this.loading.set(true);
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
  }
}
