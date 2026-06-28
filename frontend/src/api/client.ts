// Cliente HTTP minimo sobre fetch. Todas las peticiones cuelgan de /api (mismo origen):
// en dev lo resuelve el proxy de Vite y en produccion el reverse-proxy de Nginx.
const BASE = '/api';

export class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message);
    this.name = 'ApiError';
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...init,
  });

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
