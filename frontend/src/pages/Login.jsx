import { useState } from 'react';
import { api } from '../api.js';

const DEMO_USERS = [
  { label: 'Admin', email: 'admin@demo.com' },
  { label: 'Manager', email: 'manager@demo.com' },
  { label: 'Employee', email: 'priya@demo.com' },
];

export default function Login({ onLogin }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function submit(e) {
    e.preventDefault();
    setError('');
    setBusy(true);
    try {
      const res = await api('/auth/login', { method: 'POST', body: { email, password } });
      onLogin(res.token, res.user);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="login-page">
      <form className="card login-card" onSubmit={submit}>
        <h1>Mini HRMS</h1>
        <p className="muted">Sign in to manage leave and employees</p>

        <label>Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoFocus />
        </label>
        <label>Password
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        </label>

        {error && <div className="alert error">{error}</div>}
        <button className="btn primary full" disabled={busy}>{busy ? 'Signing in…' : 'Sign in'}</button>

        <div className="demo">
          <span className="muted small">Demo accounts (password: password123)</span>
          <div className="demo-buttons">
            {DEMO_USERS.map((u) => (
              <button type="button" key={u.email} className="btn ghost small"
                onClick={() => { setEmail(u.email); setPassword('password123'); }}>
                {u.label}
              </button>
            ))}
          </div>
        </div>
      </form>
    </div>
  );
}
