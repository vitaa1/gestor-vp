export type ExpirationStatus = 'EXPIRED' | 'ATTENTION' | 'WATCH' | 'OK';

export interface StockEntry {
  id: number;
  productName: string;
  quantity: number;
  expirationDate: string;
  status: ExpirationStatus;
  statusLabel: string;
  daysUntilExpiration: number;
  createdAt: string;
}

export interface CreateStockEntry {
  productName: string;
  quantity: number;
  expirationDate: string;
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
  initialQuantity: number;
  availableQuantity: number;
  expirationDate: string;
  status: ExpirationStatus;
  statusLabel: string;
  daysUntilExpiration: number;
  createdAt: string;
}

export interface WithdrawStock {
  quantity: number;
  reason: WithdrawalReason;
}
