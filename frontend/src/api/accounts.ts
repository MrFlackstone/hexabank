import { api } from './client';

// Tipos espejo de los DTO REST de account-service (AccountResponse / CreateAccountRequest).
export interface Account {
  id: string;
  holder: string;
  balance: number;
  version: number;
}

export interface CreateAccountRequest {
  holder: string;
  initialBalance: number;
}

export const createAccount = (req: CreateAccountRequest) =>
  api.post<Account>('/accounts', req);

export const getAccount = (id: string) => api.get<Account>(`/accounts/${id}`);
