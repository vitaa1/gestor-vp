import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { StockEntryForm } from './stock-entry-form';
import { StockEntry } from './stock-entry.model';
import { StockEntryService } from './stock-entry.service';

const createdEntry: StockEntry = {
  id: 1,
  productName: 'Leite Integral',
  quantity: 12,
  expirationDate: '2026-08-20',
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

    component.form.setValue({ productName: '', quantity: 0, expirationDate: '' });
    component.submit();

    expect(component.form.invalid).toBe(true);
    expect(service.create).not.toHaveBeenCalled();
  });

  it('should reject whitespace-only names and decimal quantities', () => {
    const fixture = TestBed.createComponent(StockEntryForm);
    const component = fixture.componentInstance;

    component.form.setValue({ productName: '   ', quantity: 1.5, expirationDate: '2026-08-20' });
    component.submit();

    expect(component.form.invalid).toBe(true);
    expect(service.create).not.toHaveBeenCalled();
  });

  it('should not submit while inventory data is loading', () => {
    const fixture = TestBed.createComponent(StockEntryForm);
    fixture.componentRef.setInput('creationDisabled', true);
    const component = fixture.componentInstance;
    component.form.setValue({
      productName: 'Leite Integral',
      quantity: 12,
      expirationDate: '2026-08-20',
    });

    component.submit();

    expect(service.create).not.toHaveBeenCalled();
  });

  it('should create an entry and confirm it to the user', () => {
    const fixture = TestBed.createComponent(StockEntryForm);
    const component = fixture.componentInstance;
    const emitted: StockEntry[] = [];
    component.entryCreated.subscribe((entry) => emitted.push(entry));
    component.form.setValue({
      productName: 'Leite Integral',
      quantity: 12,
      expirationDate: '2026-08-20',
    });

    component.submit();

    expect(service.create).toHaveBeenCalledWith({
      productName: 'Leite Integral',
      quantity: 12,
      expirationDate: '2026-08-20',
    });
    expect(emitted).toEqual([createdEntry]);
    expect(component.successMessage()).toBe('Produto adicionado!');
  });
});
