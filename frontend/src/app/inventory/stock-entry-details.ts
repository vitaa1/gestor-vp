import {
  AfterViewInit,
  Component,
  computed,
  ElementRef,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { StockEntryDetailsModel, WithdrawalReason, WithdrawStock } from './stock-entry.model';

interface ReasonOption {
  value: WithdrawalReason;
  label: string;
}

@Component({
  selector: 'app-stock-entry-details',
  templateUrl: './stock-entry-details.html',
  styleUrl: './stock-entry-details.scss',
})
export class StockEntryDetails implements AfterViewInit {
  readonly entry = input<StockEntryDetailsModel | null>(null);
  readonly loading = input(false);
  readonly errorMessage = input('');
  readonly withdrawalError = input('');
  readonly withdrawalPending = input(false);
  readonly closeRequested = output<void>();
  readonly withdrawalRequested = output<WithdrawStock>();

  readonly quantity = signal<number | null>(null);
  readonly reason = signal<WithdrawalReason>('SOLD');
  readonly confirming = signal(false);
  readonly validationMessage = signal('');
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('detailsDialog');

  private readonly allReasons: readonly ReasonOption[] = [
    { value: 'SOLD', label: 'Vendi' },
    { value: 'USED', label: 'Usei' },
    { value: 'DONATED', label: 'Doei' },
    { value: 'LOST', label: 'Perdi' },
    { value: 'EXPIRED', label: 'Venceu' },
  ];

  readonly availableReasons = computed(() =>
    this.entry()?.status === 'EXPIRED'
      ? this.allReasons.filter((reason) => reason.value === 'LOST' || reason.value === 'EXPIRED')
      : this.allReasons,
  );

  ngAfterViewInit(): void {
    const dialog = this.dialog().nativeElement;
    if (typeof dialog.showModal === 'function') {
      dialog.showModal();
    } else {
      dialog.setAttribute('open', '');
    }
  }

  requestClose(): void {
    if (!this.withdrawalPending()) {
      this.close();
    }
  }

  close(): void {
    const dialog = this.dialog().nativeElement;
    if (typeof dialog.close === 'function' && dialog.open) {
      dialog.close();
    } else {
      dialog.removeAttribute('open');
      this.closeRequested.emit();
    }
  }

  handleCancel(event: Event): void {
    if (this.withdrawalPending()) {
      event.preventDefault();
    }
  }

  handleClosed(): void {
    this.closeRequested.emit();
  }

  updateQuantity(event: Event): void {
    const value = Number((event.target as HTMLInputElement).value);
    this.quantity.set(Number.isFinite(value) ? value : null);
    this.validationMessage.set('');
  }

  updateReason(event: Event): void {
    this.reason.set((event.target as HTMLSelectElement).value as WithdrawalReason);
  }

  reviewWithdrawal(): void {
    const entry = this.entry();
    const quantity = this.quantity();
    if (!entry || quantity === null || !Number.isInteger(quantity) || quantity <= 0) {
      this.validationMessage.set('Informe uma quantidade inteira maior que zero.');
      return;
    }
    if (quantity > entry.availableQuantity) {
      this.validationMessage.set('A quantidade informada supera o saldo disponível.');
      return;
    }
    if (!this.availableReasons().some((reason) => reason.value === this.reason())) {
      this.reason.set(this.availableReasons()[0].value);
    }
    this.validationMessage.set('');
    this.confirming.set(true);
  }

  cancelConfirmation(): void {
    this.confirming.set(false);
  }

  confirmWithdrawal(): void {
    const quantity = this.quantity();
    if (quantity !== null) {
      this.withdrawalRequested.emit({ quantity, reason: this.reason() });
    }
  }

  reasonLabel(): string {
    return this.availableReasons().find((reason) => reason.value === this.reason())?.label ?? '';
  }

  formatDate(value: string): string {
    const [year, month, day] = value.split('-');
    return `${day}/${month}/${year}`;
  }
}
