import { useEffect, useRef, useState } from 'react'
import { api } from '../api.js'
import Icon from './Icon.jsx'
import { Brain } from './Illustrations.jsx'

const START = [
  'What did I do last week?',
  'What is pending in my backlog?',
  'Any upcoming events?',
  'How did my thesis work progress?',
]

function lines(text) {
  return text.split('\n').map((l, i) => {
    const parts = l.split(/\*\*(.+?)\*\*/g)
    return <div key={i} className="ln">{parts.map((p, j) => (j % 2 ? <b key={j}>{p}</b> : p))}</div>
  })
}

// Quick answers that need no AI call: they come straight from the dashboard data.
function localIntent(text, dash) {
  const t = text.toLowerCase()
  if (/^(hi|hii|hello|hey|namaste|yo)\b/.test(t)) {
    return { text: `Hi ${dash?.displayName?.split(' ')[0] ?? 'there'}! I'm DigiBot. Ask me about your work, your backlog or upcoming events.` }
  }
  if (/\b(what can you do|help|kya kar)\b/.test(t)) {
    return { text: 'I can:\n- answer questions about your timeline (“what did I do last week?”)\n- tell you what is **pending or overdue**\n- list **upcoming events**\n- find related work. Every answer shows its evidence.' }
  }
  if (dash && /\b(backlog|pending|overdue|baaki|reminders?|due|late)\b/.test(t)) {
    const n = dash.notices.filter((x) => x.kind !== 'EVENT_SOON')
    if (!n.length) return { text: 'Nothing is pending. You are all caught up!' }
    return { text: `You have **${n.length}** items needing attention:\n` + n.slice(0, 8).map((x) => `- **${x.title}**: ${x.message}`).join('\n') + (n.length > 8 ? `\n…and ${n.length - 8} more.` : ''), go: 'tasks' }
  }
  if (dash && /\b(upcoming|events?|schedule|meeting|calendar)\b/.test(t) && !/\b(did|last)\b/.test(t)) {
    if (!dash.upcomingEvents.length) return { text: 'No upcoming events in your calendar.' }
    return { text: 'Coming up:\n' + dash.upcomingEvents.map((e) => `- **${e.title}**: ${new Date(e.date + 'T00:00').toLocaleDateString([], { day: 'numeric', month: 'short' })}, ${e.time}`).join('\n'), go: 'events' }
  }
  return null
}

export default function ChatWidget({ dash, onNavigate }) {
  const [open, setOpen] = useState(false)
  const [busy, setBusy] = useState(false)
  const [input, setInput] = useState('')
  const [msgs, setMsgs] = useState([{ from: 'bot', text: 'Hi! I am DigiBot. Ask me anything about your timeline.' }])
  const end = useRef(null)

  useEffect(() => { end.current?.scrollIntoView({ behavior: 'smooth' }) }, [msgs, open])

  const send = async (q) => {
    const text = (q ?? input).trim()
    if (!text || busy) return
    setInput('')
    setMsgs((m) => [...m, { from: 'me', text }])
    const quick = localIntent(text, dash)
    if (quick) { setMsgs((m) => [...m, { from: 'bot', ...quick }]); return }
    setBusy(true)
    try {
      const r = await api.ask(text)
      setMsgs((m) => [...m, { from: 'bot', text: r.answer, cites: r.citations.filter((c) => c.citedInAnswer).slice(0, 5), follow: r.followUps, meta: r.mode }])
    } catch (e) {
      setMsgs((m) => [...m, { from: 'bot', text: `Sorry, I could not answer that: ${e.message}` }])
    } finally { setBusy(false) }
  }

  return (
    <>
      <button className={`chat-fab ${open ? 'hide' : ''}`} onClick={() => setOpen(true)} aria-label="Open DigiBot chat">
        <Icon name="chat" size={26} /><span>Ask DigiBot</span>
      </button>
      {open && (
        <section className="chat" aria-label="DigiBot chat">
          <header>
            <Brain className="chat-brain" />
            <div><b>DigiBot</b><span>Knows your timeline</span></div>
            <button className="icon-btn" onClick={() => setOpen(false)} aria-label="Close chat"><Icon name="x" /></button>
          </header>
          <div className="chat-body">
            {msgs.map((m, i) => (
              <div key={i} className={`msg ${m.from}`}>
                <div className="bubble">{lines(m.text)}</div>
                {m.cites?.length > 0 && (
                  <div className="cites">{m.cites.map((c) => <span key={c.id} className="cite-chip" title={c.why}>{c.ref} · {c.title}</span>)}</div>
                )}
                {m.go && <button className="chip" onClick={() => { onNavigate(m.go); setOpen(false) }}>Open the list</button>}
                {m.follow && i === msgs.length - 1 && <div className="chips">{m.follow.slice(0, 3).map((f) => <button key={f} className="chip" onClick={() => send(f)}>{f}</button>)}</div>}
              </div>
            ))}
            {msgs.length === 1 && <div className="chips">{START.map((s) => <button key={s} className="chip" onClick={() => send(s)}>{s}</button>)}</div>}
            {busy && <div className="msg bot"><div className="bubble typing"><i /><i /><i /></div></div>}
            <div ref={end} />
          </div>
          <form onSubmit={(e) => { e.preventDefault(); send() }}>
            <input aria-label="Message DigiBot" value={input} onChange={(e) => setInput(e.target.value)} placeholder="Ask about your work…" />
            <button className="send" aria-label="Send" disabled={busy}><Icon name="send" size={18} /></button>
          </form>
        </section>
      )}
    </>
  )
}
