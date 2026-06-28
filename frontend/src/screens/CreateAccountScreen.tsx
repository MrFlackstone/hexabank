import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { createAccount, type Account } from '../api/accounts';
import { ApiError } from '../api/client';

export function CreateAccountScreen() {
  const [holder, setHolder] = useState('');
  const [initialBalance, setInitialBalance] = useState('1000');
  const [created, setCreated] = useState<Account[]>([]);

  const mutation = useMutation({
    mutationFn: createAccount,
    onSuccess: (account) => {
      setCreated((prev) => [account, ...prev]);
      setHolder('');
    },
  });

  const onSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    mutation.mutate({ holder, initialBalance: Number(initialBalance) });
  };

  const copy = (id: string) => navigator.clipboard?.writeText(id);

  return (
    <div className="grid">
      <section className="card">
        <h2>Nueva cuenta</h2>
        <form onSubmit={onSubmit} className="form">
          <label>
            Titular
            <input
              value={holder}
              onChange={(e) => setHolder(e.target.value)}
              placeholder="Ada Lovelace"
              required
            />
          </label>
          <label>
            Saldo inicial (€)
            <input
              type="number"
              min="0"
              step="0.01"
              value={initialBalance}
              onChange={(e) => setInitialBalance(e.target.value)}
              required
            />
          </label>
          <button type="submit" disabled={mutation.isPending}>
            {mutation.isPending ? 'Creando…' : 'Crear cuenta'}
          </button>
          {mutation.isError && (
            <p className="error">
              {mutation.error instanceof ApiError ? mutation.error.message : 'Error inesperado'}
            </p>
          )}
        </form>
      </section>

      <section className="card">
        <h2>Cuentas creadas en esta sesión</h2>
        {created.length === 0 ? (
          <p className="muted">Aún no has creado ninguna cuenta.</p>
        ) : (
          <ul className="account-list">
            {created.map((a) => (
              <li key={a.id}>
                <div>
                  <strong>{a.holder}</strong>
                  <span className="balance">{a.balance.toFixed(2)} €</span>
                </div>
                <button className="link" onClick={() => copy(a.id)} title="Copiar ID">
                  <code>{a.id}</code> ⧉
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
