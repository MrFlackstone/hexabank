import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { CreateAccountScreen } from './screens/CreateAccountScreen';
import { TransferScreen } from './screens/TransferScreen';
import { LiveLedgerScreen } from './screens/LiveLedgerScreen';
import { SessionBar } from './auth/SessionBar';

type Tab = 'accounts' | 'transfer' | 'ledger';

const TABS: Tab[] = ['accounts', 'transfer', 'ledger'];

export function App() {
  const { t } = useTranslation();
  const [tab, setTab] = useState<Tab>('accounts');

  return (
    <div className="app">
      <header className="app-header">
        <div className="app-title">
          <h1>
            Hexa<span className="accent">Bank</span>
          </h1>
          <p className="subtitle">{t('app.subtitle')}</p>
        </div>
        <SessionBar />
      </header>

      <nav className="tabs">
        {TABS.map((id) => (
          <button
            key={id}
            className={`tab ${tab === id ? 'tab-active' : ''}`}
            onClick={() => setTab(id)}
          >
            {t(`nav.${id}`)}
          </button>
        ))}
      </nav>

      <main className="content">
        {tab === 'accounts' && <CreateAccountScreen />}
        {tab === 'transfer' && <TransferScreen />}
        {tab === 'ledger' && <LiveLedgerScreen />}
      </main>
    </div>
  );
}
