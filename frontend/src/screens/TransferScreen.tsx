import { useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import {
  requestTransfer,
  getTransfer,
  getTransferView,
  isTerminal,
} from '../api/transfers';
import { ApiError } from '../api/client';

function StatusBadge({ status }: { status?: string }) {
  if (!status) return <span className="badge badge-pending">—</span>;
  const tone = status === 'COMPLETED' ? 'ok' : status === 'FAILED' ? 'fail' : 'pending';
  return <span className={`badge badge-${tone}`}>{status}</span>;
}

export function TransferScreen() {
  const [sourceAccountId, setSource] = useState('');
  const [destinationAccountId, setDest] = useState('');
  const [amount, setAmount] = useState('100');
  const [transferId, setTransferId] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: requestTransfer,
    onSuccess: (t) => setTransferId(t.id),
  });

  // Lado COMANDO: estado autoritativo del orquestador. Poll hasta estado terminal.
  const orchestrator = useQuery({
    queryKey: ['transfer', transferId],
    queryFn: () => getTransfer(transferId!),
    enabled: !!transferId,
    refetchInterval: (q) => (isTerminal(q.state.data?.status) ? false : 800),
  });

  // Lado LECTURA: proyeccion CQRS del ledger. Puede ir por detras (404 hasta el primer evento).
  const projection = useQuery({
    queryKey: ['transfer-view', transferId],
    queryFn: () => getTransferView(transferId!),
    enabled: !!transferId,
    retry: false,
    refetchInterval: () => (isTerminal(orchestrator.data?.status) ? false : 800),
  });

  const onSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setTransferId(null);
    mutation.mutate({
      sourceAccountId,
      destinationAccountId,
      amount: Number(amount),
    });
  };

  const projectionNotReady =
    projection.isError && projection.error instanceof ApiError && projection.error.status === 404;

  return (
    <div className="grid">
      <section className="card">
        <h2>Nueva transferencia</h2>
        <form onSubmit={onSubmit} className="form">
          <label>
            Cuenta origen (ID)
            <input value={sourceAccountId} onChange={(e) => setSource(e.target.value)} required />
          </label>
          <label>
            Cuenta destino (ID)
            <input
              value={destinationAccountId}
              onChange={(e) => setDest(e.target.value)}
              required
            />
          </label>
          <label>
            Importe (€)
            <input
              type="number"
              min="0.01"
              step="0.01"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              required
            />
          </label>
          <button type="submit" disabled={mutation.isPending}>
            {mutation.isPending ? 'Enviando…' : 'Transferir'}
          </button>
          {mutation.isError && (
            <p className="error">
              {mutation.error instanceof ApiError ? mutation.error.message : 'Error inesperado'}
            </p>
          )}
        </form>
      </section>

      {transferId && (
        <section className="card">
          <h2>Seguimiento de la saga</h2>
          <p className="muted">
            ID transferencia: <code>{transferId}</code>
          </p>

          <div className="cqrs">
            <div className="cqrs-panel">
              <h3>Orquestador (transfer-service)</h3>
              <p className="hint">Estado autoritativo · máquina de estados de la saga</p>
              <StatusBadge status={orchestrator.data?.status} />
              {!isTerminal(orchestrator.data?.status) && orchestrator.data && (
                <span className="polling">↻ en curso…</span>
              )}
            </div>

            <div className="cqrs-panel">
              <h3>Proyección de lectura (ledger-service)</h3>
              <p className="hint">Read-model CQRS · construido desde eventos (asíncrono)</p>
              {projectionNotReady ? (
                <span className="polling">aún no proyectada…</span>
              ) : (
                <StatusBadge status={projection.data?.status} />
              )}
            </div>
          </div>

          <p className="footnote">
            El orquestador y la proyección convergen al mismo estado final, pero el read-model puede
            ir un instante por detrás: ahí se ve la consistencia eventual de CQRS.
          </p>
        </section>
      )}
    </div>
  );
}
