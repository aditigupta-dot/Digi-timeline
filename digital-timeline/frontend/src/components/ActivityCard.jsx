import { useState } from 'react'
import { api } from '../api.js'

export const TYPE_COLOR = { TASK: '#b3382b', NOTE: '#3d8b6a', DOCUMENT: '#7a5ac8', GITHUB: '#3b1613', EVENT: '#c98a2b' }
const LABEL = { TASK: 'Task', NOTE: 'Note', DOCUMENT: 'Document', GITHUB: 'GitHub', EVENT: 'Event' }

export default function ActivityCard({ a, onChanged }) {
  const [open, setOpen] = useState(false)
  const [related, setRelated] = useState(null)
  const d = new Date(a.occurredAt)
  const time = d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  const isTask = a.type === 'TASK'
  const done = isTask && a.details?.status === 'DONE'
  const due = isTask && a.details?.dueAt ? new Date(a.details.dueAt) : null
  const overdue = due && !done && due < new Date()

  const toggle = async () => {
    const next = !open
    setOpen(next)
    if (next && related === null) setRelated(await api.related(a.id).catch(() => []))
  }
  const complete = async () => {
    await api.update(a.id, {
      type: 'TASK', title: a.title, content: a.content, project: a.project, tags: a.tags,
      attrs: { status: 'DONE', priority: String(a.details?.priority ?? 2), ...(a.details?.dueAt ? { dueAt: a.details.dueAt } : {}) },
    })
    onChanged()
  }

  return (
    <article className={`act ${done ? 'done' : ''}`} style={{ '--tc': TYPE_COLOR[a.type] }}>
      <div className="top">
        <button className="title" onClick={toggle} aria-expanded={open}>{a.title}</button>
        <span className="mono">{d.toLocaleDateString([], { day: 'numeric', month: 'short' })} · {time}</span>
      </div>
      <div className="meta">
        <span className="kind">{LABEL[a.type]}{isTask && a.details?.status ? ` · ${a.details.status.toLowerCase().replace('_', ' ')}` : ''}</span>
        {a.project && <span className="proj">{a.project}</span>}
        {due && <span className={`due ${overdue ? 'late' : ''}`}>{overdue ? 'Overdue · ' : 'Due '}{due.toLocaleDateString([], { day: 'numeric', month: 'short' })}</span>}
        {a.tags.slice(0, 3).map((t) => <span key={t} className="tag">{t}</span>)}
      </div>
      {open && (
        <>
          {a.content && <p>{a.content}</p>}
          {a.details?.location && <p className="muted">📍 {a.details.location}</p>}
          {a.details?.url && <p><a href={a.details.url} target="_blank" rel="noreferrer">{a.details.url}</a></p>}
          <div className="related">
            <b className="small-title">Connected to</b>
            {related === null && <span className="muted">Finding connections…</span>}
            {related?.length === 0 && <span className="muted">No strong connections yet.</span>}
            {related?.map((r) => (
              <div key={r.activity.id}>
                <span className="bar"><i style={{ width: `${Math.round(r.weight * 100)}%` }} /></span>
                <b>{r.activity.title}</b> <span className="muted">because {r.reason}</span>
              </div>
            ))}
          </div>
          <div className="row" style={{ marginTop: 10 }}>
            {isTask && !done && <button className="btn small" onClick={complete}>Mark done</button>}
            <button className="btn ghost small" onClick={async () => { if (confirm('Delete this activity?')) { await api.remove(a.id); onChanged() } }}>Delete</button>
          </div>
        </>
      )}
    </article>
  )
}
