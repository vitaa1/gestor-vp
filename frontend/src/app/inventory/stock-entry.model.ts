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
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
