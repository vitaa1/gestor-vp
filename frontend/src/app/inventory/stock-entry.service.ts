import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import {
  CreateStockEntry,
  StockEntry,
  StockEntryDetailsModel,
  StockEntryPage,
  WithdrawStock,
} from './stock-entry.model';

@Injectable({ providedIn: 'root' })
export class StockEntryService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly resourceUrl = '/api/v1/stock-entries';

  list(
    size = 50,
    cursorExpirationDate?: string,
    cursorCreatedAt?: string,
    cursorId?: number,
  ): Observable<StockEntryPage> {
    return this.http.get<StockEntryPage>(this.resourceUrl, {
      headers: this.authService.headers(),
      params:
        cursorExpirationDate === undefined ||
        cursorCreatedAt === undefined ||
        cursorId === undefined
          ? { size }
          : { size, cursorExpirationDate, cursorCreatedAt, cursorId },
    });
  }

  create(entry: CreateStockEntry): Observable<StockEntry> {
    return this.http.post<StockEntry>(this.resourceUrl, entry, {
      headers: this.authService.headers(),
    });
  }

  details(entryId: number): Observable<StockEntryDetailsModel> {
    return this.http.get<StockEntryDetailsModel>(`${this.resourceUrl}/${entryId}`, {
      headers: this.authService.headers(),
    });
  }

  withdraw(entryId: number, withdrawal: WithdrawStock): Observable<StockEntryDetailsModel> {
    return this.http.post<StockEntryDetailsModel>(
      `${this.resourceUrl}/${entryId}/withdrawals`,
      withdrawal,
      { headers: this.authService.headers() },
    );
  }
}
