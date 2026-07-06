import { useAuth } from 'react-oidc-context';
import { useTranslation } from 'react-i18next';
import { useSessionExpiry } from './useSessionExpiry';

// Aviso persistente cuando la sesion expira: el usuario reentra (nuevo login) o cierra sesion. No se
// puede seguir operando porque las llamadas responderian 401.
export function SessionExpiredBanner() {
  const auth = useAuth();
  const { t } = useTranslation();
  const { expired } = useSessionExpiry();

  if (!expired) return null;

  return (
    <div className="session-expired" role="alert">
      <span>{t('session.expired')}</span>
      <div className="session-expired-actions">
        <button type="submit" onClick={() => void auth.signinRedirect()}>
          {t('session.reenter')}
        </button>
        <button className="link" onClick={() => void auth.signoutRedirect()}>
          {t('session.logout')}
        </button>
      </div>
    </div>
  );
}
