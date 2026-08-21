import { TestBed } from '@angular/core/testing';
import { StockMovement } from './stock-movement.model';
import { StockMovementList } from './stock-movement-list';

const movements: StockMovement[] = [
  {
    id: 2,
    stockEntryId: 1,
    productName: 'Leite Integral',
    expirationDate: '2030-01-10',
    type: 'WITHDRAWAL',
    typeLabel: 'Retirada',
    quantity: 5,
    reason: 'SOLD',
    reasonLabel: 'Vendi',
    createdAt: '2026-08-15T18:30:00Z',
    entryClosed: true,
  },
  {
    id: 1,
    stockEntryId: 1,
    productName: 'Leite Integral',
    expirationDate: '2030-01-10',
    type: 'ENTRY',
    typeLabel: 'Entrada',
    quantity: 12,
    reason: null,
    reasonLabel: null,
    createdAt: '2026-08-14T12:00:00Z',
    entryClosed: false,
  },
];

describe('StockMovementList', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [StockMovementList] }).compileComponents();
  });

  it('shows one movement per row with all applicable information', () => {
    const fixture = TestBed.createComponent(StockMovementList);
    fixture.componentRef.setInput('movements', movements);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    const rows = element.querySelectorAll('.movement-row');

    expect(rows).toHaveLength(2);
    expect(rows[0].textContent).toContain('Leite Integral');
    expect(rows[0].textContent).toContain('10/01/2030');
    expect(rows[0].textContent).toContain('Retirada');
    expect(rows[0].textContent).toContain('5 unidades');
    expect(rows[0].textContent).toContain('Vendi');
    expect(rows[0].textContent).toMatch(/15\/08\/2026.*\d{2}:\d{2}/s);
    expect(rows[1].textContent).toContain('Entrada');
    expect(rows[1].textContent).not.toContain('Motivo');
  });

  it('opens an entry from its movement and identifies closed entries as read only', () => {
    const fixture = TestBed.createComponent(StockMovementList);
    fixture.componentRef.setInput('movements', movements);
    const detailsRequested = vi.fn();
    fixture.componentInstance.detailsRequested.subscribe(detailsRequested);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    const button = element.querySelector(
      'button[aria-label="Consultar entrada encerrada de Leite Integral"]',
    ) as HTMLButtonElement;

    expect(button.textContent).toContain('Consultar entrada encerrada');
    button.click();
    expect(detailsRequested).toHaveBeenCalledWith(1);
  });

  it('requests another page without replacing the current rows', () => {
    const fixture = TestBed.createComponent(StockMovementList);
    fixture.componentRef.setInput('movements', movements);
    fixture.componentRef.setInput('hasMore', true);
    const loadMoreRequested = vi.fn();
    fixture.componentInstance.loadMoreRequested.subscribe(loadMoreRequested);
    fixture.detectChanges();

    const button = (fixture.nativeElement as HTMLElement).querySelector(
      '.load-more',
    ) as HTMLButtonElement;
    button.click();

    expect(loadMoreRequested).toHaveBeenCalledOnce();
    expect((fixture.nativeElement as HTMLElement).querySelectorAll('.movement-row')).toHaveLength(
      2,
    );
  });
});
