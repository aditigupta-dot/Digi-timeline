import { useEffect, useMemo, useState } from 'react'
import { api } from '../api.js'
import Icon from './Icon.jsx'
import ActivityCard from './ActivityCard.jsx'
import NotificationBell from './NotificationBell.jsx'
import { BookStack, Brain, Paper, Pen } from './Illustrations.jsx'

const DEFAULT_TEACHERS = [
  { name: 'Dr. R. Sharma', subject: 'Mathematics' },
  { name: 'Prof. S. Mehta', subject: 'Data Structures' },
  { name: 'Dr. P. Verma', subject: 'Computer Networks' },
]
const MONTHS = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December']
const key = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`

function Ring({ value, label, icon, tone }) {
  const [ready, setReady] = useState(false)
  useEffect(() => { const t = setTimeout(() => setReady(true), 120); return () => clearTimeout(t) }, [])
  const r = 46, c = 2 * Math.PI * r
  return (
    <div className={`ring-card ${tone}`}>
      <div className="ring-title"><Icon name={icon} size={20} />{label}</div>
      <svg viewBox="0 0 120 120" className="ring" role="img" aria-label={`${label} ${value} percent`}>
        <defs><linearGradient id={`g-${label}`} x1="0" y1="0" x2="1" y2="1"><stop offset="0" stopColor="#f08a73" /><stop offset="1" stopColor="#ffd9cc" /></linearGradient></defs>
        <circle cx="60" cy="60" r={r} fill="none" stroke="rgba(255,255,255,.13)" strokeWidth="11" />
        <circle cx="60" cy="60" r={r} fill="none" stroke={`url(#g-${label})`} strokeWidth="11" strokeLinecap="round"
                strokeDasharray={c} strokeDashoffset={ready ? c * (1 - value / 100) : c} transform="rotate(-90 60 60)" className="ring-arc" />
        <text x="60" y="67" textAnchor="middle" className="ring-num">{value}%</text>
      </svg>
    </div>
  )
}

function Calendar({ schedule, selected, onSelect }) {
  const today = new Date()
  const [cursor, setCursor] = useState(new Date(today.getFullYear(), today.getMonth(), 1))
  const byDay = useMemo(() => {
    const m = {}
    schedule.forEach((s) => { (m[s.date] ??= []).push(s) })
    return m
  }, [schedule])
  const first = new Date(cursor.getFullYear(), cursor.getMonth(), 1)
  const offset = (first.getDay() + 6) % 7
  const cells = Array.from({ length: 42 }, (_, i) => new Date(cursor.getFullYear(), cursor.getMonth(), 1 - offset + i))
  const shift = (n) => setCursor(new Date(cursor.getFullYear(), cursor.getMonth() + n, 1))
  return (
    <div className="cal">
      <div className="cal-head">
        <button className="icon-btn" onClick={() => shift(-1)} aria-label="Previous month">‹</button>
        <b>{MONTHS[cursor.getMonth()]} {cursor.getFullYear()}</b>
        <button className="icon-btn" onClick={() => shift(1)} aria-label="Next month">›</button>
      </div>
      <div className="cal-grid">
        {['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'].map((d) => <span key={d} className="dow">{d}</span>)}
        {cells.map((d) => {
          const k = key(d), items = byDay[k]
          const other = d.getMonth() !== cursor.getMonth()
          return (
            <button key={k} className={`cell ${other ? 'other' : ''} ${k === key(today) ? 'today' : ''} ${k === selected ? 'sel' : ''}`}
                    onClick={() => onSelect(k)} aria-label={`${k}${items ? `, ${items.length} items` : ''}`}>
              {d.getDate()}{items && <i className={items.some((x) => x.kind === 'TASK_DUE') ? 'dot warn' : 'dot'} />}
            </button>
          )
        })}
      </div>
    </div>
  )
}

const HUES = [['#b3382b', '#e0705a'], ['#4b1914', '#9b3328'], ['#8d4a2f', '#d99a6c'], ['#6f2a3c', '#c4627a'], ['#5a3a2a', '#b8805e']]

