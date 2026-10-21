import { useEffect, useState } from 'react';
import { api, getToken, setToken } from './api.js';
import Login from './pages/Login.jsx';
import MyLeaves from './pages/MyLeaves.jsx';
import Approvals from './pages/Approvals.jsx';
import Employees from './pages/Employees.jsx';
import AuditLog from './pages/AuditLog.jsx';
import Profile from './pages/Profile.jsx';

// The tabs each role can see. NOTE: hiding tabs is only for convenience.
// The real protection is on the backend, which checks the role on every request.
function tabsFor(role) {
  const tabs = [{ id: 'leaves', label: 'My leave' }];
  if (role === 'ADMIN' || role === 'MANAGER') {
    tabs.push({ id: 'approvals', label: 'Approvals' });
    tabs.push({ id: 'employees', label: role === 'ADMIN' ? 'Employees' : 'My team' });
  }
  if (role === 'ADMIN') tabs.push({ id: 'audit', label: 'Audit log' });
  tabs.push({ id: 'profile', label: 'Profile' });
  return tabs;
}

export default function App() {
  const [user, setUser] = useState(null);
  const [checking, setChecking] = useState(true);
  const [tab, setTab] = useState('leaves');

  useEffect(() => {
    const onLogout = () => setUser(null);
    window.addEventListener('hrms:logout', onLogout);

    // If we already have a token (page refresh), ask the backend who we are
    if (getToken()) {
      api('/me')
        .then(setUser)
        .catch(() => setToken(null))
        .finally(() => setChecking(false));
    } else {
      setChecking(false);
    }
    return () => window.removeEventListener('hrms:logout', onLogout);
  }, []);

  function handleLogin(token, loggedInUser) {
    setToken(token);
    setUser(loggedInUser);
    setTab('leaves');
  }

  function logout() {
    setToken(null);
    setUser(null);
  }

  if (checking) return <div className="center muted">Loading…</div>;
  if (!user) return <Login onLogin={handleLogin} />;

  const tabs = tabsFor(user.role);

  return (
    <div className="app">
      <header className="topbar">
        <div className="brand">Mini HRMS</div>
        <div className="who">
          <span>{user.name}</span>
          <span className={`badge role-${user.role.toLowerCase()}`}>{user.role}</span>
          <button className="btn ghost small" onClick={logout}>Log out</button>
        </div>
      </header>

      <nav className="tabs">
        {tabs.map((t) => (
          <button key={t.id} className={tab === t.id ? 'tab active' : 'tab'} onClick={() => setTab(t.id)}>
            {t.label}
          </button>
        ))}
      </nav>

      <main className="content">
        {tab === 'leaves' && <MyLeaves user={user} onUserChange={setUser} />}
        {tab === 'approvals' && <Approvals />}
        {tab === 'employees' && <Employees currentUser={user} />}
        {tab === 'audit' && <AuditLog />}
        {tab === 'profile' && <Profile user={user} />}
      </main>
    </div>
  );
}
