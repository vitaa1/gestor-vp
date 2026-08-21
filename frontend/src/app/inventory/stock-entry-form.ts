import { HttpErrorResponse } from '@angular/common/http';
import { Component, ElementRef, inject, input, output, signal, viewChild } from '@angular/core';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { finalize } from 'rxjs';
import { StockEntry } from './stock-entry.model';
import { StockEntryService } from './stock-entry.service';

const BRAZILIAN_DATE_PATTERN = /^\d{2}\/\d{2}\/\d{4}$/;

function existingBrazilianDate(control: AbstractControl<string>): ValidationErrors | null {
  if (!BRAZILIAN_DATE_PATTERN.test(control.value)) {
    return null;
  }

  const [dayText, monthText, yearText] = control.value.split('/');
  const day = Number(dayText);
  const month = Number(monthText);
  const year = Number(yearText);
  const leapYear = year % 4 === 0 && (year % 100 !== 0 || year % 400 === 0);
  const daysInMonth = [31, leapYear ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];

  return year >= 1 && month >= 1 && month <= 12 && day >= 1 && day <= daysInMonth[month - 1]
    ? null
    : { invalidDate: true };
}

@Component({
  selector: 'app-stock-entry-form',
  imports: [ReactiveFormsModule],
  templateUrl: './stock-entry-form.html',
  styleUrl: './stock-entry-form.scss',
})
export class StockEntryForm {
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly stockEntryService = inject(StockEntryService);

  readonly entryCreated = output<StockEntry>();
  readonly creationDisabled = input(false);
  readonly submitting = signal(false);
  readonly successMessage = signal('');
  readonly errorMessage = signal('');
  private readonly optionalDetails = viewChild<ElementRef<HTMLDetailsElement>>('optionalDetails');

  readonly form = this.formBuilder.group({
    productName: [
      '',
      [Validators.required, Validators.maxLength(120), Validators.pattern(/.*\S.*/)],
    ],
    quantity: [
      1,
      [
        Validators.required,
        Validators.min(1),
        Validators.max(2_147_483_647),
        Validators.pattern(/^\d+$/),
      ],
    ],
    expirationDate: [
      '',
      [Validators.required, Validators.pattern(BRAZILIAN_DATE_PATTERN), existingBrazilianDate],
    ],
    barcode: ['', Validators.pattern(/^\d{8,14}$/)],
    category: ['', Validators.maxLength(120)],
    unitCost: [
      '',
      [
        Validators.min(0),
        Validators.max(9_999_999_999.99),
        Validators.pattern(/^\d+(?:\.\d{1,2})?$/),
      ],
    ],
    supplier: ['', Validators.maxLength(120)],
    batchNumber: ['', Validators.maxLength(120)],
  });

  submit(): void {
    this.successMessage.set('');
    this.errorMessage.set('');

    if (this.creationDisabled()) {
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.revealFirstInvalidOptionalField();
      return;
    }

    this.submitting.set(true);
    const value = this.form.getRawValue();
    this.stockEntryService
      .create({
        productName: value.productName.trim(),
        quantity: value.quantity,
        expirationDate: this.toIsoDate(value.expirationDate),
        barcode: this.optionalText(value.barcode),
        category: this.optionalText(value.category),
        unitCost: value.unitCost === '' ? null : Number(value.unitCost),
        supplier: this.optionalText(value.supplier),
        batchNumber: this.optionalText(value.batchNumber),
      })
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: (entry) => {
          this.entryCreated.emit(entry);
          this.successMessage.set('Produto adicionado!');
          this.form.reset({
            productName: '',
            quantity: 1,
            expirationDate: '',
            barcode: '',
            category: '',
            unitCost: '',
            supplier: '',
            batchNumber: '',
          });
        },
        error: (error: HttpErrorResponse) => {
          const conflictDetail =
            error.status === 409 && typeof error.error?.detail === 'string'
              ? error.error.detail
              : null;
          this.errorMessage.set(
            error.status === 0
              ? 'Não foi possível acessar o servidor. Tente novamente.'
              : (conflictDetail ??
                  'Não foi possível adicionar o produto. Confira os dados e tente novamente.'),
          );
        },
      });
  }

  private optionalText(value: string): string | null {
    const trimmed = value.trim();
    return trimmed === '' ? null : trimmed;
  }

  formatExpirationDate(event: Event): void {
    const input = event.target as HTMLInputElement;
    const caret = input.selectionStart ?? input.value.length;
    const digitsBeforeCaret = input.value.slice(0, caret).replace(/\D/g, '').length;
    const digits = input.value.replace(/\D/g, '').slice(0, 8);
    const maskedDate = [digits.slice(0, 2), digits.slice(2, 4), digits.slice(4, 8)]
      .filter((part) => part !== '')
      .join('/');

    input.value = maskedDate;
    this.form.controls.expirationDate.setValue(maskedDate, { emitEvent: false });
    const maskedCaret = this.caretAfterDigits(maskedDate, digitsBeforeCaret);
    input.setSelectionRange(maskedCaret, maskedCaret);
  }

  private caretAfterDigits(value: string, digitCount: number): number {
    if (digitCount === 0) {
      return 0;
    }

    let seenDigits = 0;
    for (let index = 0; index < value.length; index += 1) {
      if (/\d/.test(value[index])) {
        seenDigits += 1;
      }
      if (seenDigits === digitCount) {
        return index + 1;
      }
    }
    return value.length;
  }

  private toIsoDate(value: string): string {
    const [day, month, year] = value.split('/');
    return `${year}-${month}-${day}`;
  }

  private revealFirstInvalidOptionalField(): void {
    const optionalFields = [
      ['barcode', 'barcode'],
      ['category', 'category'],
      ['unitCost', 'unit-cost'],
      ['supplier', 'supplier'],
      ['batchNumber', 'batch-number'],
    ] as const;
    const invalidField = optionalFields.find(
      ([controlName]) => this.form.controls[controlName].invalid,
    );
    const details = this.optionalDetails()?.nativeElement;
    if (invalidField === undefined || details === undefined) {
      return;
    }

    details.open = true;
    details.querySelector<HTMLInputElement>(`#${invalidField[1]}`)?.focus();
  }
}
