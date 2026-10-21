import { useEffect, useState } from 'react';
import { api } from '../api.js';

const EMPTY_FORM = { name: '', email: '', password: '', role: 'EMPLOYEE', departmentId: '', managerId: '', leaveBalance: 20 };

export default function Employees({ currentUser }) {
  const isAdmin = currentUser.role === 'ADMIN';
  const [employees, setEmployees] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [form, setForm] = useState(EMPTY_FORM);
  const [editingId, setEditingId] = useState(null);   // null = adding a new employee
  const [showForm, setShowForm] = useState(false);
  const [message, setMessage] = useState(null);
  const [csvFile, setCsvFile] = useState(null);
  const [importResult, setImportResult] = useState(null);

  async function load() {
    try {
      const [emps, depts] = await Promise.all([api('/employees'), api('/departments')]);
      setEmployees(emps);
      setDepartments(depts);
    } catch (err) {
      setMessage({ type: 'error', text: err.message });
    }
  }

  useEffect(() => { load(); }, []);

  const managers = employees.filter((e) => e.active && e.role !== 'EMPLOYEE' && e.id !== editingId);

  function startAdd() {
    setEditingId(null);
    setForm(EMPTY_FORM);
    setShowForm(true);
    setMessage(null);
  }

  function startEdit(e) {
    setEditingId(e.id);
    setForm({
      name: e.name, email: e.email, password: '', role: e.role,
      departmentId: e.departmentId ?? '', managerId: e.managerId ?? '', leaveBalance: e.leaveBalance,
    });
    setShowForm(true);
    setMessage(null);
  }

  async function save(ev) {
    ev.preventDefault();
    setMessage(null);
    // Empty select = null, numbers as numbers
    const common = {
      name: form.name,
      role: form.role,
      departmentId: form.departmentId === '' ? null : Number(form.departmentId),
      managerId: form.managerId === '' ? null : Number(form.managerId),
      leaveBalance: Number(form.leaveBalance),
    };
    try {
      if (editingId) {
        await api(`/employees/${editingId}`, { method: 'PUT', body: common });
        setMessage({ type: 'success', text: 'Employee updated.' });
      } else {
        await api('/employees', { method: 'POST', body: { ...common, email: form.email, password: form.password } });
        setMessage({ type: 'success', text: 'Employee added.' });
      }
      setShowForm(false);
      load();
    } catch (err) {
      setMessage({ type: 'error', text: err.message });
    }
  }

  async function toggleActive(e) {
    if (e.active && !window.confirm(`Deactivate ${e.name}? They will no longer be able to log in.`)) return;
    try {
      await api(`/employees/${e.id}/active`, { method: 'PATCH', body: { active: !e.active } });
      load();
    } catch (err) {
      setMessage({ type: 'error', text: err.message });
    }
  }

  async function importCsv(ev) {
    ev.preventDefault();
    if (!csvFile) return;
    setImportResult(null);
    const data = new FormData();
    data.append('file', csvFile);
    try {
      const result = await api('/employees/import', { method: 'POST', body: data, isForm: true });
      setImportResult(result);
      if (result.imported > 0) load();
    } catch (err) {
      setImportResult({ imported: 0, errors: [{ row: 0, message: err.message }] });
    }
  }

  return (
    <div className="stack">
      <div className="card">
        <div className="card-head">
          <h2>{isAdmin ? 'All employees' : 'My team'}</h2>
          {isAdmin && <button className="btn primary small" onClick={startAdd}>+ Add employee</button>}
        </div>
        {message && <div className={`alert ${message.type}`}>{message.text}</div>}

        {showForm && (
          <form className="subform" onSubmit={save}>
            <h3>{editingId ? 'Edit employee' : 'New employee'}</h3>
            <div className="row">
              <label>Name<input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required /></label>
              <label>Email<input type="email" value={form.email} disabled={!!editingId}
                onChange={(e) => setForm({ ...form, email: e.target.value })} required /></label>
            </div>
            {!editingId && (
              <label>Initial password (min 8 characters)
                <input type="password" minLength={8} value={form.password}
                  onChange={(e) => setForm({ ...form, password: e.target.value })} required />
              </label>
            )}
            <div className="row">
              <label>Role
                <select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })}>
                  <option value="EMPLOYEE">Employee</option>
                  <option value="MANAGER">Manager</option>
                  <option value="ADMIN">Admin</option>
                </select>
              </label>
              <label>Department
                <select value={form.departmentId} onChange={(e) => setForm({ ...form, departmentId: e.target.value })}>
                  <option value="">— None —</option>
                  {departments.map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}
                </select>
              </label>
            </div>
            <div className="row">
              <label>Manager
                <select value={form.managerId} onChange={(e) => setForm({ ...form, managerId: e.target.value })}>
                  <option value="">— None —</option>
                  {managers.map((m) => <option key={m.id} value={m.id}>{m.name}</option>)}
                </select>
              </label>
              <label>Leave balance (days)
                <input type="number" min={0} max={365} value={form.leaveBalance}
                  onChange={(e) => setForm({ ...form, leaveBalance: e.target.value })} required />
              </label>
            </div>
            <div className="actions">
              <button className="btn primary">{editingId ? 'Save changes' : 'Add employee'}</button>
              <button type="button" className="btn ghost" onClick={() => setShowForm(false)}>Cancel</button>
            </div>
          </form>
        )}

        <div className="table-wrap">
          <table>
            <thead><tr><th>Name</th><th>Email</th><th>Role</th><th>Department</th><th>Manager</th><th>Balance</th><th>Status</th>{isAdmin && <th></th>}</tr></thead>
            <tbody>
              {employees.map((e) => (
                <tr key={e.id} className={e.active ? '' : 'inactive'}>
                  <td>{e.name}</td>
                  <td>{e.email}</td>
                  <td><span className={`badge role-${e.role.toLowerCase()}`}>{e.role}</span></td>
                  <td>{e.departmentName || '—'}</td>
                  <td>{e.managerName || '—'}</td>
                  <td>{e.leaveBalance}</td>
                  <td>{e.active ? 'Active' : 'Inactive'}</td>
                  {isAdmin && (
                    <td className="actions">
                      <button className="btn ghost small" onClick={() => startEdit(e)}>Edit</button>
                      {e.id !== currentUser.id && (
                        <button className="btn ghost small" onClick={() => toggleActive(e)}>
                          {e.active ? 'Deactivate' : 'Activate'}
                        </button>
                      )}
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {isAdmin && (
        <form className="card" onSubmit={importCsv}>
          <h2>Import employees from CSV</h2>
          <p className="muted small">
            First line must be: <code>name,email,role,department,manager_email</code><br />
            Example: <code>Kiran Das,kiran@demo.com,EMPLOYEE,Engineering,manager@demo.com</code><br />
            If any row has a problem, nothing is imported and you'll see which rows to fix.
            Imported employees get the default password <code>Welcome@123</code>.
          </p>
          <div className="actions">
            <input type="file" accept=".csv,text/csv" onChange={(e) => setCsvFile(e.target.files[0] || null)} />
            <button className="btn primary" disabled={!csvFile}>Upload</button>
          </div>
          {importResult && importResult.imported > 0 && (
            <div className="alert success">Imported {importResult.imported} employee(s).</div>
          )}
          {importResult && importResult.errors.length > 0 && (
            <div className="alert error">
              Nothing was imported. Fix these rows and upload again:
              <ul>
                {importResult.errors.map((er, i) => (
                  <li key={i}>{er.row > 0 ? `Row ${er.row}: ` : ''}{er.message}</li>
                ))}
              </ul>
            </div>
          )}
        </form>
      )}
    </div>
  );
}
