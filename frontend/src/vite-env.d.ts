/// <reference types="vite/client" />

// Config OIDC inyectada por Vite (prefijo VITE_ para exponerse al cliente). Valores locales en
// .env.development; en produccion se definen al construir la imagen del frontend.
interface ImportMetaEnv {
  readonly VITE_KEYCLOAK_AUTHORITY: string;
  readonly VITE_KEYCLOAK_CLIENT_ID: string;
  readonly VITE_KEYCLOAK_REDIRECT_URI: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
