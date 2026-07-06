import { useTranslation } from 'react-i18next';
import { useLedgerStream } from '../hooks/useLedgerStream';

const STATUS_KEY: Record<string, string> = {
  connecting: 'ledger.statusConnecting',
  live: 'ledger.statusLive',
  error: 'ledger.statusError',
};

export function LiveLedgerScreen() {
  const { t } = useTranslation();
  const { entries, status } = useLedgerStream();

  return (
    <section className="card">
      <div className="ledger-head">
        <h2>{t('ledger.title')}</h2>
        <span className={`stream-status stream-${status}`}>● {t(STATUS_KEY[status])}</span>
      </div>
      <p className="muted">{t('ledger.intro')}</p>

      {entries.length === 0 ? (
        <p className="muted">{t('ledger.empty')}</p>
      ) : (
        <table className="ledger-table">
          <thead>
            <tr>
              <th>{t('ledger.colTime')}</th>
              <th>{t('ledger.colType')}</th>
              <th>{t('ledger.colAccount')}</th>
              <th className="num">{t('ledger.colAmount')}</th>
              <th>{t('ledger.colTransfer')}</th>
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
