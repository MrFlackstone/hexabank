import { useAuth } from 'react-oidc-context';

// Pantalla previa al login: unica vista accesible sin sesion. El boton lanza el redirect a Keycloak
// (Authorization Code + PKCE); al volver, AuthGate ya renderiza la app.
export function LoginScreen() {
  const auth = useAuth();

  return (
    <div className="app">
      <header className="app-header">
        <h1>
          Hexa<span className="accent">Bank</span>
        </h1>
        <p className="subtitle">Saga de transferencias sobre Kafka · arquitectura hexagonal · CQRS</p>
      </header>

      <main className="content centered-content">
        <section className="card login-card">
          <h2>Inicia sesión</h2>
          <p className="muted">
            Necesitas una sesión para operar. Usuarios de demo: <code>alice</code> / <code>bob</code>{' '}
            (cliente) · <code>staff</code> (personal de banco). La contraseña coincide con el usuario.
          </p>
          <button type="submit" onClick={() => void auth.signinRedirect()}>
            Entrar con Keycloak
          </button>
        </section>
      </main>
    </div>
  );
}
