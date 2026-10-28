// Thin fetch wrapper: adds the JWT, turns RFC-7807 errors into readable Error messages.
const KEY = 'dt.session'

export const session = {
  load() { try { return JSON.parse(localStorage.getItem(KEY)) } catch { return null } },
  save(s) { localStorage.setItem(KEY, JSON.stringify(s)) },
  clear() { localStorage.removeItem(KEY) },
}

export const tz = Intl.DateTimeFormat().resolvedOptions().timeZone

async function request(path, { method = 'GET', body, form, raw } = {}) {
  const s = session.load()
  const headers = {}
  if (s?.token) headers.Authorization = `Bearer ${s.token}`
  if (body) headers['Content-Type'] = 'application/json'
  const res = await fetch(`/api${path}`, { method, headers, body: form ?? (body ? JSON.stringify(body) : undefined) })
  if (res.status === 401 && s) { session.clear(); window.location.reload() }
  if (!res.ok) {
    let msg = `Request failed (${res.status})`
    try { const p = await res.json(); msg = p.detail || p.title || msg } catch { /* not json */ }
    throw new Error(msg)
  }
  if (raw) return res
  if (res.status === 204) return null
  return res.json()
}

const qs = (o) => new URLSearchParams(Object.entries(o).filter(([, v]) => v !== undefined && v !== '' && v !== null)).toString()

export const api = {
  login: (email, password) => request('/auth/login', { method: 'POST', body: { email, password } }),
  register: (email, password, displayName) => request('/auth/register', { method: 'POST', body: { email, password, displayName } }),
  me: () => request('/me'),
  setGithub: (githubUsername) => request('/me', { method: 'PUT', body: { githubUsername } }),
  system: () => request('/system'),
  timeline: (p) => request(`/timeline?${qs({ ...p, tz })}`),
  dashboard: () => request(`/dashboard?${qs({ tz })}`),
  get: (id) => request(`/activities/${id}`),
  update: (id, a) => request(`/activities/${id}`, { method: 'PUT', body: a }),
  projects: () => request('/projects'),
  create: (a) => request('/activities', { method: 'POST', body: a }),
  remove: (id) => request(`/activities/${id}`, { method: 'DELETE' }),
  related: (id) => request(`/activities/${id}/related`),
  ask: (question) => request('/ask', { method: 'POST', body: { question, tz } }),
  search: (q) => request(`/search?${qs({ q })}`),
  graph: (days) => request(`/graph?days=${days}`),
  clusters: (days) => request(`/clusters?days=${days}`),
  insights: (days) => request(`/insights?${qs({ days, tz })}`),
  forgotten: () => request('/insights/forgotten'),
  heatmap: (days) => request(`/insights/heatmap?days=${days}`),
  digest: () => request('/digest', { method: 'POST' }),
  reindex: () => request('/reindex', { method: 'POST' }),
  syncAll: () => request('/collect/sync-all', { method: 'POST' }),
  github: (username) => request('/collect/github', { method: 'POST', body: username ? { username } : {} }),
  folder: (path, days) => request('/collect/folder', { method: 'POST', body: { path, days: String(days) } }),
  calendar: (file) => { const f = new FormData(); f.append('file', file); f.append('tz', tz); return request('/collect/calendar', { method: 'POST', form: f }) },
  importSnapshot: (file) => { const f = new FormData(); f.append('file', file); return request('/import/snapshot', { method: 'POST', form: f }) },
  download: async (path, filename) => {
    const res = await request(path, { raw: true })
    const url = URL.createObjectURL(await res.blob())
    const a = Object.assign(document.createElement('a'), { href: url, download: filename })
    a.click(); URL.revokeObjectURL(url)
  },
  streamUrl: () => `/api/stream?access_token=${encodeURIComponent(session.load()?.token ?? '')}`,
}
