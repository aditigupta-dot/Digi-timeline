import { useState } from 'react';
import { api } from '../api.js';
import { Empty, Skeleton, fmt, useLoad, useToast } from './ui.jsx';
const PRI = { 1: 'High', 2: 'Medium', 3: 'Low' };
export default function Tasks() {
  const [status, setStatus] = useState(''); const { data, loading, error, reload } = useLoad('/tasks' + (status ? '?status=' + status : ''));
  const toast = useToast(); const [f, setF] = useState({ title: '', due: '', priority: 2 });
  const run = async (fn, msg) => { try { await fn(); if (msg) toast(msg); reload(); } catch (e) { toast(e.message, 'err'); } };
  const add = (e) => { e.preventDefault(); if (!f.title.trim()) return toast('Give the task a title', 'err');
    run(() => api('/tasks', { method: 'POST', body: { title: f.title, priority: +f.priority, dueAt: f.due ? new Date(f.due).toISOString() : null } }), 'Task added').then(() => setF({ title: '', due: '', priority: 2 })); };
  const patch = (id, body, msg) => run(() => api('/tasks/' + id, { method: 'PATCH', body }), msg);
  return (<><p className="eyebrow">WORK</p><h1 className="display sm">Tasks</h1>
    <form className="bar" onSubmit={add}><input aria-label="Title" placeholder="New task" value={f.title} onChange={(e) => setF({ ...f, title: e.target.value })} />
      <input aria-label="Due" type="datetime-local" value={f.due} onChange={(e) => setF({ ...f, due: e.target.value })} />
      <select aria-label="Priority" value={f.priority} onChange={(e) => setF({ ...f, priority: e.target.value })}>{Object.entries(PRI).map(([k, v]) => <option key={k} value={k}>{v}</option>)}</select><button className="btn small">Add</button></form>
    <div className="bar"><select aria-label="Filter status" value={status} onChange={(e) => setStatus(e.target.value)}><option value="">All</option><option value="PENDING">Pending</option><option value="IN_PROGRESS">In progress</option><option value="COMPLETED">Completed</option></select></div>
    {loading ? <Skeleton /> : error ? <p className="form-error" role="alert">{error}</p> : !data.length ? <Empty title="No tasks">Add your first task above.</Empty> :
      <ul className="rows">{data.map((t) => (<li key={t.id}><span>{t.suggestion && <span className="tag">Suggestion</span>} <span style={{ textDecoration: t.status === 'COMPLETED' ? 'line-through' : 'none' }}>{t.title}</span>
        <span className="mono"> · {PRI[t.priority]} · due {fmt(t.dueAt)}</span></span>
        <span className="acts">{t.suggestion && <button className="linkbtn" onClick={() => patch(t.id, { confirmSuggestion: true }, 'Task confirmed')}>Confirm</button>}
          <select aria-label="Status" value={t.status} onChange={(e) => patch(t.id, { status: e.target.value })}><option value="PENDING">Pending</option><option value="IN_PROGRESS">In progress</option><option value="COMPLETED">Completed</option></select>
          <button className="linkbtn" onClick={() => run(() => api('/tasks/' + t.id, { method: 'DELETE' }), 'Task deleted')}>Delete</button></span></li>))}</ul>}</>);
}
