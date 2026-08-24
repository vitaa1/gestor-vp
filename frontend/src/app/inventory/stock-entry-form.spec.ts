import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { StockEntryForm } from './stock-entry-form';
import { StockEntry } from './stock-entry.model';
import { StockEntryService } from './stock-entry.service';

const createdEntry: StockEntry = {
  id: 1,
  productName: 'Leite Integral',
  barcode: null,
  category: null,
  quantity: 12,
  expirationDate: '2026-08-20',
  unitCost: null,
  supplier: null,
  batchNumber: null,
  status: 'ATTENTION',
  statusLabel: 'Atenção',
  daysUntilExpiration: 6,
  createdAt: '2026-08-14T12:00:00Z',
};

describe('StockEntryForm', () => {
  const service = {
    create: vi.fn(() => of(createdEntry)),
  };

  beforeEach(async () => {
    service.create.mockClear();
    await TestBed.configureTestingModule({
      imports: [StockEntryForm],
      providers: [{ provide: StockEntryService, useValue: service }],
    }).compileComponents();
  });

  it('should require name, quantity and expiration date', () => {
    const fixture = TestBed.createComponent(StockEntryForm);
    const component = fixture.componentInstance;

    component.form.patchValue({ productName: '', quantity: 0, expirationDate: '' });
    component.submit();

    expect(component.form.invalid).toBe(true);
    expect(service.create).not.toHaveBeenCalled();
  });

  it('allows the expiration date field to shrink inside the mobile layout', () => {
    const fixture = TestBed.createComponent(StockEntryForm);
    fixture.detectChanges();
    const expirationDate = (fixture.nativeElement as HTMLElement).querySelector<HTMLInputElement>(
      '#expiration-date',
    );
    const field = expirationDate?.closest<HTMLElement>('.field');

    expect(expirationDate).not.toBeNull();
    expect(field).not.toBeNull();
    expect(getComputedStyle(field!).minWidth).toBe('0px');
    expect(getComputedStyle(expirationDate!).minWidth).toBe('0px');
    expect(getComputedStyle(expirationDate!).maxWidth).toBe('100%');
  });

  it('should reject whitespace-only names and decimal quantities', () => {
    const fixture = TestBed.createComponent(StockEntryForm);
    const component = fixture.componentInstance;

    component.form.patchValue({
      productName: '   ',
      quantity: 1.5,
      expirationDate: '20/08/2026',
    });
    component.submit();

    expect(component.form.invalid).toBe(true);
    expect(service.create).not.toHaveBeenCalled();
  });

  it('formats the expiration date as day, month and year while typing', () => {
    const fixture = TestBed.createComponent(StockEntryForm);
    fixture.detectChanges();
    const expirationDate = (fixture.nativeElement as HTMLElement).querySelector<HTMLInputElement>(
      '#expiration-date',
    )!;

    expirationDate.value = '31122035';
    expirationDate.dispatchEvent(new Event('input'));

    expect(expirationDate.value).toBe('31/12/2035');
    expect(fixture.componentInstance.form.controls.expirationDate.value).toBe('31/12/2035');
  });

  it('preserves the date segments when correcting the month with the keyboard', () => {
    const fixture = TestBed.createComponent(StockEntryForm);
    fixture.detectChanges();
    const expirationDate = (fixture.nativeElement as HTMLElement).querySelector<HTMLInputElement>(
      '#expiration-date',
    )!;

    expirationDate.value = '31/1/2035';
    expirationDate.setSelectionRange(4, 4);
    expirationDate.dispatchEvent(new Event('input'));

    const caret = expirationDate.selectionStart!;
    expirationDate.value = `${expirationDate.value.slice(0, caret)}1${expirationDate.value.slice(caret)}`;
    expirationDate.setSelectionRange(caret + 1, caret + 1);
    expirationDate.dispatchEvent(new Event('input'));

    expect(expirationDate.value).toBe('31/11/2035');
    expect(expirationDate.selectionStart).toBe(5);
    expect(fixture.componentInstance.form.controls.expirationDate.value).toBe('31/11/2035');
  });

  it('rejects an expiration date that does not exist', () => {
    const fixture = TestBed.createComponent(StockEntryForm);
    const component = fixture.componentInstance;
    component.form.patchValue({
      productName: 'Leite Integral',
      quantity: 1,
      expirationDate: '31/02/2035',
    });

    component.submit();

    expect(component.form.controls.expirationDate.hasError('invalidDate')).toBe(true);
    expect(service.create).not.toHaveBeenCalled();
  });

  it('should not submit while inventory data is loading', () => {
    const fixture = TestBed.createComponent(StockEntryForm);
    fixture.componentRef.setInput('creationDisabled', true);
    const component = fixture.componentInstance;
    component.form.patchValue({
      productName: 'Leite Integral',
      quantity: 12,
      expirationDate: '20/08/2026',
    });

    component.submit();

    expect(service.create).not.toHaveBeenCalled();
  });

  it('should create an entry and confirm it to the user', () => {
    const fixture = TestBed.createComponent(StockEntryForm);
    const component = fixture.componentInstance;
    const emitted: StockEntry[] = [];
    component.entryCreated.subscribe((entry) => emitted.push(entry));
    component.form.patchValue({
      productName: 'Leite Integral',
      quantity: 12,
      expirationDate: '20/08/2026',
    });

    component.submit();

    expect(service.create).toHaveBeenCalledWith({
      productName: 'Leite Integral',
      quantity: 12,
      expirationDate: '2026-08-20',
      barcode: null,
      category: null,
      unitCost: null,
      supplier: null,
      batchNumber: null,
    });
    expect(emitted).toEqual([createdEntry]);
    expect(component.successMessage()).toBe('Produto adicionado!');

    fixture.detectChanges();
    const success = (fixture.nativeElement as HTMLElement).querySelector('.feedback__success');
    const icon = success?.querySelector('.feedback__success-icon');
    expect(success?.textContent).toContain('Produto adicionado!');
    expect(icon?.textContent).toBe('✓');
    expect(icon?.getAttribute('aria-hidden')).toBe('true');
  });

  it('starts with optional fields collapsed and reveals the invalid barcode', () => {
    const fixture = TestBed.createComponent(StockEntryForm);
    fixture.detectChanges();
    const details = (fixture.nativeElement as HTMLElement).querySelector('details');
    expect(details?.hasAttribute('open')).toBe(false);

    const component = fixture.componentInstance;
    component.form.patchValue({
      productName: 'Leite Integral',
      quantity: 12,
      expirationDate: '2026-08-20',
      barcode: '12AB',
    });
    component.submit();

    fixture.detectChanges();

    expect(component.form.controls.barcode.hasError('pattern')).toBe(true);
    expect(details?.open).toBe(true);
    expect(document.activeElement).toBe(
      (fixture.nativeElement as HTMLElement).querySelector('#barcode'),
    );
    expect(service.create).not.toHaveBeenCalled();
  });

  it('trims and sends the optional product and stock details', () => {
    const fixture = TestBed.createComponent(StockEntryForm);
    const component = fixture.componentInstance;
    component.form.patchValue({
      productName: ' Café Especial ',
      quantity: 6,
      expirationDate: '15/04/2030',
      barcode: '07891234567890',
      category: ' Mercearia ',
      unitCost: '18.75',
      supplier: ' Torrefação Central ',
      batchNumber: ' LOTE-2030-A ',
    });

    component.submit();

    expect(service.create).toHaveBeenCalledWith({
      productName: 'Café Especial',
      quantity: 6,
      expirationDate: '2030-04-15',
      barcode: '07891234567890',
      category: 'Mercearia',
      unitCost: 18.75,
      supplier: 'Torrefação Central',
      batchNumber: 'LOTE-2030-A',
    });
  });

  it('shows the barcode conflict returned by the API', () => {
    service.create.mockReturnValueOnce(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 409,
            error: { detail: 'O código de barras informado já pertence a outro produto.' },
          }),
      ),
    );
    const fixture = TestBed.createComponent(StockEntryForm);
    const component = fixture.componentInstance;
    component.form.patchValue({
      productName: 'Produto B',
      quantity: 1,
      expirationDate: '01/01/2030',
      barcode: '7891234567890',
    });

    component.submit();

    expect(component.errorMessage()).toBe(
      'O código de barras informado já pertence a outro produto.',
    );
  });
});
