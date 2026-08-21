import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { StockMovementPage } from './stock-movement.model';

@Injectable({ providedIn: 'root' })
export class StockMovementService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly resourceUrl = '/api/v1/stock-movements';

  list(size = 20, cursorCreatedAt?: string, cursorId?: number): Observable<StockMovementPage> {
    return this.http.get<StockMovementPage>(this.resourceUrl, {
      headers: this.authService.headers(),
      params:
        cursorCreatedAt === undefined || cursorId === undefined
          ? { size }
          : { size, cursorCreatedAt, cursorId },
    });
  }
}
