import { useState } from 'react'
import { api } from '../api.js'

const pad = (n) => String(n).padStart(2, '0')
const localNow = () => { const d = new Date(); return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}` }

export default function AddActivity({ onClose, onSaved, initialType = 'NOTE' }) {
  const [f, setF] = useState({ type: initialType, title: '', content: '', project: '', tags: '', status: 'TODO', priority: '2', due: '', when: localNow(), location: '' })
  const [error, setError] = useState('')
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value })

  const save = async (e) => {
    e.preventDefault()
    try {
      const attrs = {}
      if (f.type === 'TASK') { attrs.status = f.status; attrs.priority = f.priority; if (f.due) attrs.dueAt = new Date(`${f.due}T17:00`).toISOString() }
      if (f.type === 'EVENT' && f.location) attrs.location = f.location
      await api.create({
        type: f.type, title: f.title, content: f.content, project: f.project || null,
        occurredAt: f.when ? new Date(f.when).toISOString() : null,
        tags: f.tags.split(',').map((t) => t.trim()).filter(Boolean), attrs,
      })
      onSaved()
    } catch (err) { setError(err.message) }
  }

  return (
    <div className="modal" onClick={onClose}>
      <form className="modal-card" onClick={(e) => e.stopPropagation()} onSubmit={save}>
        <h2>Add to your timeline</h2>
        <div className="row two">
          <div className="field"><label htmlFor="t">Type</label>
            <select id="t" value={f.type} onChange={set('type')}>
              <option value="NOTE">Note</option><option value="TASK">Task</option><option value="EVENT">Event</option>
              <option value="DOCUMENT">Document</option><option value="GITHUB">GitHub</option>
            </select></div>
          <div className="field"><label htmlFor="p">Project</label><input id="p" value={f.project} onChange={set('project')} placeholder="e.g. Hackathon" /></div>
        </div>
        <div className="field"><label htmlFor="ti">Title</label><input id="ti" value={f.title} onChange={set('title')} required maxLength={300} /></div>
        <div className="field"><label htmlFor="c">Details</label><textarea id="c" value={f.content} onChange={set('content')} placeholder="Tip: write #hashtags to tag automatically" /></div>
        <div className="row two">
          <div className="field"><label htmlFor="w">{f.type === 'EVENT' ? 'Starts at' : 'Happened at'}</label><input id="w" type="datetime-local" value={f.when} onChange={set('when')} /></div>
          {f.type === 'EVENT' && <div className="field"><label htmlFor="lo">Location</label><input id="lo" value={f.location} onChange={set('location')} /></div>}
          {f.type === 'TASK' && <div className="field"><label htmlFor="du">Due date</label><input id="du" type="date" value={f.due} onChange={set('due')} /></div>}
        </div>
        {f.type === 'TASK' && (
          <div className="row two">
            <div className="field"><label htmlFor="s">Status</label><select id="s" value={f.status} onChange={set('status')}><option>TODO</option><option>IN_PROGRESS</option><option>DONE</option></select></div>
            <div className="field"><label htmlFor="pr">Priority</label><select id="pr" value={f.priority} onChange={set('priority')}><option value="1">High</option><option value="2">Normal</option><option value="3">Low</option></select></div>
          </div>
        )}
        <div className="field"><label htmlFor="tg">Tags (comma separated, optional)</label><input id="tg" value={f.tags} onChange={set('tags')} /></div>
        {error && <div className="error" role="alert">{error}</div>}
        <div className="row"><button className="btn">Save</button><button type="button" className="btn ghost" onClick={onClose}>Cancel</button></div>
      </form>
    </div>
  )
}
