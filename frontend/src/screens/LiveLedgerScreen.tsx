import { useLedgerStream } from '../hooks/useLedgerStream';

const STATUS_LABEL: Record<string, string> = {
  connecting: 'conectando…',
  live: 'en vivo',
  error: 'desconectado',
};

export function LiveLedgerScreen() {
  const { entries, status } = useLedgerStream();

  return (
    <section className="card">
      <div className="ledger-head">
        <h2>Libro mayor</h2>
        <span className={`stream-status stream-${status}`}>● {STATUS_LABEL[status]}</span>
      </div>
      <p className="muted">
        Cada transferencia genera asientos (DEBIT/CREDIT) que llegan en tiempo real por SSE.
      </p>

      {entries.length === 0 ? (
        <p className="muted">Sin asientos todavía. Lanza una transferencia para verlos aparecer.</p>
      ) : (
        <table className="ledger-table">
          <thead>
            <tr>
              <th>Hora</th>
              <th>Tipo</th>
              <th>Cuenta</th>
              <th className="num">Importe</th>
              <th>Transferencia</th>
            </tr>
          </thead>
          <tbody>
            {entries.map((e) => (
              <tr key={e.entryId}>
                <td>{new Date(e.createdAt).toLocaleTimeString()}</td>
                <td>
                  <span className={`tag tag-${e.type.toLowerCase()}`}>{e.type}</span>
                </td>
                <td>
                  <code>{e.accountId.slice(0, 8)}</code>
                </td>
                <td className="num">{e.amount.toFixed(2)} €</td>
                <td>
                  <code>{e.transferId.slice(0, 8)}</code>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
