import { useEffect, useRef, useState } from 'react';
import { getRecentLedger, type LedgerEntry } from '../api/ledger';

type Status = 'connecting' | 'live' | 'error';

// Suscripcion al feed en vivo del libro mayor por Server-Sent Events.
//
// El backend emite eventos con nombre "ledger-entry" (ver SseLedgerBroadcaster), por lo que hay
// que escucharlos con addEventListener('ledger-entry', ...) y no con onmessage (que solo recibe
// eventos sin nombre). Primero cargamos el historico reciente por REST y luego abrimos el stream;
// los asientos nuevos se anteponen para que lo ultimo quede arriba.
export function useLedgerStream() {
  const [entries, setEntries] = useState<LedgerEntry[]>([]);
  const [status, setStatus] = useState<Status>('connecting');
  const seenIds = useRef(new Set<string>());

  useEffect(() => {
    let cancelled = false;

    const add = (entry: LedgerEntry, prepend: boolean) => {
      if (seenIds.current.has(entry.entryId)) return;
      seenIds.current.add(entry.entryId);
      setEntries((prev) => (prepend ? [entry, ...prev] : [...prev, entry]));
    };

    getRecentLedger()
      .then((initial) => {
        if (cancelled) return;
        // recentEntries viene en orden descendente; lo mantenemos tal cual (lo nuevo arriba).
        initial.forEach((e) => add(e, false));
      })
      .catch(() => {
        /* si falla la carga inicial seguimos: el stream rellenara en vivo */
      });

    const source = new EventSource('/api/ledger/stream');
    source.onopen = () => !cancelled && setStatus('live');
    source.addEventListener('ledger-entry', (ev) => {
      if (cancelled) return;
      add(JSON.parse((ev as MessageEvent).data) as LedgerEntry, true);
    });
    source.onerror = () => !cancelled && setStatus('error');

    return () => {
      cancelled = true;
      source.close();
    };
  }, []);

  return { entries, status };
}
