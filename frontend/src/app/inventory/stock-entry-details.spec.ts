import { TestBed } from '@angular/core/testing';
import { StockEntryDetails } from './stock-entry-details';
import { StockEntryDetailsModel } from './stock-entry.model';

const activeEntry: StockEntryDetailsModel = {
  id: 1,
  productName: 'Leite Integral',
  initialQuantity: 12,
  availableQuantity: 7,
  expirationDate: '2030-01-10',
  status: 'OK',
  statusLabel: 'Tudo certo',
  daysUntilExpiration: 1200,
  createdAt: '2026-08-15T12:00:00Z',
};

describe('StockEntryDetails', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [StockEntryDetails] }).compileComponents();
  });

  it('shows complete quantities and asks for confirmation before emitting a withdrawal', () => {
    const fixture = TestBed.createComponent(StockEntryDetails);
    fixture.componentRef.setInput('entry', activeEntry);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('Quantidade inicial');
    expect(element.textContent).toContain('12 unidades');
    expect(element.textContent).toContain('Quantidade disponível');
    expect(element.textContent).toContain('7 unidades');

    const quantity = element.querySelector('#withdrawal-quantity') as HTMLInputElement;
    quantity.value = '3';
    quantity.dispatchEvent(new Event('input'));
    const reason = element.querySelector('#withdrawal-reason') as HTMLSelectElement;
    reason.value = 'SOLD';
    reason.dispatchEvent(new Event('change'));
    (element.querySelector('[data-action="review"]') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(element.textContent).toContain('Confirme a retirada');
    expect(element.textContent).toContain('3 unidades');
    expect(element.textContent).toContain('Vendi');
    expect(element.querySelector('[aria-live="polite"]')?.textContent).toContain('Revise o resumo');

    const emitted = vi.fn();
    fixture.componentInstance.withdrawalRequested.subscribe(emitted);
    (element.querySelector('[data-action="confirm"]') as HTMLButtonElement).click();
    expect(emitted).toHaveBeenCalledWith({ quantity: 3, reason: 'SOLD' });
  });

  it('cancels the confirmation without emitting a withdrawal', () => {
    const fixture = TestBed.createComponent(StockEntryDetails);
    fixture.componentRef.setInput('entry', activeEntry);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    const quantity = element.querySelector('#withdrawal-quantity') as HTMLInputElement;
    quantity.value = '2';
    quantity.dispatchEvent(new Event('input'));
    (element.querySelector('[data-action="review"]') as HTMLButtonElement).click();
    fixture.detectChanges();

    const emitted = vi.fn();
    fixture.componentInstance.withdrawalRequested.subscribe(emitted);
    (element.querySelector('[data-action="cancel"]') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(emitted).not.toHaveBeenCalled();
    expect(element.querySelector('[data-action="review"]')).not.toBeNull();
  });

  it('offers only loss and expiration reasons for an expired entry', () => {
    const fixture = TestBed.createComponent(StockEntryDetails);
    fixture.componentRef.setInput('entry', { ...activeEntry, status: 'EXPIRED' });
    fixture.detectChanges();
    const labels = [...(fixture.nativeElement as HTMLElement).querySelectorAll('option')].map(
      (option) => option.textContent?.trim(),
    );

    expect(labels).toContain('Perdi');
    expect(labels).toContain('Venceu');
    expect(labels).not.toContain('Vendi');
    expect(labels).not.toContain('Doei');
  });

  it('opens as a modal dialog and emits close when requested', () => {
    const fixture = TestBed.createComponent(StockEntryDetails);
    fixture.componentRef.setInput('entry', activeEntry);
    const closed = vi.fn();
    fixture.componentInstance.closeRequested.subscribe(closed);
    fixture.detectChanges();

    const dialog = (fixture.nativeElement as HTMLElement).querySelector('dialog');
    expect(dialog?.hasAttribute('open')).toBe(true);

    fixture.componentInstance.requestClose();
    expect(closed).toHaveBeenCalledOnce();
  });
});
