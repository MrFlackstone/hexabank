import { useEffect, useState } from 'react';
import { useAuth } from 'react-oidc-context';
import { setUnauthorizedHandler } from '../api/client';

// Detecta la sesion expirada por tres vias: el access token caduca sin renovarse
// (addAccessTokenExpired), falla la renovacion silenciosa (addSilentRenewError) o una llamada REST
// responde 401. Se limpia sola cuando vuelve a haber un token vigente (re-login correcto).
export function useSessionExpiry(): { expired: boolean } {
  const auth = useAuth();
  const [expired, setExpired] = useState(false);

  useEffect(() => {
    const markExpired = () => setExpired(true);
    const removeExpired = auth.events.addAccessTokenExpired(markExpired);
    const removeRenewError = auth.events.addSilentRenewError(markExpired);
    setUnauthorizedHandler(markExpired);
    return () => {
      removeExpired();
      removeRenewError();
      setUnauthorizedHandler(() => {});
    };
  }, [auth.events]);

  // Un token vigente (tras un re-login) rearma el estado.
  useEffect(() => {
    if (auth.user && !auth.user.expired) setExpired(false);
  }, [auth.user]);

  return { expired };
}
