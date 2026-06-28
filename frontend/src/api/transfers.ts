import { api } from './client';

// --- Lado COMANDO (transfer-service, el orquestador de la saga) ---
// Espejo de TransferResponse: 'status' es el estado de la maquina de estados de la saga.
export interface Transfer {
  id: string;
  sourceAccountId: string;
  destinationAccountId: string;
  amount: number;
  status: string;
  version: number;
}

export interface CreateTransferRequest {
  sourceAccountId: string;
  destinationAccountId: string;
  amount: number;
}

// --- Lado LECTURA (ledger-service, proyeccion CQRS construida desde eventos) ---
// Espejo de TransferViewResponse. Es el read-model: se actualiza de forma asincrona
// al consumir los eventos de la saga, por eso puede ir "un paso por detras" del orquestador.
export interface TransferView {
  transferId: string;
  sourceAccountId: string;
  destinationAccountId: string;
  amount: number;
  status: string;
}

// Maquina de estados de la saga: PENDING -> DEBITED -> COMPLETED (exito) o
// DEBITED -> COMPENSATING -> FAILED (compensacion) o PENDING -> FAILED (debito rechazado).
// Terminales: COMPLETED y FAILED. Al alcanzarlos dejamos de hacer polling.
export const TERMINAL_STATUSES = ['COMPLETED', 'FAILED'];

export const isTerminal = (status?: string) =>
  status != null && TERMINAL_STATUSES.includes(status);

export const requestTransfer = (req: CreateTransferRequest) =>
  api.post<Transfer>('/transfers', req);

// Estado autoritativo del orquestador.
export const getTransfer = (id: string) => api.get<Transfer>(`/transfers/${id}`);

// Proyeccion de lectura del ledger (CQRS). Va por un prefijo distinto en el proxy para
// desambiguar la colision de path con el orquestador (/transfers/{id} existe en ambos).
export const getTransferView = (id: string) =>
  api.get<TransferView>(`/projections/transfers/${id}`);
