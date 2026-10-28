import { useEffect, useRef, useState } from 'react'
import Icon from './Icon.jsx'

const GROUPS = [
  ['OVERDUE', 'Overdue', 'high'],
  ['DUE_SOON', 'Due soon', 'medium'],
  ['BACKLOG', 'Backlog', 'medium'],
  ['EVENT_SOON', 'Upcoming events', 'info'],
]

export default function NotificationBell({ notices = [], onOpen }) {
  const [open, setOpen] = useState(false)
  const [perm, setPerm] = useState(typeof Notification === 'undefined' ? 'unsupported' : Notification.permission)
  const box = useRef(null)

  useEffect(() => {
    const close = (e) => { if (box.current && !box.current.contains(e.target)) setOpen(false) }
    document.addEventListener('mousedown', close)
    return () => document.removeEventListener('mousedown', close)
  }, [])

  const urgent = notices.filter((n) => n.severity !== 'info').length

  return (
    <div className="bell-wrap" ref={box}>
      <button className="icon-btn" aria-label={`Notifications, ${notices.length} items`} aria-expanded={open} onClick={() => setOpen(!open)}>
        <Icon name="bell" size={22} />
        {notices.length > 0 && <span className={`badge ${urgent ? 'hot' : ''}`}>{notices.length > 9 ? '9+' : notices.length}</span>}
      </button>
      {open && (
        <div className="bell-pop" role="dialog" aria-label="Notifications">
          <div className="bell-head"><b>Reminders</b><span className="muted">{notices.length} need a look</span></div>
          {notices.length === 0 && <p className="muted pad">All clear. Nothing is pending.</p>}
          {GROUPS.map(([kind, label, sev]) => {
            const items = notices.filter((n) => n.kind === kind)
            if (!items.length) return null
            return (
              <div key={kind} className="bell-group">
                <div className={`bell-label ${sev}`}>{label} <span>{items.length}</span></div>
                {items.slice(0, 4).map((n) => (
                  <button key={n.id} className="bell-item" onClick={() => { setOpen(false); onOpen(n) }}>
                    <b>{n.title}</b><span>{n.message}{n.project ? ` · ${n.project}` : ''}</span>
                  </button>
                ))}
                {items.length > 4 && <div className="muted more">+ {items.length - 4} more</div>}
              </div>
            )
          })}
          {perm === 'default' && (
            <button className="btn small wide" onClick={async () => setPerm(await Notification.requestPermission())}>Turn on desktop alerts</button>
          )}
          {perm === 'granted' && <p className="muted pad small">Desktop alerts are on.</p>}
        </div>
      )}
    </div>
  )
}
