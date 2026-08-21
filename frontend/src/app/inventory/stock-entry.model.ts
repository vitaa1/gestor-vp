export type ExpirationStatus = 'EXPIRED' | 'ATTENTION' | 'WATCH' | 'OK';

export interface StockEntry {
  id: number;
  productName: string;
  barcode: string | null;
  category: string | null;
  quantity: number;
  expirationDate: string;
  unitCost: number | null;
  supplier: string | null;
  batchNumber: string | null;
  status: ExpirationStatus;
  statusLabel: string;
  daysUntilExpiration: number;
  createdAt: string;
}

export interface CreateStockEntry {
  productName: string;
  quantity: number;
  expirationDate: string;
  barcode: string | null;
  category: string | null;
  unitCost: number | null;
  supplier: string | null;
  batchNumber: string | null;
}

export interface StockEntryPage {
  content: StockEntry[];
  size: number;
  hasNext: boolean;
  nextCursorExpirationDate: string | null;
  nextCursorCreatedAt: string | null;
  nextCursorId: number | null;
}

export type WithdrawalReason = 'SOLD' | 'USED' | 'DONATED' | 'LOST' | 'EXPIRED';

export interface StockEntryDetailsModel {
  id: number;
  productName: string;
  barcode: string | null;
  category: string | null;
  initialQuantity: number;
  availableQuantity: number;
  expirationDate: string;
  unitCost: number | null;
  supplier: string | null;
  batchNumber: string | null;
  status: ExpirationStatus;
  statusLabel: string;
  daysUntilExpiration: number;
  createdAt: string;
}

export interface WithdrawStock {
  quantity: number;
  reason: WithdrawalReason;
}
