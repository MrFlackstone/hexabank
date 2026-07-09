// Prueba de carga k6 — transferencias concurrentes sobre la misma cuenta (Fase 11).
//
// Objetivo: demostrar bajo carga real las dos garantias de concurrencia del sistema:
//   1. CERO SOBREGIRO: N transferencias simultaneas sobre una cuenta con saldo limitado nunca dejan
//      el saldo negativo (bloqueo optimista @Version + consumidores idempotentes).
//   2. CORRECCION EXACTA: solo se completan las que caben en el saldo; el resto acaban FAILED.
//
// Como funciona: N VUs disparan una POST /transfers cada uno (concurrencia real). La saga es
// asincrona (PENDING -> DEBITED -> COMPLETED | COMPENSATING -> FAILED), asi que teardown() sondea el
// saldo hasta que se estabiliza y comprueba la invariante. k6 mide de paso throughput y latencia
// (http_req_duration), que el benchmark de virtual threads compara con vthreads OFF vs ON.
//
// Uso:
//   k6 run loadtest/concurrent-transfers.js
//   N=100 INITIAL_BALANCE=100 AMOUNT=10 k6 run loadtest/concurrent-transfers.js
//   (en CI/benchmark los servicios corren con HEXABANK_RATELIMIT_ENABLED=false)
//
// Variables de entorno (con sus valores por defecto):
//   KEYCLOAK_URL=http://localhost:8180   ACCOUNT_URL=http://localhost:8081
//   TRANSFER_URL=http://localhost:8082   N=50   AMOUNT=10   INITIAL_BALANCE=100
//   PROPAGATION_WAIT=5 (seg. de espera a que la proyeccion de ownership llegue a transfer-service)

import http from 'k6/http';
import { check, sleep } from 'k6';

const KEYCLOAK = __ENV.KEYCLOAK_URL || 'http://localhost:8180';
const ACCOUNTS = __ENV.ACCOUNT_URL || 'http://localhost:8081';
const TRANSFERS = __ENV.TRANSFER_URL || 'http://localhost:8082';

const N = parseInt(__ENV.N || '50', 10);
const AMOUNT = parseFloat(__ENV.AMOUNT || '10');
const INITIAL_BALANCE = parseFloat(__ENV.INITIAL_BALANCE || '100');
const PROPAGATION_WAIT = parseInt(__ENV.PROPAGATION_WAIT || '5', 10);

const EXPECTED_COMPLETED = Math.min(N, Math.floor(INITIAL_BALANCE / AMOUNT));
const EXPECTED_FINAL = INITIAL_BALANCE - EXPECTED_COMPLETED * AMOUNT;

export const options = {
  scenarios: {
    // Un VU por transferencia, una iteracion: N POST /transfers practicamente simultaneas.
    concurrent_transfers: {
      executor: 'per-vu-iterations',
      vus: N,
      iterations: 1,
      maxDuration: '60s',
    },
  },
  thresholds: {
    checks: ['rate==1.0'], // cualquier check fallido (incluido el sobregiro) hace fallar el run
    http_req_failed: ['rate<0.05'],
  },
};

function authJsonHeaders(token) {
  return { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' };
}

function getToken() {
  const res = http.post(`${KEYCLOAK}/realms/hexabank/protocol/openid-connect/token`, {
    grant_type: 'password',
    client_id: 'hexabank-frontend',
    username: 'alice',
    password: 'alice',
  });
  check(res, { 'token de alice obtenido': (r) => r.status === 200 });
  return res.json('access_token');
}

function createAccount(token, holder, balance) {
  const res = http.post(
    `${ACCOUNTS}/accounts`,
    JSON.stringify({ holder, initialBalance: balance }),
    { headers: authJsonHeaders(token) },
  );
  check(res, { [`cuenta '${holder}' creada`]: (r) => r.status === 201 });
  return res.json('id');
}

// setup(): token + cuenta origen financiada + destino. Espera a que la proyeccion de ownership
// (evento AccountOpened via outbox/Kafka) llegue a transfer-service, si no las POST darian 404.
export function setup() {
  const token = getToken();
  const source = createAccount(token, 'k6-source', INITIAL_BALANCE);
  const destination = createAccount(token, 'k6-destination', 0);
  sleep(PROPAGATION_WAIT);
  return { token, source, destination };
}

// Cada VU dispara una transferencia origen -> destino. El importe supera el saldo global cuando se
// suman las N, forzando que solo EXPECTED_COMPLETED puedan debitar.
export default function (data) {
  const res = http.post(
    `${TRANSFERS}/transfers`,
    JSON.stringify({
      sourceAccountId: data.source,
      destinationAccountId: data.destination,
      amount: AMOUNT,
    }),
    { headers: authJsonHeaders(data.token) },
  );
  check(res, { 'transferencia aceptada (201)': (r) => r.status === 201 });
}

// teardown(): sondea el saldo hasta estabilizarlo y verifica la invariante de concurrencia.
export function teardown(data) {
  const finalBalance = pollStableBalance(data.token, data.source);
  console.log(
    `Saldo final=${finalBalance} esperado=${EXPECTED_FINAL} ` +
      `(completadas≈${EXPECTED_COMPLETED}/${N}, importe=${AMOUNT}, inicial=${INITIAL_BALANCE})`,
  );
  check(finalBalance, {
    'sin sobregiro (saldo final >= 0)': (b) => b >= 0,
    [`saldo final == ${EXPECTED_FINAL}`]: (b) => Math.abs(b - EXPECTED_FINAL) < 0.001,
  });
}

// Lee el saldo cada segundo hasta 3 lecturas iguales consecutivas (saga asentada) o 40 intentos.
function pollStableBalance(token, accountId) {
  let last = null;
  let stableReads = 0;
  for (let i = 0; i < 40 && stableReads < 3; i++) {
    const res = http.get(`${ACCOUNTS}/accounts/${accountId}`, { headers: authJsonHeaders(token) });
    const balance = parseFloat(res.json('balance'));
    stableReads = last !== null && Math.abs(balance - last) < 0.001 ? stableReads + 1 : 0;
    last = balance;
    sleep(1);
  }
  return last;
}
