import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { fmt, useToast } from './ui.jsx';
const IDEAS = ['What are my most urgent tasks?', 'Summarize my activity today', 'What deadlines are approaching?', 'What did I work on this week?'];
export default function Assistant() {
  const toast = useToast(); const [msgs, setMsgs] = useState([]); const [text, setText] = useState(''); const [busy, setBusy] = useState(false);
  const [conv, setConv] = useState(null); const [ext, setExt] = useState(false); const [extOk, setExtOk] = useState(false); const end = useRef();
  useEffect(() => { api('/ai/status').then((s) => setExtOk(s.externalAvailable)).catch(() => {}); api('/settings').then((s) => setExt(s.aiExternalDefault)).catch(() => {}); }, []);
  useEffect(() => end.current?.scrollIntoView({ block: 'end' }), [msgs]);
  async function ask(q) {
    if (!q.trim() || busy) return; setMsgs((m) => [...m, { role: 'user', text: q }]); setText(''); setBusy(true);
    try { const r = await api('/ai/chat', { method: 'POST', body: { message: q, conversationId: conv, useExternalAi: ext && extOk } });
      setConv(r.conversationId); setMsgs((m) => [...m, { role: 'assistant', text: r.answer, refs: r.references, provider: r.provider }]); }
    catch (e) { toast(e.message, 'err'); } finally { setBusy(false); }
  }
  return (<><p className="eyebrow">GROUNDED IN YOUR DATA</p><h1 className="display sm">Assistant</h1>
    <p className="muted">Answers are built only from records found in your own tasks and activities, and those records are listed under each answer. It works without any AI key.</p>
    {!msgs.length && <div className="bar">{IDEAS.map((i) => <button key={i} className="btn small ghost" onClick={() => ask(i)}>{i}</button>)}</div>}
    <div className="chat" aria-live="polite">{msgs.map((m, i) => (<div key={i} className={'msg ' + m.role}>
      <p style={{ whiteSpace: 'pre-wrap' }}>{m.text}</p>
      {m.refs?.length > 0 && <details open><summary className="mono">{m.refs.length} source record(s) · {m.provider}</summary><ul className="rows">{m.refs.map((r) => (
        <li key={r.kind + r.id}><span>{r.url ? <a href={r.url} target="_blank" rel="noreferrer">{r.title}</a> : r.kind === 'task' ? <Link to="/app/tasks">{r.title}</Link> : r.title}</span><span className="mono">{r.kind} · {r.at ? fmt(r.at) : 'no date'}</span></li>))}</ul></details>}
    </div>))}{busy && <p className="muted">Looking through your records…</p>}<div ref={end} /></div>
    {extOk && <label className="muted"><input type="checkbox" checked={ext} onChange={(e) => setExt(e.target.checked)} /> Use Gemini to phrase answers (sends record titles, dates and short notes to Google; never email content)</label>}
    <form className="bar" onSubmit={(e) => { e.preventDefault(); ask(text); }}><input aria-label="Ask a question" maxLength={500} placeholder="Ask about your tasks and activity" value={text} onChange={(e) => setText(e.target.value)} /><button className="btn small" disabled={busy}>Ask</button></form></>);
}
