import { useCallback, useContext, useEffect, useState } from 'react';
import { api } from '../api.js';
import { Empty, LiveCtx, Skeleton, fmt, useToast } from './ui.jsx';
const SOURCES = ['', 'MANUAL', 'GITHUB', 'GMAIL', 'CALENDAR', 'CLASSROOM'];
export default function Timeline() {
  const toast = useToast(); const tick = useContext(LiveCtx);
  const [items, setItems] = useState([]); const [loading, setLoading] = useState(true); const [more, setMore] = useState(false);
  const [source, setSource] = useState(''); const [q, setQ] = useState(''); const [dq, setDq] = useState(''); const [title, setTitle] = useState('');
  useEffect(() => { const t = setTimeout(() => setDq(q), 300); return () => clearTimeout(t); }, [q]); // debounce search
  const query = (before) => `/timeline?limit=30${source ? '&source=' + source : ''}${dq ? '&q=' + encodeURIComponent(dq) : ''}${before ? '&before=' + encodeURIComponent(before) : ''}`;
  const load = useCallback(() => { setLoading(true); api(query()).then((r) => { setItems(r); setMore(r.length === 30); })
    .catch((e) => toast(e.message, 'err')).finally(() => setLoading(false)); }, [source, dq, tick]); // eslint-disable-line
  useEffect(load, [load]);
  const loadMore = () => api(query(items[items.length - 1].occurredAt)).then((r) => { setItems([...items, ...r]); setMore(r.length === 30); }).catch((e) => toast(e.message, 'err'));
  const add = async (e) => { e.preventDefault(); if (!title.trim()) return;
    try { await api('/timeline/manual', { method: 'POST', body: { title } }); setTitle(''); toast('Entry added'); load(); } catch (err) { toast(err.message, 'err'); } };
  const groups = items.reduce((m, a) => { const d = new Date(a.occurredAt).toLocaleDateString([], { dateStyle: 'full' }); (m[d] ||= []).push(a); return m; }, {});
  return (<><p className="eyebrow">CHRONOLOGICAL</p><h1 className="display sm">Timeline</h1>
    <div className="bar"><input aria-label="Search" placeholder="Search titles and descriptions" value={q} onChange={(e) => setQ(e.target.value)} />
      <select aria-label="Source" value={source} onChange={(e) => setSource(e.target.value)}>{SOURCES.map((s) => <option key={s} value={s}>{s || 'All sources'}</option>)}</select></div>
    <form className="bar" onSubmit={add}><input aria-label="New manual entry" placeholder="Add a manual entry" value={title} onChange={(e) => setTitle(e.target.value)} /><button className="btn small">Add</button></form>
    {loading ? <Skeleton lines={4} /> : !items.length ? <Empty title="No activities match">Nothing imported or entered yet for these filters.</Empty> :
      Object.entries(groups).map(([day, list]) => (<section key={day}><h2>{day}</h2><ul className="rows">{list.map((a) => (
        <li key={a.id}><span>{a.sourceUrl ? <a href={a.sourceUrl} target="_blank" rel="noreferrer">{a.title}</a> : a.title}</span><span className="mono">{a.source} · {fmt(a.occurredAt)}</span></li>))}</ul></section>))}
    {more && <button className="btn ghost" onClick={loadMore}>Load more</button>}</>);
}
