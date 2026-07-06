import { useAuth } from 'react-oidc-context';
import { useTranslation } from 'react-i18next';
import { LanguageSwitcher } from '../i18n/LanguageSwitcher';

// Pantalla previa al login: unica vista accesible sin sesion. El boton lanza el redirect a Keycloak
// (Authorization Code + PKCE); al volver, AuthGate ya renderiza la app.
export function LoginScreen() {
  const auth = useAuth();
  const { t } = useTranslation();

  return (
    <div className="app">
      <header className="app-header">
        <div className="app-title">
          <h1>
            Hexa<span className="accent">Bank</span>
          </h1>
          <p className="subtitle">{t('app.subtitle')}</p>
        </div>
        <LanguageSwitcher />
      </header>

      <main className="content centered-content">
        <section className="card login-card">
          <h2>{t('login.title')}</h2>
          <p className="muted">{t('login.intro')}</p>
          <button type="submit" onClick={() => void auth.signinRedirect()}>
            {t('login.cta')}
          </button>
        </section>
      </main>
    </div>
  );
}