export default function Dashboard({ dash, user, onNavigate, onOpenProject, onNoticeOpen, onRefresh, onLogout }) {
  const [q, setQ] = useState('')
  const [hits, setHits] = useState(null)
  const [menu, setMenu] = useState(false)
  const [selected, setSelected] = useState(key(new Date()))
  const [teachers, setTeachers] = useState(() => { try { return JSON.parse(localStorage.getItem('dt.teachers')) ?? DEFAULT_TEACHERS } catch { return DEFAULT_TEACHERS } })
  useEffect(() => { localStorage.setItem('dt.teachers', JSON.stringify(teachers)) }, [teachers])

  const first = (dash?.displayName ?? user.displayName ?? '').split(' ')[0]
  const day = useMemo(() => (dash?.schedule ?? []).filter((s) => s.date === selected), [dash, selected])
  const next = (dash?.schedule ?? []).filter((s) => s.date >= key(new Date())).slice(0, 3)
  const list = day.length ? day : next
  const counts = useMemo(() => {
    const c = { OVERDUE: 0, DUE_SOON: 0, BACKLOG: 0, EVENT_SOON: 0 }
    ;(dash?.notices ?? []).forEach((n) => { c[n.kind]++ })
    return c
  }, [dash])

  const search = async (e) => { e.preventDefault(); setHits(q.trim() ? await api.search(q.trim()).catch(() => []) : null) }
  const addTeacher = () => {
    const v = prompt('Teacher name, subject (example: Dr. Rao, Physics)')
    if (!v) return
    const [name, subject] = v.split(',').map((s) => s.trim())
    if (name) setTeachers([...teachers, { name, subject: subject || 'Teacher' }])
  }

  return (
    <div className="dash">
      <div className="dash-decor" aria-hidden="true">
        <BookStack className="dd-books float-a" /><Pen className="dd-pen sway" /><Brain className="dd-brain" /><Paper className="dd-paper flutter" />
      </div>

      <header className="dash-head">
        <div>
          <h1>Hello, <span className="accent">{first}</span>!</h1>
          <p className="muted">Your thoughts, tasks and memories — all in one place.</p>
        </div>
        <form className="searchbar" onSubmit={search}>
          <Icon name="search" size={18} />
          <input aria-label="Search everything" placeholder="Search everything… (notes, tasks, projects)" value={q} onChange={(e) => { setQ(e.target.value); if (!e.target.value) setHits(null) }} />
        </form>
        <div className="head-tools">
          <NotificationBell notices={dash?.notices ?? []} onOpen={onNoticeOpen} />
          <div className="menu-wrap">
            <button className="icon-btn" aria-label="Menu" onClick={() => setMenu(!menu)}><Icon name="dots" size={22} /></button>
            {menu && <div className="menu-pop"><button onClick={() => onNavigate('settings')}>Settings and sources</button><button onClick={onLogout}>Sign out</button></div>}
          </div>
        </div>
      </header>

      {hits && (
        <section className="panel search-results">
          <div className="row between"><h2>{hits.length} results for “{q}”</h2><button className="btn ghost small" onClick={() => { setHits(null); setQ('') }}>Close</button></div>
          {hits.slice(0, 8).map((h) => <ActivityCard key={h.activity.id} a={h.activity} onChanged={onRefresh} />)}
        </section>
      )}

      <div className="dash-grid">
        <div className="dash-main">
          {(counts.OVERDUE + counts.BACKLOG + counts.DUE_SOON > 0 || counts.EVENT_SOON > 0) && (
            <section className="panel attention">
              <div className="panel-title"><h2>Needs your attention</h2></div>
              <div className="att-chips">
                {counts.OVERDUE > 0 && <button className="att high" onClick={() => onNavigate('tasks')}><b>{counts.OVERDUE}</b> overdue</button>}
                {counts.DUE_SOON > 0 && <button className="att med" onClick={() => onNavigate('tasks')}><b>{counts.DUE_SOON}</b> due within 2 days</button>}
                {counts.BACKLOG > 0 && <button className="att med" onClick={() => onNavigate('tasks')}><b>{counts.BACKLOG}</b> in backlog</button>}
                {counts.EVENT_SOON > 0 && <button className="att info" onClick={() => onNavigate('events')}><b>{counts.EVENT_SOON}</b> events this week</button>}
              </div>
              <div className="att-list">
                {(dash?.notices ?? []).slice(0, 3).map((n) => (
                  <button key={n.id} className={`att-row ${n.severity}`} onClick={() => onNoticeOpen(n)}><b>{n.title}</b><span>{n.message}</span></button>
                ))}
              </div>
            </section>
          )}

          <div className="two-col">
            <section className="panel">
              <div className="panel-title"><h2>Linked Teachers</h2><button className="more" onClick={addTeacher}>Add +</button></div>
              {teachers.map((t, i) => (
                <div className="row-card" key={t.name + i}>
                  <span className="avatar">{t.name.replace(/^(Dr\.|Prof\.)\s*/, '')[0]}</span>
                  <div><b>{t.name}</b><span className="muted">{t.subject}</span></div>
                  <button className="icon-btn soft" aria-label={`Remove ${t.name}`} onClick={() => setTeachers(teachers.filter((_, j) => j !== i))}><Icon name="x" size={16} /></button>
                </div>
              ))}
            </section>
            <section className="panel">
              <div className="panel-title"><h2>Upcoming events</h2><button className="more" onClick={() => onNavigate('events')}>See more →</button></div>
              {(dash?.upcomingEvents ?? []).length === 0 && <p className="muted">No upcoming events. Add one from the Events page.</p>}
              {(dash?.upcomingEvents ?? []).slice(0, 3).map((e) => (
                <div className="row-card" key={e.id}>
                  <span className="avatar sq"><Icon name="calendar" size={18} /></span>
                  <div><b>{e.title}</b><span className="muted">{new Date(e.date + 'T00:00').toLocaleDateString([], { day: 'numeric', month: 'short', year: 'numeric' })} · {e.time}</span></div>
                </div>
              ))}
            </section>
          </div>

          <section className="panel">
            <div className="panel-title"><h2><Icon name="calendar" size={18} /> My schedule</h2></div>
            <div className="sched">
              <Calendar schedule={dash?.schedule ?? []} selected={selected} onSelect={setSelected} />
              <div className="sched-list">
                {!day.length && <p className="muted small">Nothing on this day. Coming up next:</p>}
                {list.length === 0 && <p className="muted">Your calendar is empty.</p>}
                {list.map((s) => (
                  <div className="day-row" key={s.id + s.kind}>
                    <span className="daynum">{Number(s.date.slice(8))}</span>
                    <div><b>{s.title}</b><span className="muted">{s.time} · {s.kind === 'TASK_DUE' ? 'task deadline' : 'event'}</span></div>
                  </div>
                ))}
              </div>
            </div>
          </section>

          <section className="panel">
            <div className="panel-title"><h2><Icon name="folder" size={18} /> My projects</h2><button className="more" onClick={() => onNavigate('timeline')}>See all →</button></div>
            <div className="proj-grid">
              {(dash?.projects ?? []).slice(0, 4).map((p, i) => (
                <button key={p.name} className="proj-card" onClick={() => onOpenProject(p.name)}>
                  <div className="proj-banner" style={{ background: `linear-gradient(135deg, ${HUES[i % HUES.length][0]}, ${HUES[i % HUES.length][1]})` }}>
                    <span>{p.name[0]}</span>
                  </div>
                  <b>{p.name}</b>
                  <span className="muted">{p.total} activities{p.open ? ` · ${p.open} open` : ''}</span>
                </button>
              ))}
            </div>
          </section>
        </div>

        <aside className="rail">
          <Ring value={dash?.rings.productivity ?? 0} label="Productivity" icon="target" tone="t1" />
          <Ring value={dash?.rings.focus ?? 0} label="Focus" icon="book" tone="t2" />
          <Ring value={dash?.rings.consistency ?? 0} label="Consistency" icon="pulse" tone="t3" />
          <p className="rail-note">Productivity = tasks finished. Focus = how little you jump between projects. Consistency = active days this month.</p>
          <button className="more center" onClick={() => onNavigate('insights')}>See more →</button>
        </aside>
      </div>
    </div>
  )
}
