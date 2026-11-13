// One place for all HTTP calls. Adds the access token and retries once after refreshing it.
const REFRESH_KEY = 'dt_refresh';
let accessToken = null;
export const setTokens = (t) => {
  accessToken = t?.accessToken ?? null;
  if (t?.refreshToken) localStorage.setItem(REFRESH_KEY, t.refreshToken); else localStorage.removeItem(REFRESH_KEY);
};
export const hasRefreshToken = () => !!localStorage.getItem(REFRESH_KEY);

async function raw(path, method, body, auth) {
  const res = await fetch('/api' + path, { method,
    headers: { 'Content-Type': 'application/json', ...(auth && accessToken ? { Authorization: `Bearer ${accessToken}` } : {}) },
    body: body ? JSON.stringify(body) : undefined });
  if (res.status === 204) return null;
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw Object.assign(new Error(data.message || 'Request failed'), { status: res.status });
  return data;
}
export async function refreshSession() {
  const refreshToken = localStorage.getItem(REFRESH_KEY);
  if (!refreshToken) throw new Error('No session');
  const t = await raw('/auth/refresh', 'POST', { refreshToken }, false);
  setTokens(t); return t;
}
export async function api(path, { method = 'GET', body } = {}) {
  try { return await raw(path, method, body, true); }
  catch (e) {
    if (e.status !== 401 || !hasRefreshToken()) throw e;
    await refreshSession();
    return raw(path, method, body, true);
  }
}
export const authCall = (path, body) => raw(path, 'POST', body, false);
/** Downloads an authenticated file (the browser can't add our Bearer header to a plain <a href>). */
export async function download(path, filename) {
  const go = () => fetch('/api' + path, { headers: { Authorization: `Bearer ${accessToken}` } });
  let res = await go();
  if (res.status === 401 && hasRefreshToken()) { await refreshSession(); res = await go(); }
  if (!res.ok) throw new Error('Export failed');
  const url = URL.createObjectURL(await res.blob());
  const a = Object.assign(document.createElement('a'), { href: url, download: filename }); a.click(); URL.revokeObjectURL(url);
}
/** Reads the SSE stream with fetch so the token stays in a header (not in the URL). Resolves/throws when the stream ends. */
export async function streamEvents(signal, onEvent) {
  const res = await fetch('/api/events', { headers: { Authorization: `Bearer ${accessToken}`, Accept: 'text/event-stream' }, signal });
  if (res.status === 401) { await refreshSession(); throw new Error('retry'); }
  if (!res.ok || !res.body) throw new Error('stream failed');
  const reader = res.body.getReader(); const dec = new TextDecoder(); let buf = '';
  for (;;) {
    const { done, value } = await reader.read(); if (done) return;
    buf += dec.decode(value, { stream: true }); let i;
    while ((i = buf.indexOf('\n\n')) >= 0) {
      const block = buf.slice(0, i); buf = buf.slice(i + 2); let ev = 'message', data = '';
      for (const line of block.split('\n')) { if (line.startsWith('event:')) ev = line.slice(6).trim(); else if (line.startsWith('data:')) data += line.slice(5).trim(); }
      if (data) { try { onEvent(ev, JSON.parse(data)); } catch { /* ignore malformed event */ } }
    }
  }
}
