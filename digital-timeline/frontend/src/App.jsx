import { useCallback, useEffect, useState } from 'react'
import { api, session } from './api.js'
import Landing from './components/Landing.jsx'
import Dashboard from './components/Dashboard.jsx'
import Timeline from './components/Timeline.jsx'
import KnowledgeMap from './components/KnowledgeMap.jsx'
import Insights from './components/Insights.jsx'
import Sources from './components/Sources.jsx'
import ChatWidget from './components/ChatWidget.jsx'
import NotificationBell from './components/NotificationBell.jsx'
import Icon from './components/Icon.jsx'
import { BookStack, Logo, Notebook, Paper } from './components/Illustrations.jsx'

const NAV = [
  ['dashboard', 'Dashboard', 'home'], ['timeline', 'My Timeline', 'clock'], ['notes', 'My Notes', 'note'],
  ['tasks', 'My Tasks', 'check'], ['docs', 'Documents', 'folder'], ['github', 'GitHub', 'github'],
  ['events', 'Events', 'calendar'], ['map', 'Knowledge Map', 'graph'], ['insights', 'Insights', 'chart'],
]

export default function App() {
  const [user, setUser] = useState(session.load())
  const [tab, setTab] = useState('dashboard')
  const [project, setProject] = useState('')
  const [dash, setDash] = useState(null)
  const [tick, setTick] = useState(0)
  const [live, setLive] = useState(false)
  const [toast, setToast] = useState(null)

  const refresh = useCallback(() => setTick((t) => t + 1), [])
  const logout = useCallback(() => { session.clear(); setUser(null); setDash(null) }, [])
  const go = useCallback((t) => { setProject(''); setTab(t) }, [])

  useEffect(() => {
    if (!user) return
    api.dashboard().then(setDash).catch(() => {})
    const timer = setInterval(() => api.dashboard().then(setDash).catch(() => {}), 5 * 60 * 1000)
    return () => clearInterval(timer)
  }, [user, tick])

  useEffect(() => {                       // live updates from the server
    if (!user) return
    const es = new EventSource(api.streamUrl())
    es.addEventListener('hello', () => setLive(true))
    es.addEventListener('activity', refresh)
    es.addEventListener('linked', refresh)
    es.onerror = () => setLive(false)
    return () => es.close()
  }, [user, refresh])

  useEffect(() => {                       // banner once per session + optional desktop alerts
    if (!dash) return
    const urgent = dash.notices.filter((n) => n.severity !== 'info')
    if (urgent.length && !sessionStorage.getItem('dt.toast')) {
      const over = dash.notices.filter((n) => n.kind === 'OVERDUE').length
      const back = dash.notices.filter((n) => n.kind === 'BACKLOG').length
      setToast(`${over ? `${over} overdue` : ''}${over && back ? ', ' : ''}${back ? `${back} in backlog` : ''}${!over && !back ? `${urgent.length} tasks due soon` : ''}. Want to clear them?`)
      sessionStorage.setItem('dt.toast', '1')
    }
    if (typeof Notification !== 'undefined' && Notification.permission === 'granted') {
      const seen = new Set(JSON.parse(sessionStorage.getItem('dt.seen') || '[]'))
      dash.notices.filter((n) => n.severity !== 'info' || n.kind === 'EVENT_SOON').filter((n) => !seen.has(n.id)).slice(0, 3).forEach((n) => {
        new Notification(n.title, { body: n.message }); seen.add(n.id)
      })
      sessionStorage.setItem('dt.seen', JSON.stringify([...seen]))
    }
  }, [dash])

  if (!user) return <Landing onAuth={(s) => { sessionStorage.removeItem('dt.toast'); session.save(s); setUser(s) }} />

  const openNotice = (n) => go(n.kind === 'EVENT_SOON' ? 'events' : 'tasks')
  const common = { tick, live }
  const page = {
    dashboard: <Dashboard dash={dash} user={user} onNavigate={go} onOpenProject={(p) => { setProject(p); setTab('timeline') }} onNoticeOpen={openNotice} onRefresh={refresh} onLogout={logout} />,
    timeline: <Timeline key="tl" {...common} title="My Timeline" blurb="Everything you did, newest first." initialProject={project} />,
    notes: <Timeline key="no" {...common} title="My Notes" fixedTypes={['NOTE']} addType="NOTE" blurb="Ideas, meeting notes and thoughts." />,
    tasks: <Timeline key="ta" {...common} title="My Tasks" fixedTypes={['TASK']} addType="TASK" days={90} blurb="Open tasks show a due date. Red means overdue." />,
    docs: <Timeline key="do" {...common} title="Documents" fixedTypes={['DOCUMENT']} addType="DOCUMENT" blurb="Files you worked on." />,
    github: <Timeline key="gh" {...common} title="GitHub" fixedTypes={['GITHUB']} addType="GITHUB" blurb="Your code activity. Connect your username in Settings." />,
    events: <Timeline key="ev" {...common} title="Events" fixedTypes={['EVENT']} addType="EVENT" ahead={90} blurb="Upcoming events appear first." />,
    map: <KnowledgeMap tick={tick} />,
    insights: <Insights tick={tick} />,
    settings: <Sources onChange={refresh} />,
  }[tab]

  return (
    <div className="shell">
      <aside className="side">
        <Logo light />
        <div className="me"><span className="avatar big">{(user.displayName ?? '?')[0]}</span><div><b>{user.displayName}</b><span>{user.email}</span></div></div>
        <nav aria-label="Main">
          {NAV.map(([id, label, icon]) => (
            <button key={id} className="nav" aria-current={tab === id ? 'page' : undefined} onClick={() => go(id)}><Icon name={icon} />{label}</button>
          ))}
        </nav>
        <div className="side-foot">
          <button className="nav" aria-current={tab === 'settings' ? 'page' : undefined} onClick={() => go('settings')}><Icon name="gear" />Settings</button>
          <p className="tagline">Small notes. Big dreams.</p>
        </div>
        <Paper className="side-paper flutter" /><BookStack className="side-books float-b" />
      </aside>
      <main className="main">
        {tab !== 'dashboard' && (
          <div className="topbar"><NotificationBell notices={dash?.notices ?? []} onOpen={openNotice} />
            <button className="btn ghost small" onClick={logout}>Sign out</button></div>
        )}
        {toast && (
          <div className="toast" role="status"><span>{toast}</span>
            <button className="btn small" onClick={() => { setToast(null); go('tasks') }}>Review</button>
            <button className="icon-btn" aria-label="Dismiss" onClick={() => setToast(null)}><Icon name="x" size={16} /></button></div>
        )}
        {page}
        <Notebook className="corner-notebook float-a" />
      </main>
      <ChatWidget dash={dash} onNavigate={go} />
    </div>
  )
}
