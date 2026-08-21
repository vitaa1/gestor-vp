export type StockMovementType = 'ENTRY' | 'WITHDRAWAL';

export interface StockMovement {
  id: number;
  stockEntryId: number;
  productName: string;
  expirationDate: string;
  type: StockMovementType;
  typeLabel: string;
  quantity: number;
  reason: string | null;
  reasonLabel: string | null;
  createdAt: string;
  entryClosed: boolean;
}

export interface StockMovementPage {
  content: StockMovement[];
  size: number;
  hasNext: boolean;
  nextCursorCreatedAt: string | null;
  nextCursorId: number | null;
}
