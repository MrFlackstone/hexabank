import { useAuth } from 'react-oidc-context';
import { useSessionExpiry } from './useSessionExpiry';

// Aviso persistente cuando la sesion expira: el usuario reentra (nuevo login) o cierra sesion. No se
// puede seguir operando porque las llamadas responderian 401.
export function SessionExpiredBanner() {
  const auth = useAuth();
  const { expired } = useSessionExpiry();

  if (!expired) return null;

  return (
    <div className="session-expired" role="alert">
      <span>Tu sesión ha expirado. Vuelve a entrar para seguir operando.</span>
      <div className="session-expired-actions">
        <button type="submit" onClick={() => void auth.signinRedirect()}>
          Volver a entrar
        </button>
        <button className="link" onClick={() => void auth.signoutRedirect()}>
          Cerrar sesión
        </button>
      </div>
    </div>
  );
}
