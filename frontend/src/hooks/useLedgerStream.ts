import { useEffect, useRef, useState } from 'react';
import { useAuth } from 'react-oidc-context';
import { fetchEventSource } from '@microsoft/fetch-event-source';
import { getRecentLedger, type LedgerEntry } from '../api/ledger';

type Status = 'connecting' | 'live' | 'error';

// Suscripcion al feed en vivo del libro mayor por Server-Sent Events.
//
// El backend emite eventos con nombre "ledger-entry" (ver SseLedgerBroadcaster), por lo que solo se
// procesan los mensajes con ese nombre. Primero cargamos el historico reciente por REST y luego
// abrimos el stream; los asientos nuevos se anteponen para que lo ultimo quede arriba.
//
// El stream esta protegido y el token viaja en la cabecera Authorization: Bearer. Por eso NO se usa
// el EventSource nativo (que no puede enviar cabeceras) sino fetchEventSource, que hace un fetch con
// streaming y permite la cabecera -> el token no aparece en la URL (ni en logs, historial o Referer).
// El efecto depende del token: al renovarse (lifespan 300s < read-timeout SSE 3600s) se aborta y se
// reabre el stream con el token fresco, evitando un 401 en un reconnect con token caducado.
export function useLedgerStream() {
  const auth = useAuth();
  const token = auth.user?.access_token;
  const [entries, setEntries] = useState<LedgerEntry[]>([]);
  const [status, setStatus] = useState<Status>('connecting');
  const seenIds = useRef(new Set<string>());

  useEffect(() => {
    if (!token) return;
    const ctrl = new AbortController();
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

    void fetchEventSource('/api/ledger/stream', {
      headers: { Authorization: `Bearer ${token}` },
      signal: ctrl.signal,
      openWhenHidden: true, // no cerrar el stream al ocultar la pestana (comportamiento como el nativo)
      async onopen(res) {
        if (cancelled) return;
        const eventStream = res.headers.get('content-type')?.includes('text/event-stream');
        if (res.ok && eventStream) {
          setStatus('live');
          return;
        }
        // 401/403/otro: error fatal, se corta (lanzar detiene los reintentos de la libreria).
        setStatus('error');
        throw new Error(`SSE ${res.status}`);
      },
      onmessage(ev) {
        if (cancelled || ev.event !== 'ledger-entry') return;
        add(JSON.parse(ev.data) as LedgerEntry, true);
      },
      onerror() {
        // Error transitorio: se marca y NO se relanza -> la libreria reintenta (como el EventSource
        // nativo). Un fallo de auth (401/403) lo corta onopen; un reintento tras caducar el token
        // vuelve a pasar por onopen (no-ok) y termina, y el efecto reabre al renovarse el token.
        if (!cancelled) setStatus('error');
      },
    }).catch(() => {
      /* abort en el cleanup o error fatal: el efecto reabrira al cambiar el token */
    });

    return () => {
      cancelled = true;
      ctrl.abort();
    };
  }, [token]);

  return { entries, status };
}
