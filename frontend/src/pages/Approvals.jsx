import { useEffect, useState } from 'react';
import { api, formatDate, formatDateTime } from '../api.js';

export default function Approvals() {
  const [pending, setPending] = useState([]);
  const [message, setMessage] = useState(null);
  const [busyId, setBusyId] = useState(null);
  const [loaded, setLoaded] = useState(false);

  async function load() {
    try {
      setPending(await api('/leaves/pending'));
    } catch (err) {
      setMessage({ type: 'error', text: err.message });
    } finally {
      setLoaded(true);
    }
  }

  useEffect(() => { load(); }, []);

  async function decide(leave, approve) {
    let note = null;
    if (!approve) {
      note = window.prompt(`Reason for rejecting ${leave.employeeName}'s request (optional):`, '');
      if (note === null) return;               // user pressed Cancel
    }
    setBusyId(leave.id);
    setMessage(null);
    try {
      await api(`/leaves/${leave.id}/decision`, { method: 'POST', body: { approve, note } });
      setMessage({ type: 'success', text: `${approve ? 'Approved' : 'Rejected'} ${leave.employeeName}'s leave.` });
      load();
    } catch (err) {
      setMessage({ type: 'error', text: err.message });
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div className="card">
      <h2>Waiting for your decision</h2>
      {message && <div className={`alert ${message.type}`}>{message.text}</div>}
      {loaded && pending.length === 0 && <p className="muted">Nothing to review. All caught up.</p>}
      {pending.length > 0 && (
        <div className="table-wrap">
          <table>
            <thead><tr><th>Employee</th><th>Dates</th><th>Days</th><th>Reason</th><th>Requested</th><th></th></tr></thead>
            <tbody>
              {pending.map((l) => (
                <tr key={l.id}>
                  <td>{l.employeeName}</td>
                  <td>{formatDate(l.startDate)} – {formatDate(l.endDate)}</td>
                  <td>{l.days}</td>
                  <td>{l.reason || <span className="muted">—</span>}</td>
                  <td className="muted small">{formatDateTime(l.createdAt)}</td>
                  <td className="actions">
                    <button className="btn primary small" disabled={busyId === l.id} onClick={() => decide(l, true)}>Approve</button>
                    <button className="btn danger small" disabled={busyId === l.id} onClick={() => decide(l, false)}>Reject</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
