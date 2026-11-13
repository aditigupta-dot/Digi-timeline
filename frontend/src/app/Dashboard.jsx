import { Link } from 'react-router-dom';
import { Empty, Skeleton, fmt, useLoad } from './ui.jsx';
const PROVIDERS = ['GITHUB', 'GMAIL', 'CALENDAR', 'CLASSROOM'];
export default function Dashboard() {
  const { data, error, loading } = useLoad('/dashboard/today');
  const pri = useLoad('/tasks/priorities?limit=5');
  if (loading) return <Skeleton lines={5} />;
  if (error) return <p className="form-error" role="alert">Could not load the dashboard: {error}</p>;
  const status = (p) => data.integrations.find((i) => i.provider === p);
  const nothing = !data.recent.length && !data.openTasks;
  return (<><p className="eyebrow">TODAY</p><h1 className="display sm">Overview</h1>
    <div className="stats"><div><b className="num">{data.openTasks}</b><span>open tasks</span></div>
      <div><b className="num">{data.activitiesThisWeek}</b><span>activities in the last 7 days</span></div></div>
    {nothing && <Empty title="Nothing here yet">Add a task or a manual timeline entry to begin. Provider connections arrive in a later batch and will appear here once authorised. <Link to="/app/tasks">Add a task</Link></Empty>}
    <section><h2>Top priorities</h2>{pri.data?.length ? <ul className="rows">{pri.data.map((x) => <li key={x.task.id}><span>{x.task.title}<br /><span className="mono">{x.reasons.join(' · ')}</span></span><span className="mono">score {x.score}</span></li>)}</ul> : <p className="muted">No open confirmed tasks to rank.</p>}</section>
    <section><h2>Upcoming deadlines</h2>{data.upcoming.length ? <ul className="rows">{data.upcoming.map((t) => <li key={t.id}><span>{t.title}</span><span className="mono">{fmt(t.dueAt)}</span></li>)}</ul> : <p className="muted">No open tasks due in the next 7 days.</p>}</section>
    <section><h2>Recent activity</h2>{data.recent.length ? <ul className="rows">{data.recent.map((a) => <li key={a.id}><span>{a.title}</span><span className="mono">{a.source} · {fmt(a.occurredAt)}</span></li>)}</ul> : <p className="muted">No activity yet.</p>}</section>
    <section><h2>Integrations</h2><ul className="rows">{PROVIDERS.map((p) => { const s = status(p); return <li key={p}><span>{p}</span><span className="mono">{s ? s.status : 'DISCONNECTED'}{s?.lastSyncAt ? ' · synced ' + fmt(s.lastSyncAt) : ''}</span></li>; })}</ul></section></>);
}
