import { Component, input, output } from '@angular/core';
import { StockMovement } from './stock-movement.model';

@Component({
  selector: 'app-stock-movement-list',
  templateUrl: './stock-movement-list.html',
  styleUrl: './stock-movement-list.scss',
})
export class StockMovementList {
  readonly movements = input.required<StockMovement[]>();
  readonly loading = input(false);
  readonly loadingMore = input(false);
  readonly hasMore = input(false);
  readonly errorMessage = input('');
  readonly retryRequested = output<void>();
  readonly loadMoreRequested = output<void>();
  readonly detailsRequested = output<number>();

  formatDate(value: string): string {
    const [year, month, day] = value.split('-');
    return `${day}/${month}/${year}`;
  }

  formatDateTime(value: string): string {
    return new Intl.DateTimeFormat('pt-BR', {
      dateStyle: 'short',
      timeStyle: 'short',
    }).format(new Date(value));
  }
}
