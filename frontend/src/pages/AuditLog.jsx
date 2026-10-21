import { useEffect, useState } from 'react';
import { api, formatDateTime } from '../api.js';

export default function AuditLog() {
  const [data, setData] = useState({ items: [], page: 0, totalPages: 0, totalItems: 0 });
  const [page, setPage] = useState(0);
  const [error, setError] = useState('');

  useEffect(() => {
    api(`/admin/audit-logs?page=${page}&size=20`)
      .then(setData)
      .catch((err) => setError(err.message));
  }, [page]);

  return (
    <div className="card">
      <div className="card-head">
        <h2>Audit log</h2>
        <span className="muted small">{data.totalItems} entries</span>
      </div>
      <p className="muted small">Every important change is recorded here. Entries can never be edited or deleted.</p>
      {error && <div className="alert error">{error}</div>}
      <div className="table-wrap">
        <table>
          <thead><tr><th>When</th><th>Who</th><th>Action</th><th>Details</th></tr></thead>
          <tbody>
            {data.items.map((a) => (
              <tr key={a.id}>
                <td className="muted small nowrap">{formatDateTime(a.createdAt)}</td>
                <td>{a.actorName}</td>
                <td><code>{a.action}</code></td>
                <td>{a.details}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {data.totalPages > 1 && (
        <div className="pager">
          <button className="btn ghost small" disabled={page === 0} onClick={() => setPage(page - 1)}>← Newer</button>
          <span className="muted small">Page {page + 1} of {data.totalPages}</span>
          <button className="btn ghost small" disabled={page + 1 >= data.totalPages} onClick={() => setPage(page + 1)}>Older →</button>
        </div>
      )}
    </div>
  );
}
