import { useEffect, useState } from 'react'
import { Bar, BarChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { api } from '../api.js'

const DOW = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']

function Heatmap({ cells }) {
  const grid = Array.from({ length: 7 }, () => Array(24).fill(0))
  cells.forEach((c) => { const d = (new Date(c.day + 'T00:00').getDay() + 6) % 7; grid[d][c.hour] += c.count })
  const max = Math.max(1, ...grid.flat())
  return (
    <div className="heat" role="img" aria-label="Activity by weekday and hour">
      <span />{Array.from({ length: 24 }, (_, h) => <span key={h}>{h % 6 === 0 ? h : ''}</span>)}
      {grid.map((row, d) => [<span key={`l${d}`}>{DOW[d]}</span>, ...row.map((v, h) => (
        <div key={`${d}-${h}`} className="c" title={`${DOW[d]} ${h}:00 · ${v} activities`} style={{ background: v ? `rgba(179,56,43,${0.15 + 0.85 * v / max})` : undefined }} />
      ))])}
    </div>
  )
}

export default function Insights({ tick }) {
  const [days, setDays] = useState(30)
  const [data, setData] = useState(null)
  const [forgotten, setForgotten] = useState([])
  const [heat, setHeat] = useState([])
  const [msg, setMsg] = useState('')

  useEffect(() => {
    api.insights(days).then(setData).catch((e) => setMsg(e.message))
    api.forgotten().then(setForgotten).catch(() => {})
    api.heatmap(Math.max(days, 60)).then(setHeat).catch(() => {})
  }, [days, tick])

  const r = data?.report
  return (
    <>
      <div className="head">
        <h1>Insights</h1>
        <div className="row">
          <button className="btn ghost" onClick={async () => { const x = await api.digest(); setMsg(x.created ? 'Weekly digest added to your timeline.' : x.message) }}>Write weekly digest</button>
          <select aria-label="Range" value={days} onChange={(e) => setDays(+e.target.value)} style={{ width: 'auto' }}>
            <option value={7}>7 days</option><option value={30}>30 days</option><option value={90}>90 days</option>
          </select>
        </div>
      </div>
      {msg && <div className="notice" style={{ marginBottom: 14 }}>{msg}</div>}
      {r && (
        <>
          <div className="stats">
            <div className="stat"><b>{r.total}</b>activities{r.changeVsPrevious != null && <div className="mono">{r.changeVsPrevious >= 0 ? '+' : ''}{Math.round(r.changeVsPrevious)}% vs before</div>}</div>
            <div className="stat"><b>{r.activeDays}</b>active days</div>
            <div className="stat"><b>{r.longestStreak}</b>day best streak</div>
            <div className="stat"><b>{r.focusScore}</b>focus score<div className="mono">{r.contextSwitches} project switches</div></div>
            <div className="stat"><b>{String(r.peakHour).padStart(2, '0')}:00</b>{r.workStyle}</div>
          </div>
          <div className="panel"><h2>What the data says</h2><div className="answer">{data.narrative}</div></div>
          <div className="grid2">
            <div className="panel"><h2>Activity per day</h2>
              <ResponsiveContainer width="100%" height={200}><BarChart data={r.perDay}><XAxis dataKey="day" tickFormatter={(d) => d.slice(5)} fontSize={11} /><YAxis allowDecimals={false} width={28} fontSize={11} /><Tooltip /><Bar dataKey="count" fill="#b3382b" radius={[3, 3, 0, 0]} /></BarChart></ResponsiveContainer></div>
            <div className="panel"><h2>Where your time went</h2>
              {Object.entries(r.byProject).map(([p, n]) => (
                <div key={p} className="row" style={{ justifyContent: 'space-between', borderTop: '1px solid var(--rule)', padding: '6px 0' }}><span>{p}</span><span className="mono">{n}</span></div>))}</div>
          </div>
        </>
      )}
      <div className="panel"><h2>When you work</h2><Heatmap cells={heat} /></div>
      <div className="panel">
        <h2>Forgotten work</h2>
        <p className="muted" style={{ marginTop: 0 }}>Open tasks you stopped touching that still relate to what you did this week.</p>
        {forgotten.length === 0 && <span className="muted">Nothing is slipping through. Nice.</span>}
        {forgotten.map((f) => (
          <div key={f.task.id} style={{ borderTop: '1px solid var(--rule)', padding: '8px 0' }}>
            <b>{f.task.title}</b> <span className="proj">{f.task.project}</span>
            <div className="muted" style={{ fontSize: 13.5 }}>{f.reason}</div>
          </div>))}
      </div>
    </>
  )
}
