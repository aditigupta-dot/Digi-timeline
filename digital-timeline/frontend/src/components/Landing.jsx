import { useState } from 'react'
import { api } from '../api.js'
import { BookStack, Brain, Logo, Notebook, Paper, Pen, Sparkles, Squiggle } from './Illustrations.jsx'
import Icon from './Icon.jsx'

const FEATURES = [
  ['clock', 'One timeline', 'Tasks, notes, documents, GitHub and events in a single story of your days.'],
  ['chat', 'DigiBot', 'Ask in plain words: “what did I do last week?” and get answers with proof.'],
  ['graph', 'Knowledge map', 'See how your work connects. Every link tells you why.'],
  ['bell', 'Smart reminders', 'Backlog, overdue tasks and upcoming events never slip away.'],
]

export default function Landing({ onAuth }) {
  const [mode, setMode] = useState('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [name, setName] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const go = async (fn) => {
    setBusy(true); setError('')
    try { onAuth(await fn()) } catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  const submit = (e) => {
    e.preventDefault()
    go(() => (mode === 'login' ? api.login(email, password) : api.register(email, password, name)))
  }

  return (
    <div className="landing">
      <Sparkles />
      <BookStack className="d-books float-a" />
      <Pen className="d-pen sway" />
      <Brain className="d-brain" />
      <Paper className="d-paper flutter" />
      <Notebook className="d-notebook float-b" />
      <BookStack className="d-books2 float-b" />

      <header className="land-nav"><Logo /></header>

      <section className="hero">
        <div className="hero-copy">
          <p className="eyebrow">Small notes. Big dreams.</p>
          <h1>Your thoughts, tasks and memories, all in one place.</h1>
          <p className="lead">DigiTimeline gathers everything you do into one timeline, connects the dots with a knowledge map,
            and lets you ask questions about your own work. It also nudges you about backlog and upcoming events.</p>
          <Squiggle className="hero-squiggle" />
          <div className="row">
            <button className="btn big" disabled={busy} onClick={() => go(() => api.login('demo@digitaltimeline.dev', 'demo1234'))}>
              Try the demo <Icon name="arrow" size={18} />
            </button>
            <button className="btn ghost big" onClick={() => document.getElementById('auth-email')?.focus()}>Sign in</button>
          </div>
        </div>

        <form className="auth-card" onSubmit={submit}>
          <h2>{mode === 'login' ? 'Welcome back' : 'Create your space'}</h2>
          <p className="muted">{mode === 'login' ? 'Sign in to continue your timeline.' : 'It takes ten seconds.'}</p>
          {mode === 'register' && (
            <div className="field"><label htmlFor="nm">Your name</label><input id="nm" value={name} onChange={(e) => setName(e.target.value)} required /></div>
          )}
          <div className="field"><label htmlFor="auth-email">Email</label><input id="auth-email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required /></div>
          <div className="field"><label htmlFor="pw">Password</label><input id="pw" type="password" minLength={8} value={password} onChange={(e) => setPassword(e.target.value)} required /></div>
          {error && <div className="error" role="alert">{error}</div>}
          <button className="btn wide" disabled={busy}>{busy ? 'Please wait…' : mode === 'login' ? 'Sign in' : 'Create account'}</button>
          <button type="button" className="link" onClick={() => { setError(''); setMode(mode === 'login' ? 'register' : 'login') }}>
            {mode === 'login' ? 'New here? Create an account' : 'Already have an account? Sign in'}
          </button>
        </form>
      </section>

      <section className="land-features">
        {FEATURES.map(([icon, title, text]) => (
          <div className="feat" key={title}><span className="feat-ico"><Icon name={icon} /></span><h3>{title}</h3><p>{text}</p></div>
        ))}
      </section>
    </div>
  )
}
