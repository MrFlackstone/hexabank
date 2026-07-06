import type { AuthProviderProps } from 'react-oidc-context';
import { WebStorageStateStore } from 'oidc-client-ts';

// Config del cliente OIDC. Flujo Authorization Code + PKCE S256 (response_type 'code' -> la libreria
// activa PKCE por defecto) contra el client publico 'hexabank-frontend'. El authority es el emisor
// PUBLICO (localhost:8180), el mismo 'iss' que validan los 3 resource-servers.
export const oidcConfig: AuthProviderProps = {
  authority: import.meta.env.VITE_KEYCLOAK_AUTHORITY,
  client_id: import.meta.env.VITE_KEYCLOAK_CLIENT_ID,
  redirect_uri: import.meta.env.VITE_KEYCLOAK_REDIRECT_URI,
  post_logout_redirect_uri: import.meta.env.VITE_KEYCLOAK_REDIRECT_URI,
  response_type: 'code',
  scope: 'openid profile',
  // Renueva el access token en segundo plano usando el refresh token antes de que caduque (lifespan
  // 300s en el realm). Sin iframe: al haber refresh token se usa el grant refresh_token directamente.
  automaticSilentRenew: true,
  // Persistir en localStorage para que la sesion sobreviva a recargas (el store por defecto es de
  // sessionStorage, que se pierde al cerrar la pestana).
  userStore: new WebStorageStateStore({ store: window.localStorage }),
  // Tras procesar el callback de Keycloak, limpiar ?code&state de la URL. La SPA es de una sola ruta
  // (pestanas por estado), asi que basta con reescribir a la ruta actual sin query.
  onSigninCallback: () => {
    window.history.replaceState({}, document.title, window.location.pathname);
  },
};
