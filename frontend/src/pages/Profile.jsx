import { useState } from 'react';
import { api } from '../api.js';

export default function Profile({ user }) {
  const [form, setForm] = useState({ currentPassword: '', newPassword: '', confirm: '' });
  const [message, setMessage] = useState(null);

  async function changePassword(e) {
    e.preventDefault();
    if (form.newPassword !== form.confirm) {
      setMessage({ type: 'error', text: 'New passwords do not match.' });
      return;
    }
    try {
      await api('/me/password', {
        method: 'POST',
        body: { currentPassword: form.currentPassword, newPassword: form.newPassword },
      });
      setForm({ currentPassword: '', newPassword: '', confirm: '' });
      setMessage({ type: 'success', text: 'Password changed.' });
    } catch (err) {
      setMessage({ type: 'error', text: err.message });
    }
  }

  return (
    <div className="stack">
      <div className="card">
        <h2>My profile</h2>
        <dl className="details">
          <dt>Name</dt><dd>{user.name}</dd>
          <dt>Email</dt><dd>{user.email}</dd>
          <dt>Role</dt><dd>{user.role}</dd>
          <dt>Department</dt><dd>{user.departmentName || '—'}</dd>
          <dt>Manager</dt><dd>{user.managerName || '—'}</dd>
        </dl>
      </div>
      <form className="card" onSubmit={changePassword}>
        <h2>Change password</h2>
        <label>Current password
          <input type="password" value={form.currentPassword}
            onChange={(e) => setForm({ ...form, currentPassword: e.target.value })} required />
        </label>
        <div className="row">
          <label>New password (min 8)
            <input type="password" minLength={8} value={form.newPassword}
              onChange={(e) => setForm({ ...form, newPassword: e.target.value })} required />
          </label>
          <label>Confirm new password
            <input type="password" minLength={8} value={form.confirm}
              onChange={(e) => setForm({ ...form, confirm: e.target.value })} required />
          </label>
        </div>
        {message && <div className={`alert ${message.type}`}>{message.text}</div>}
        <button className="btn primary">Update password</button>
      </form>
    </div>
  );
}
