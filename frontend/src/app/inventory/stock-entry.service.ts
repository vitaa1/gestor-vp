import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { CreateStockEntry, StockEntry, StockEntryPage } from './stock-entry.model';

@Injectable({ providedIn: 'root' })
export class StockEntryService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly resourceUrl = '/api/stock-entries';

  list(page = 0, size = 50): Observable<StockEntryPage> {
    return this.http.get<StockEntryPage>(this.resourceUrl, {
      headers: this.authService.headers(),
      params: { page, size },
    });
  }

  create(entry: CreateStockEntry): Observable<StockEntry> {
    return this.http.post<StockEntry>(this.resourceUrl, entry, {
      headers: this.authService.headers(),
    });
  }
}
