import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, input, output, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { StockEntry } from './stock-entry.model';
import { StockEntryService } from './stock-entry.service';

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
    expirationDate: ['', Validators.required],
  });

  submit(): void {
    this.successMessage.set('');
    this.errorMessage.set('');

    if (this.creationDisabled()) {
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.stockEntryService
      .create({
        ...this.form.getRawValue(),
        productName: this.form.controls.productName.value.trim(),
      })
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: (entry) => {
          this.entryCreated.emit(entry);
          this.successMessage.set('Produto adicionado!');
          this.form.reset({ productName: '', quantity: 1, expirationDate: '' });
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage.set(
            error.status === 0
              ? 'Não foi possível acessar o servidor. Tente novamente.'
              : 'Não foi possível adicionar o produto. Confira os dados e tente novamente.',
          );
        },
      });
  }
}
