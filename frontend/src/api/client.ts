// Cliente HTTP minimo sobre fetch. Todas las peticiones cuelgan de /api (mismo origen):
// en dev lo resuelve el proxy de Vite y en produccion el reverse-proxy de Nginx.
const BASE = '/api';

export class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message);
    this.name = 'ApiError';
  }
}

// Puente token<->modulo: client.ts no es React. Un efecto de nivel superior (AuthGate) registra aqui
// una funcion que devuelve el access token vigente; request() la consulta en cada llamada, asi el
// token renovado se usa sin recablear nada.
let accessTokenProvider: () => string | null = () => null;

export function setAccessTokenProvider(provider: () => string | null): void {
  accessTokenProvider = provider;
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const token = accessTokenProvider();
  // Se fusionan las cabeceras del llamante sobre las base y, por ultimo, el Bearer (headers va
  // despues de ...init en fetch, de modo que no lo pisa el spread).
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(init?.headers as Record<string, string> | undefined),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
  const res = await fetch(`${BASE}${path}`, { ...init, headers });

  if (!res.ok) {
    // Spring devuelve un cuerpo de error (problem+json o validacion). Extraemos un mensaje legible.
    let detail = `${res.status} ${res.statusText}`;
    try {
      const body = await res.json();
      detail = body.message ?? body.detail ?? body.error ?? detail;
    } catch {
      /* respuesta sin cuerpo JSON: nos quedamos con el status */
    }
    throw new ApiError(res.status, detail);
  }

  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}

export const api = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body: unknown) =>
    request<T>(path, { method: 'POST', body: JSON.stringify(body) }),
};
