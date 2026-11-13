import { useState } from 'react';
import { api } from '../api.js';
import { Empty, Skeleton, useLoad, useToast } from './ui.jsx';
export default function Projects() {
  const { data, loading, error, reload } = useLoad('/projects'); const toast = useToast(); const [name, setName] = useState('');
  const add = async (e) => { e.preventDefault(); if (!name.trim()) return;
    try { await api('/projects', { method: 'POST', body: { name } }); setName(''); toast('Project created'); reload(); } catch (err) { toast(err.message, 'err'); } };
  return (<><p className="eyebrow">WORKSPACES</p><h1 className="display sm">Projects</h1>
    <form className="bar" onSubmit={add}><input aria-label="Project name" placeholder="New project" value={name} onChange={(e) => setName(e.target.value)} /><button className="btn small">Create</button></form>
    {loading ? <Skeleton /> : error ? <p className="form-error" role="alert">{error}</p> : !data.length ? <Empty title="No projects yet">Create one to group tasks and activities.</Empty> :
      <ul className="rows">{data.map((p) => { const pct = p.totalTasks ? Math.round((100 * p.doneTasks) / p.totalTasks) : 0;
        return <li key={p.id}><span>{p.name}</span><span className="mono">{p.doneTasks}/{p.totalTasks} tasks · {pct}%</span></li>; })}</ul>}</>);
}
