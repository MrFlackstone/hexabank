import type { AuthProviderProps } from 'react-oidc-context';

// Config del cliente OIDC. Flujo Authorization Code + PKCE S256 (response_type 'code' -> la libreria
// activa PKCE por defecto) contra el client publico 'hexabank-frontend'. El authority es el emisor
// PUBLICO (localhost:8180), el mismo 'iss' que validan los 3 resource-servers.
//
// Almacenamiento: se usa el store por defecto (sessionStorage), NO localStorage. Asi el refresh
// token no queda persistido y legible por cualquier script (menor superficie de exfiltracion por
// XSS); sobrevive al F5 pero se limpia al cerrar la pestana. El modelo verdaderamente seguro seria
// un BFF con cookie httpOnly (custodia del refresh token en servidor) -> evolucion futura.
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
  // Tras procesar el callback de Keycloak, limpiar ?code&state de la URL. La SPA es de una sola ruta
  // (pestanas por estado), asi que basta con reescribir a la ruta actual sin query.
  onSigninCallback: () => {
    window.history.replaceState({}, document.title, window.location.pathname);
  },
};
