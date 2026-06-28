import { api } from './client';

// Espejo de LedgerEntryResponse. 'type' es DEBIT/CREDIT (el tipo de asiento contable).
export interface LedgerEntry {
  entryId: string;
  transferId: string;
  accountId: string;
  type: string;
  amount: number;
  createdAt: string;
}

// Carga inicial de los asientos recientes; el feed en vivo llega despues por SSE.
export const getRecentLedger = () => api.get<LedgerEntry[]>('/ledger');
