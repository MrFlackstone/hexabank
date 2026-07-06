import { useAuth } from 'react-oidc-context';

// Los roles de realm viajan en el access token (claim realm_access.roles), no en el id token; se
// decodifica el payload solo para mostrarlos (los mismos que el backend mapea a ROLE_*).
function realmRoles(accessToken?: string): string[] {
  if (!accessToken) return [];
  try {
    const payload = JSON.parse(atob(accessToken.split('.')[1])) as {
      realm_access?: { roles?: string[] };
    };
    return (payload.realm_access?.roles ?? []).filter((r) => r === 'USER' || r === 'STAFF');
  } catch {
    return [];
  }
}

// Chip de sesion en la cabecera: usuario, roles y logout (end-session en Keycloak -> vuelve a la app
// sin sesion).
export function SessionBar() {
  const auth = useAuth();
  const username = auth.user?.profile.preferred_username ?? 'usuario';
  const roles = realmRoles(auth.user?.access_token);

  return (
    <div className="session-bar">
      <span className="session-user">
        <strong>{username}</strong>
        {roles.map((role) => (
          <span key={role} className={`role-chip role-${role.toLowerCase()}`}>
            {role}
          </span>
        ))}
      </span>
      <button className="link" onClick={() => void auth.signoutRedirect()}>
        Cerrar sesión
      </button>
    </div>
  );
}
