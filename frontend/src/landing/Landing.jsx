import { useState } from 'react';
import { Link } from 'react-router-dom';
import { AnimatePresence, motion } from 'framer-motion';
import HeroTimeline from './HeroTimeline.jsx';
const STEPS = [['01', 'Collect', 'Gmail, GitHub and Calendar are read with read-only permissions you grant.'], ['02', 'Connect', 'Related events are linked, and every link says why it exists.'],
  ['03', 'Suggest', 'Possible tasks appear as suggestions. Nothing is marked done without you.'], ['04', 'Prioritise', 'A transparent score ranks what needs attention, with its reasons shown.']];
const SAMPLE = [['Gmail', 'Email: Assignment 4 brief received', 'Mon 09:12'], ['Tasks', 'Suggested task: Submit Assignment 4 (unconfirmed)', 'Mon 09:12'],
  ['GitHub', 'Commit: add parser tests', 'Thu 21:40'], ['Calendar', 'Deadline: Assignment 4 due', 'Fri 17:00']];
const SOURCES = [['GitHub', 'Commits, issues and pull requests'], ['Gmail', 'Subject, sender and time. Never message bodies'], ['Google Calendar', 'Events and deadlines'], ['Google Classroom', 'Assignments, only if your account has courses']];
function Preview() {
  const [f, setF] = useState('All'); const shown = SAMPLE.filter((s) => f === 'All' || s[0] === f);
  return (<div><div className="chips" role="group" aria-label="Filter sample timeline">{['All', 'Gmail', 'GitHub', 'Calendar', 'Tasks'].map((c) => <button key={c} aria-pressed={f === c} className={f === c ? 'on' : ''} onClick={() => setF(c)}>{c}</button>)}</div>
    <ul className="rows"><AnimatePresence initial={false}>{shown.map((s) => <motion.li layout key={s[1]} initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}><span>{s[1]}</span><span className="mono">{s[0]} · {s[2]}</span></motion.li>)}</AnimatePresence></ul>
    <p className="muted mono">Illustrative sample, not real data.</p></div>);
}
const Band = ({ id, tone, eyebrow, title, children }) => (<section id={id} className={'band ' + (tone || '')}><div className="split"><div><p className="eyebrow">{eyebrow}</p><h2 className="display sm">{title}</h2></div><div>{children}</div></div></section>);
export default function Landing() {
  return (<>
    <header className="nav"><span className="brand">DigitalTimeline</span><nav><Link to="/login">Log in</Link><Link to="/register" className="btn small">Get started</Link></nav></header>
    <section className="hero"><div className="hero-copy"><p className="eyebrow">PERSONAL PRODUCTIVITY INTELLIGENCE</p>
      <h1 className="display">Your digital life,<br />finally in <em>one</em><br />timeline.</h1>
      <p className="lede">Emails, commits and deadlines arrive in different places. DigitalTimeline lays them on a single line and shows how they relate.</p>
      <div className="cta"><Link to="/app" className="btn">Explore Dashboard</Link><a href="#how" className="btn ghost">See How It Works</a></div>
      <p className="mono trust">One timeline. Multiple sources. Clearer priorities.</p></div><HeroTimeline /></section>
    <section id="how" className="how"><h2 className="display sm">How it works</h2><ol>{STEPS.map(([n, t, d], i) => (<li key={n} style={{ marginLeft: `${i % 2 ? 12 : 0}%` }}><span className="mono">{n}</span><h3>{t}</h3><p>{d}</p></li>))}</ol></section>
    <Band eyebrow="THE PROBLEM" title="Deadlines are scattered."><p className="lede">An assignment lands in your inbox, its deadline sits in a calendar, and the work happens in a repository. Nothing connects them, so you do the connecting in your head.</p></Band>
    <Band tone="paper" eyebrow="CONNECTED SOURCES" title="Read-only, and only what you authorise."><ul className="rows">{SOURCES.map(([n, d]) => <li key={n}><b>{n}</b><span className="muted">{d}</span></li>)}</ul></Band>
    <Band eyebrow="TIMELINE PREVIEW" title="Everything in order."><Preview /></Band>
    <Band tone="paper" eyebrow="SMART TASKS" title="Suggestions, never assumptions."><p className="lede">Tasks found in your data are labelled as suggestions until you confirm them. A commit never marks work complete. Ranking is a visible score, and each point of it comes with a reason.</p></Band>
    <Band eyebrow="AI PRODUCTIVITY INTELLIGENCE" title="Answers that show their sources."><p className="lede">The assistant answers only from records in your own account and lists the ones it used. It works with no AI key at all. The optional Gemini mode sees titles and dates only, and only if you switch it on.</p></Band>
    <Band tone="ink" eyebrow="PRIVACY AND CONTROL" title="Yours to inspect, export and delete."><ul className="plain"><li>Read-only permissions for every provider</li><li>Provider tokens encrypted at rest</li><li>Email bodies are never fetched</li><li>One-click export and full account deletion</li></ul></Band>
    <section className="final"><h2 className="display">See your week as one line.</h2><Link to="/register" className="btn">Create your account</Link></section>
    <footer className="foot"><span className="brand">DigitalTimeline</span><span className="mono">Final-year project · read-only by design</span></footer></>);
}
