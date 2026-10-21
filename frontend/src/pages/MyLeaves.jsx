import { useEffect, useState } from 'react';
import { api, formatDate } from '../api.js';
import StatusBadge from './StatusBadge.jsx';

export default function MyLeaves({ user, onUserChange }) {
  const [leaves, setLeaves] = useState([]);
  const [form, setForm] = useState({ startDate: '', endDate: '', reason: '' });
  const [message, setMessage] = useState(null);
  const [busy, setBusy] = useState(false);

  async function load() {
    try {
      const [me, mine] = await Promise.all([api('/me'), api('/leaves/mine')]);
      onUserChange(me);           // refresh balance (it changes when a manager approves)
      setLeaves(mine);
    } catch (err) {
      setMessage({ type: 'error', text: err.message });
    }
  }

  useEffect(() => { load(); }, []);

  async function apply(e) {
    e.preventDefault();
    setBusy(true);
    setMessage(null);
    try {
      await api('/leaves', { method: 'POST', body: form });
      setForm({ startDate: '', endDate: '', reason: '' });
      setMessage({ type: 'success', text: 'Leave request submitted. Your manager will review it.' });
      load();
    } catch (err) {
      setMessage({ type: 'error', text: err.message });
    } finally {
      setBusy(false);
    }
  }

  const pendingDays = leaves.filter((l) => l.status === 'PENDING').reduce((sum, l) => sum + l.days, 0);
  const today = new Date().toISOString().slice(0, 10);

  return (
    <div className="stack">
      <div className="stats">
        <div className="card stat"><span className="muted small">Leave balance</span><strong>{user.leaveBalance} days</strong></div>
        <div className="card stat"><span className="muted small">Pending approval</span><strong>{pendingDays} days</strong></div>
        <div className="card stat"><span className="muted small">Manager</span><strong>{user.managerName || '—'}</strong></div>
      </div>

      <form className="card" onSubmit={apply}>
        <h2>Apply for leave</h2>
        <div className="row">
          <label>From
            <input type="date" min={today} value={form.startDate}
              onChange={(e) => setForm({ ...form, startDate: e.target.value })} required />
          </label>
          <label>To
            <input type="date" min={form.startDate || today} value={form.endDate}
              onChange={(e) => setForm({ ...form, endDate: e.target.value })} required />
          </label>
        </div>
        <label>Reason (optional)
          <input type="text" maxLength={500} value={form.reason}
            onChange={(e) => setForm({ ...form, reason: e.target.value })} />
        </label>
        {message && <div className={`alert ${message.type}`}>{message.text}</div>}
        <button className="btn primary" disabled={busy}>{busy ? 'Submitting…' : 'Submit request'}</button>
      </form>

      <div className="card">
        <h2>My requests</h2>
        {leaves.length === 0 ? <p className="muted">No leave requests yet.</p> : (
          <div className="table-wrap">
            <table>
              <thead><tr><th>Dates</th><th>Days</th><th>Reason</th><th>Status</th><th>Decided by</th></tr></thead>
              <tbody>
                {leaves.map((l) => (
                  <tr key={l.id}>
                    <td>{formatDate(l.startDate)} – {formatDate(l.endDate)}</td>
                    <td>{l.days}</td>
                    <td>{l.reason || <span className="muted">—</span>}</td>
                    <td><StatusBadge status={l.status} /></td>
                    <td>{l.decidedByName || <span className="muted">—</span>}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
