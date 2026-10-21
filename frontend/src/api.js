// One place for all calls to the backend.
// Every request automatically gets the login token, and every error becomes
// a normal JavaScript Error with the backend's "message", so pages can just show err.message.

const TOKEN_KEY = 'hrms_token';

// Where the backend lives.
// Locally and in Docker this is empty, so requests go to /api on the same address.
// On Vercel we set VITE_API_URL to the Render address, e.g. https://mini-hrms-api.onrender.com
const API_BASE = (import.meta.env.VITE_API_URL || '').replace(/\/$/, '');

export function getToken() {
  try { return localStorage.getItem(TOKEN_KEY); } catch { return null; }
}

export function setToken(token) {
  try {
    if (token) localStorage.setItem(TOKEN_KEY, token);
    else localStorage.removeItem(TOKEN_KEY);
  } catch { /* storage blocked: user just stays logged in until refresh */ }
}

export async function api(path, { method = 'GET', body, isForm = false } = {}) {
  const headers = {};
  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body && !isForm) headers['Content-Type'] = 'application/json';

  let res;
  try {
    res = await fetch(`${API_BASE}/api${path}`, {
      method,
      headers,
      body: isForm ? body : body ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new Error('Cannot reach the server. If this is the free online version, it may be waking up: wait 30-60 seconds and try again.');
  }

  // Token missing, expired or invalid: log the user out everywhere
  if (res.status === 401 && path !== '/auth/login') {
    setToken(null);
    window.dispatchEvent(new Event('hrms:logout'));
    throw new Error('Your session has expired. Please log in again.');
  }

  if (res.status === 204) return null;
  const text = await res.text();
  let data = null;
  if (text) {
    try { data = JSON.parse(text); } catch { data = null; }
  }
  if (!res.ok) {
    throw new Error(data?.message || `Request failed (${res.status})`);
  }
  return data;
}

export function formatDate(value) {
  if (!value) return '';
  return new Date(value).toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' });
}

export function formatDateTime(value) {
  if (!value) return '';
  return new Date(value).toLocaleString(undefined, {
    day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit',
  });
}
