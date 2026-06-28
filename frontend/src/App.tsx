import { useState } from 'react';
import { CreateAccountScreen } from './screens/CreateAccountScreen';
import { TransferScreen } from './screens/TransferScreen';
import { LiveLedgerScreen } from './screens/LiveLedgerScreen';

type Tab = 'accounts' | 'transfer' | 'ledger';

const TABS: { id: Tab; label: string }[] = [
  { id: 'accounts', label: 'Crear cuenta' },
  { id: 'transfer', label: 'Transferir' },
  { id: 'ledger', label: 'Libro mayor en vivo' },
];

export function App() {
  const [tab, setTab] = useState<Tab>('accounts');

  return (
    <div className="app">
      <header className="app-header">
        <h1>
          Hexa<span className="accent">Bank</span>
        </h1>
        <p className="subtitle">Saga de transferencias sobre Kafka · arquitectura hexagonal · CQRS</p>
      </header>

      <nav className="tabs">
        {TABS.map((t) => (
          <button
            key={t.id}
            className={`tab ${tab === t.id ? 'tab-active' : ''}`}
            onClick={() => setTab(t.id)}
          >
            {t.label}
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
