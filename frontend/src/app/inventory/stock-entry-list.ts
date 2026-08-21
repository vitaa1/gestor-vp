import { Component, input, output } from '@angular/core';
import { StockEntry } from './stock-entry.model';

@Component({
  selector: 'app-stock-entry-list',
  templateUrl: './stock-entry-list.html',
  styleUrl: './stock-entry-list.scss',
})
export class StockEntryList {
  readonly entries = input<readonly StockEntry[]>([]);
  readonly loading = input(false);
  readonly errorMessage = input('');
  readonly hasMore = input(false);
  readonly loadingMore = input(false);
  readonly headingEyebrow = input('Por ordem de validade');
  readonly headingTitle = input('Produtos no estoque');
  readonly emptyTitle = input('Seu estoque ainda está vazio.');
  readonly emptyDescription = input('Adicione o primeiro produto usando o formulário acima.');
  readonly retryRequested = output<void>();
  readonly loadMoreRequested = output<void>();
  readonly detailsRequested = output<number>();

  formatDate(value: string): string {
    const [year, month, day] = value.split('-');
    return `${day}/${month}/${year}`;
  }

  expirationMessage(entry: StockEntry): string {
    if (entry.daysUntilExpiration < -1) {
      return `Venceu há ${Math.abs(entry.daysUntilExpiration)} dias`;
    }
    if (entry.daysUntilExpiration === -1) {
      return 'Venceu ontem';
    }
    if (entry.daysUntilExpiration === 0) {
      return 'Vence hoje';
    }
    if (entry.daysUntilExpiration === 1) {
      return 'Vence amanhã';
    }
    return `Vence em ${entry.daysUntilExpiration} dias`;
  }
}
