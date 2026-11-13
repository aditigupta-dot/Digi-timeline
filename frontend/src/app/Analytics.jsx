import { Bar, BarChart, CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { download } from '../api.js';
import { Empty, Skeleton, useLoad, useToast } from './ui.jsx';
const axis = { stroke: '#73766F', fontSize: 11, tickLine: false };
const Chart = ({ title, children }) => (<section><h2>{title}</h2><div style={{ height: 220 }}><ResponsiveContainer>{children}</ResponsiveContainer></div></section>);
export default function Analytics() {
  const { data, loading, error } = useLoad('/analytics/weekly'); const toast = useToast();
  if (loading) return <Skeleton lines={5} />;
  if (error) return <p className="form-error" role="alert">{error}</p>;
  const total = data.daily.reduce((n, p) => n + p.count, 0);
  const rate = data.tasksTotal ? Math.round((100 * data.tasksDone) / data.tasksTotal) : null;
  return (<><p className="eyebrow">INSIGHTS</p><h1 className="display sm">Analytics</h1>
    <div className="bar"><button className="btn small ghost" onClick={() => download('/export/timeline.csv', 'digitaltimeline-export.csv').catch((e) => toast(e.message, 'err'))}>Export timeline CSV</button></div>
    <div className="stats"><div><b className="num">{rate === null ? '—' : rate + '%'}</b><span>{data.tasksDone} of {data.tasksTotal} confirmed tasks completed</span></div>
      <div><b className="num">{total}</b><span>activities in the last 14 days</span></div></div>
    {!total && !data.bySource.length && <Empty title="No activity to chart yet">Add timeline entries or sync a connected source, then return here.</Empty>}
    <Chart title="Daily activity (14 days)"><BarChart data={data.daily}><CartesianGrid stroke="#E9E4D8" vertical={false} /><XAxis dataKey="label" {...axis} interval={1} /><YAxis allowDecimals={false} {...axis} width={28} /><Tooltip /><Bar dataKey="count" name="Activities" fill="#C66B4E" /></BarChart></Chart>
    <Chart title="Weekly trend (8 weeks, by week start)"><LineChart data={data.weekly}><CartesianGrid stroke="#E9E4D8" vertical={false} /><XAxis dataKey="label" {...axis} /><YAxis allowDecimals={false} {...axis} width={28} /><Tooltip /><Line dataKey="count" name="Activities" stroke="#75836A" strokeWidth={2} dot={{ r: 3 }} /></LineChart></Chart>
    <Chart title="Activity by source (all time)"><BarChart data={data.bySource} layout="vertical"><XAxis type="number" allowDecimals={false} {...axis} /><YAxis type="category" dataKey="label" {...axis} width={80} /><Tooltip /><Bar dataKey="count" name="Activities" fill="#C5A35B" /></BarChart></Chart>
    <section><h2>Project progress</h2>{data.projects.length ? <ul className="rows">{data.projects.map((p) => <li key={p.id}><span>{p.name}</span><span className="mono">{p.doneTasks}/{p.totalTasks} done</span></li>)}</ul> : <p className="muted">No projects yet.</p>}</section>
    <p className="muted">Completion is the current share of confirmed tasks marked done; completion dates are not tracked yet.</p></>);
}
