import { useEffect, useMemo, useRef, useState } from 'react'
import ForceGraph2D from 'react-force-graph-2d'
import { api } from '../api.js'
import { TYPE_COLOR } from './ActivityCard.jsx'

const PALETTE = ['#b3382b', '#2f6f9f', '#3d8b6a', '#c98a2b', '#7a5ac8', '#a24a7a', '#5a7a2f', '#2b7f86']
const NONE = '#9a8680'

export default function KnowledgeMap({ tick }) {
  const [days, setDays] = useState(30)
  const [view, setView] = useState(null)
  const [clusters, setClusters] = useState([])
  const [mode, setMode] = useState('map')
  const [minW, setMinW] = useState(0.4)
  const [project, setProject] = useState('')
  const [find, setFind] = useState('')
  const [sel, setSel] = useState(null)
  const [detail, setDetail] = useState(null)
  const [related, setRelated] = useState([])
  const [focus, setFocus] = useState(null)
  const [hover, setHover] = useState(null)
  const fg = useRef(null)
  const box = useRef(null)
  const [width, setWidth] = useState(700)

  useEffect(() => {
    api.graph(days).then(setView).catch(() => {})
    api.clusters(days).then(setClusters).catch(() => {})
  }, [days, tick])
  useEffect(() => {
    const fit = () => box.current && setWidth(box.current.clientWidth)
    fit(); window.addEventListener('resize', fit)
    return () => window.removeEventListener('resize', fit)
  }, [mode])

  const projects = useMemo(() => [...new Set((view?.nodes ?? []).map((n) => n.project).filter(Boolean))].sort(), [view])
  const colorOf = (p) => (p ? PALETTE[projects.indexOf(p) % PALETTE.length] : NONE)

  const graph = useMemo(() => {
    if (!view) return null
    const q = find.trim().toLowerCase()
    const nodes = view.nodes.filter((n) => (!project || n.project === project) && (!q || n.title.toLowerCase().includes(q)))
    const ids = new Set(nodes.map((n) => n.id))
    const edges = view.edges.filter((e) => e.weight >= minW && ids.has(e.from) && ids.has(e.to))
    const deg = {}
    edges.forEach((e) => { deg[e.from] = (deg[e.from] || 0) + 1; deg[e.to] = (deg[e.to] || 0) + 1 })
    return { data: { nodes: nodes.map((n) => ({ ...n })), links: edges.map((e) => ({ source: e.from, target: e.to, weight: e.weight, reason: e.reason })) }, deg, edges }
  }, [view, project, find, minW])

  const highlight = useMemo(() => {
    if (focus) return focus
    if (!sel || !graph) return null
    const s = new Set([sel])
    graph.edges.forEach((e) => { if (e.from === sel) s.add(e.to); if (e.to === sel) s.add(e.from) })
    return s
  }, [focus, sel, graph])

  const pick = async (id) => {
    setFocus(null); setSel(id); setDetail(null); setRelated([])
    api.get(id).then(setDetail).catch(() => {})
    api.related(id).then(setRelated).catch(() => {})
  }
  const showThread = (c) => { setFocus(new Set(c.members)); setSel(null); setDetail(null); setMode('map') }

  const paint = (node, ctx, scale) => {
    const r = 4 + Math.min(9, (graph?.deg[node.id] || 0) * 1.4)
    const dim = highlight && !highlight.has(node.id)
    ctx.globalAlpha = dim ? 0.14 : 1
    ctx.beginPath(); ctx.arc(node.x, node.y, r, 0, 2 * Math.PI)
    ctx.fillStyle = colorOf(node.project); ctx.fill()
    if (node.id === sel || node.id === hover) { ctx.lineWidth = 2 / scale; ctx.strokeStyle = '#3b1613'; ctx.stroke() }
    if (!dim && (node.id === sel || node.id === hover || (highlight && highlight.has(node.id)) || scale > 2.4)) {
      const text = node.title.length > 30 ? node.title.slice(0, 29) + '…' : node.title
      ctx.font = `${12 / scale}px Poppins, sans-serif`
      ctx.textAlign = 'center'; ctx.fillStyle = '#3b1613'
      ctx.fillText(text, node.x, node.y + r + 11 / scale)
    }
    ctx.globalAlpha = 1
  }
  const linkTouches = (l) => {
    const s = typeof l.source === 'object' ? l.source.id : l.source
    const t = typeof l.target === 'object' ? l.target.id : l.target
    return highlight ? highlight.has(s) && highlight.has(t) : null
  }

  return (
    <>
      <div className="head">
        <div><h1>Knowledge Map</h1><p className="muted">See how the things you did are connected. {view?.nodes.length ?? 0} items, {view?.edges.length ?? 0} connections ({view?.backend}).</p></div>
        <div className="seg" role="tablist">
          <button role="tab" aria-selected={mode === 'map'} onClick={() => setMode('map')}>Map</button>
          <button role="tab" aria-selected={mode === 'threads'} onClick={() => setMode('threads')}>Story threads</button>
        </div>
      </div>

      <section className="howto">
        <div><span className="step">1</span><b>Each dot is something you did</b><p>A note, task, file, commit or event. Bigger dot means more connections.</p></div>
        <div><span className="step">2</span><b>A line means “these are related”</b><p>Thicker line = stronger link. Hover a line to read why they are linked.</p></div>
        <div><span className="step">3</span><b>Click any dot</b><p>Everything not connected to it fades, and the panel shows what it links to.</p></div>
      </section>

      {mode === 'threads' ? (
        <>
          <p className="muted">A “story thread” is a group of activities that belong together, found automatically. Press “Show on map” to see one.</p>
          <div className="grid2">
            {clusters.map((c, i) => (
              <div className="panel" key={i}>
                <h2>{c.label || 'Untitled thread'}</h2>
                <div className="mono">{c.members.length} activities · {new Date(c.start).toLocaleDateString()} to {new Date(c.end).toLocaleDateString()}</div>
                <ul className="plain">{c.titles.map((t) => <li key={t}>{t}</li>)}</ul>
                <button className="btn small" onClick={() => showThread(c)}>Show on map</button>
              </div>
            ))}
            {clusters.length === 0 && <div className="notice">No threads yet. Add related activities and they will be grouped here.</div>}
          </div>
        </>
      ) : (
        <>
          <div className="filters">
            <input className="grow" aria-label="Find an item" placeholder="Find an item by title…" value={find} onChange={(e) => setFind(e.target.value)} />
            <select aria-label="Project" value={project} onChange={(e) => setProject(e.target.value)}><option value="">All projects</option>{projects.map((p) => <option key={p}>{p}</option>)}</select>
            <select aria-label="Range" value={days} onChange={(e) => setDays(+e.target.value)}><option value={14}>Last 14 days</option><option value={30}>Last 30 days</option><option value={90}>Last 90 days</option></select>
            <label className="slider">Only stronger links
              <input type="range" min="0.3" max="0.8" step="0.05" value={minW} onChange={(e) => setMinW(+e.target.value)} /></label>
            {(focus || sel) && <button className="btn ghost small" onClick={() => { setFocus(null); setSel(null); setDetail(null) }}>Clear selection</button>}
          </div>
          <div className="legend">{projects.map((p) => <span key={p}><i style={{ background: colorOf(p) }} />{p}</span>)}</div>
          <div className="map-layout">
            <div className="graphbox" ref={box}>
              {graph && graph.data.nodes.length > 0 ? (
                <ForceGraph2D ref={fg} graphData={graph.data} width={width} height={520} backgroundColor="#fffaf6"
                  nodeCanvasObject={paint} nodePointerAreaPaint={(n, c, ctx) => { ctx.fillStyle = c; ctx.beginPath(); ctx.arc(n.x, n.y, 11, 0, 2 * Math.PI); ctx.fill() }}
                  linkWidth={(l) => (linkTouches(l) ? 1.5 + l.weight * 4 : 0.6 + l.weight * 2.5)}
                  linkColor={(l) => { const t = linkTouches(l); return t === null ? 'rgba(130,70,60,.35)' : t ? '#b3382b' : 'rgba(130,70,60,.06)' }}
                  linkLabel={(l) => `Linked because: ${l.reason}`} onNodeClick={(n) => pick(n.id)} onNodeHover={(n) => setHover(n ? n.id : null)}
                  onBackgroundClick={() => { setSel(null); setFocus(null); setDetail(null) }}
                  cooldownTicks={140} onEngineStop={() => fg.current?.zoomToFit(400, 50)} />
              ) : <div className="notice m">No items match. Try a wider range or lower the link strength.</div>}
            </div>
            <aside className="panel side-detail">
              {!sel && <><h2>Nothing selected</h2><p className="muted">Click a dot on the map to see what it is and why it is connected to other things.</p></>}
              {sel && !detail && <p className="muted">Loading…</p>}
              {detail && (
                <>
                  <h2>{detail.title}</h2>
                  <div className="meta"><span className="kind" style={{ '--tc': TYPE_COLOR[detail.type] }}>{detail.type.toLowerCase()}</span>{detail.project && <span className="proj">{detail.project}</span>}<span className="mono">{new Date(detail.occurredAt).toLocaleDateString()}</span></div>
                  {detail.content && <p className="snippet">{detail.content.slice(0, 220)}{detail.content.length > 220 ? '…' : ''}</p>}
                  <b className="small-title">Connected to ({related.length})</b>
                  {related.length === 0 && <p className="muted">No strong connections yet.</p>}
                  {related.map((r) => (
                    <button key={r.activity.id} className="rel" onClick={() => pick(r.activity.id)}>
                      <span className="bar"><i style={{ width: `${Math.round(r.weight * 100)}%` }} /></span>
                      <b>{r.activity.title}</b><span className="muted">because {r.reason}</span>
                    </button>
                  ))}
                </>
              )}
            </aside>
          </div>
        </>
      )}
    </>
  )
}
