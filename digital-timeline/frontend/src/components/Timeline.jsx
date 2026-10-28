import { useEffect, useState } from 'react'
import { api } from '../api.js'
import ActivityCard from './ActivityCard.jsx'
import AddActivity from './AddActivity.jsx'

const TYPES = ['TASK', 'NOTE', 'DOCUMENT', 'GITHUB', 'EVENT']

/** One list component used for Timeline, Notes, Tasks, Documents, GitHub and Events pages. */
export default function Timeline({ tick, live, title = 'My Timeline', fixedTypes = null, ahead = 1, days: initialDays, initialProject = '', addType = 'NOTE', blurb }) {
  const [days, setDays] = useState(initialDays ?? (fixedTypes?.[0] === 'EVENT' ? 60 : 30))
  const [types, setTypes] = useState([])
  const [project, setProject] = useState(initialProject)
  const [projects, setProjects] = useState([])
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  const [adding, setAdding] = useState(false)
  const [reload, setReload] = useState(0)
  const [q, setQ] = useState('')
  const [hits, setHits] = useState(null)
  const [hideDone, setHideDone] = useState(false)
  const activeTypes = fixedTypes ?? types

  useEffect(() => { setProject(initialProject) }, [initialProject])
  useEffect(() => {
    api.timeline({ days, project, ahead, types: activeTypes.join(',') }).then(setData).catch((e) => setError(e.message))
    api.projects().then(setProjects).catch(() => {})
  }, [days, types, project, tick, reload, ahead, fixedTypes?.join()])

  const refresh = () => setReload((r) => r + 1)
  const search = async (e) => { e.preventDefault(); setHits(q.trim() ? await api.search(q.trim()).catch(() => []) : null) }
  const toggle = (t) => setTypes(types.includes(t) ? types.filter((x) => x !== t) : [...types, t])
  const visible = (items) => (hideDone ? items.filter((a) => a.details?.status !== 'DONE') : items)

  return (
    <>
      <div className="head">
        <div><h1>{title}</h1>{blurb && <p className="muted">{blurb}</p>}{live && <span className="live">Live</span>}</div>
        <button className="btn" onClick={() => setAdding(true)}>+ Add</button>
      </div>
      <form className="row search-row" onSubmit={search}>
        <input aria-label="Search" placeholder="Search by meaning, not just keywords…" value={q} onChange={(e) => setQ(e.target.value)} />
        <button className="btn ghost">Search</button>
        {hits && <button type="button" className="btn ghost" onClick={() => { setHits(null); setQ('') }}>Clear</button>}
      </form>

      {hits ? (
        <div className="spine"><div className="day"><h3>{hits.length} results</h3>
          {hits.map((h) => (<div key={h.activity.id}><ActivityCard a={h.activity} onChanged={refresh} /><div className="mono hit-why">{h.why}</div></div>))}
        </div></div>
      ) : (
        <>
          <div className="filters">
            {!fixedTypes && TYPES.map((t) => <button key={t} className="chip" aria-pressed={types.includes(t)} onClick={() => toggle(t)}>{t.toLowerCase()}</button>)}
            {fixedTypes?.[0] === 'TASK' && <button className="chip" aria-pressed={hideDone} onClick={() => setHideDone(!hideDone)}>hide completed</button>}
            <select aria-label="Project" value={project} onChange={(e) => setProject(e.target.value)}>
              <option value="">All projects</option>{projects.map((p) => <option key={p}>{p}</option>)}
            </select>
            <select aria-label="Range" value={days} onChange={(e) => setDays(+e.target.value)}>
              <option value={7}>Last 7 days</option><option value={30}>Last 30 days</option><option value={90}>Last 90 days</option><option value={365}>Last year</option>
            </select>
          </div>
          {error && <div className="error">{error}</div>}
          {data && data.every((d) => visible(d.items).length === 0) && <div className="notice">Nothing here yet. Press “+ Add”, or connect GitHub, a folder or a calendar in Settings.</div>}
          <div className="spine">
            {data?.map((d) => visible(d.items).length > 0 && (
              <section className="day" key={d.date}>
                <h3>{new Date(d.date + 'T00:00').toLocaleDateString([], { weekday: 'long', day: 'numeric', month: 'long' })}</h3>
                {visible(d.items).map((a) => <ActivityCard key={a.id} a={a} onChanged={refresh} />)}
              </section>
            ))}
          </div>
        </>
      )}
      {adding && <AddActivity initialType={addType} onClose={() => setAdding(false)} onSaved={() => { setAdding(false); refresh() }} />}
    </>
  )
}
