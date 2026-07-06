import { useEffect } from 'react';
import { useAuth } from 'react-oidc-context';
import { useTranslation } from 'react-i18next';
import { App } from '../App';
import { setAccessTokenProvider } from '../api/client';
import { LoginScreen } from './LoginScreen';
import { SessionExpiredBanner } from './SessionExpiredBanner';

// Guarda de sesion (la SPA no usa router: gateamos toda el area autenticada). Mientras la libreria
// resuelve el estado inicial / procesa el callback -> splash; error -> reintento; sin sesion ->
// pantalla de login; con sesion -> la app.
export function AuthGate() {
  const auth = useAuth();
  const { t } = useTranslation();

  // Registra el token vigente en el cliente HTTP; se reejecuta al renovarse el access token.
  useEffect(() => {
    setAccessTokenProvider(() => auth.user?.access_token ?? null);
  }, [auth.user]);

  if (auth.isLoading) {
    return <div className="auth-splash">{t('session.loading')}</div>;
  }

  if (auth.error) {
    return (
      <div className="auth-splash">
        <p className="error">{t('session.authError', { message: auth.error.message })}</p>
        <button type="submit" onClick={() => void auth.signinRedirect()}>
          {t('session.retry')}
        </button>
      </div>
    );
  }

  if (!auth.isAuthenticated) {
    return <LoginScreen />;
  }

  return (
    <>
      <SessionExpiredBanner />
      <App />
    </>
  );
}
