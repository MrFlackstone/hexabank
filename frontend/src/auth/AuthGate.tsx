import { useEffect } from 'react';
import { useAuth } from 'react-oidc-context';
import { App } from '../App';
import { setAccessTokenProvider } from '../api/client';
import { LoginScreen } from './LoginScreen';

// Guarda de sesion (la SPA no usa router: gateamos toda el area autenticada). Mientras la libreria
// resuelve el estado inicial / procesa el callback -> splash; error -> reintento; sin sesion ->
// pantalla de login; con sesion -> la app.
export function AuthGate() {
  const auth = useAuth();

  // Registra el token vigente en el cliente HTTP; se reejecuta al renovarse el access token.
  useEffect(() => {
    setAccessTokenProvider(() => auth.user?.access_token ?? null);
  }, [auth.user]);

  if (auth.isLoading) {
    return <div className="auth-splash">Cargando sesión…</div>;
  }

  if (auth.error) {
    return (
      <div className="auth-splash">
        <p className="error">Error de autenticación: {auth.error.message}</p>
        <button type="submit" onClick={() => void auth.signinRedirect()}>
          Reintentar
        </button>
      </div>
    );
  }

  if (!auth.isAuthenticated) {
    return <LoginScreen />;
  }

  return <App />;
}
