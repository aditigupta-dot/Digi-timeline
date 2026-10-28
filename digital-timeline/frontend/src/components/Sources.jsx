import { useEffect, useState } from 'react'
import { api } from '../api.js'

export default function Sources({ onChange }) {
  const [gh, setGh] = useState('')
  const [folder, setFolder] = useState('./sample-data')
  const [log, setLog] = useState([])
  const [busy, setBusy] = useState(false)

  useEffect(() => { api.me().then((m) => setGh(m.githubUsername ?? '')).catch(() => {}) }, [])

  const run = async (label, fn) => {
    setBusy(true)
    try {
      const r = await fn()
      const text = Array.isArray(r) ? r.map((x) => `${x.source}: ${x.error ?? `saved ${x.saved}, skipped ${x.skipped}`}`).join(' | ')
        : r?.source ? `${r.source}: saved ${r.saved}, skipped ${r.skipped} (${r.millis} ms)` : JSON.stringify(r)
      setLog((l) => [`${label} → ${text}`, ...l]); onChange()
    } catch (e) { setLog((l) => [`${label} → ${e.message}`, ...l]) } finally { setBusy(false) }
  }

  return (
    <>
      <div className="head"><h1>Sources &amp; backup</h1></div>
      <div className="grid2">
        <div className="panel"><h2>GitHub</h2>
          <div className="field"><label htmlFor="gh">Username</label><input id="gh" value={gh} onChange={(e) => setGh(e.target.value)} /></div>
          <div className="row">
            <button className="btn" disabled={busy || !gh} onClick={() => run('GitHub', async () => { await api.setGithub(gh); return api.github() })}>Import public activity</button>
            <button className="btn ghost" disabled={busy} onClick={() => run('Sync all', api.syncAll)}>Sync all sources</button>
          </div></div>
        <div className="panel"><h2>Folder of documents</h2>
          <div className="field"><label htmlFor="fo">Folder path (must be inside an allowed location)</label><input id="fo" value={folder} onChange={(e) => setFolder(e.target.value)} /></div>
          <button className="btn" disabled={busy} onClick={() => run('Folder', () => api.folder(folder, 60))}>Scan files changed in last 60 days</button></div>
        <div className="panel"><h2>Calendar (.ics)</h2>
          <p className="muted" style={{ marginTop: 0 }}>Export from Google Calendar, Outlook or Apple Calendar.</p>
          <input type="file" accept=".ics,text/calendar" onChange={(e) => e.target.files[0] && run('Calendar', () => api.calendar(e.target.files[0]))} /></div>
        <div className="panel"><h2>Backup &amp; restore</h2>
          <div className="row">
            <button className="btn ghost" onClick={() => api.download('/export/snapshot', 'timeline.dtsnap')}>Download snapshot</button>
            <button className="btn ghost" onClick={() => api.download('/export/json', 'timeline.json')}>Download JSON</button>
          </div>
          <div className="field" style={{ marginTop: 12 }}><label htmlFor="rs">Restore from snapshot</label>
            <input id="rs" type="file" accept=".dtsnap" onChange={(e) => e.target.files[0] && run('Restore', () => api.importSnapshot(e.target.files[0]))} /></div>
          <button className="btn ghost" disabled={busy} onClick={() => run('Re-index', api.reindex)}>Re-index embeddings</button></div>
      </div>
      {log.length > 0 && <div className="panel"><h2>Activity log</h2>{log.map((l, i) => <div key={i} className="mono" style={{ padding: '3px 0' }}>{l}</div>)}</div>}
    </>
  )
}
