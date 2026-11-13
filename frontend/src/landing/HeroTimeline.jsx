import { motion, useMotionValue, useReducedMotion, useSpring, useTransform } from 'framer-motion';
import { CalendarDays, GitCommitHorizontal, ListChecks, Mail } from 'lucide-react';
// Story: email arrives -> task identified -> deadline connected -> GitHub activity. All original SVG, no assets.
const NODES = [
  { Icon: Mail, x: 160, y: 90, color: 'var(--terracotta)', label: 'Email: Assignment 4 brief', time: '09:12', side: 'r' },
  { Icon: ListChecks, x: 240, y: 200, color: 'var(--mustard)', label: 'Task suggested', time: '09:12', side: 'l' },
  { Icon: CalendarDays, x: 170, y: 330, color: 'var(--moss)', label: 'Deadline linked: Fri', time: 'FRI', side: 'r' },
  { Icon: GitCommitHorizontal, x: 235, y: 430, color: 'var(--ink)', label: 'Commit pushed', time: '21:40', side: 'l' },
];
const SPINE = 'M200 20 Q150 55 160 90 T240 200 T170 330 T235 430 T200 485';
const curve = (a, b) => `M${a.x} ${a.y} C${a.x + 90} ${a.y + 40}, ${b.x - 90} ${b.y - 40}, ${b.x} ${b.y}`;
export default function HeroTimeline() {
  const reduce = useReducedMotion();
  const mx = useMotionValue(0), my = useMotionValue(0);
  const sx = useSpring(mx, { stiffness: 60, damping: 20 }), sy = useSpring(my, { stiffness: 60, damping: 20 });
  const px = useTransform(sx, (v) => v * 14), py = useTransform(sy, (v) => v * 10);
  const onMove = (e) => { const r = e.currentTarget.getBoundingClientRect();
    mx.set((e.clientX - r.left) / r.width - 0.5); my.set((e.clientY - r.top) / r.height - 0.5); };
  const d = (s) => (reduce ? 0 : s);
  return (
    <div className="hero-art" onPointerMove={reduce ? undefined : onMove}>
      <svg viewBox="0 0 400 500" role="img" aria-label="Animated timeline: an email becomes a task, is linked to a calendar deadline, and is followed by a GitHub commit.">
        <motion.g style={{ x: px, y: py }}>
          <motion.path d={SPINE} fill="none" stroke="var(--slate)" strokeWidth="2" strokeLinecap="round"
            initial={{ pathLength: reduce ? 1 : 0 }} animate={{ pathLength: 1 }} transition={{ duration: d(2.6), ease: 'easeInOut' }} />
          {[[0, 2], [1, 3]].map(([a, b], i) => (
            <motion.path key={i} d={curve(NODES[a], NODES[b])} fill="none" stroke="var(--terracotta)" strokeWidth="1.2" strokeDasharray="4 5"
              initial={{ pathLength: reduce ? 1 : 0, opacity: 0.8 }} animate={{ pathLength: 1 }} transition={{ delay: d(3 + i * 0.6), duration: d(1.2) }} />))}
          {NODES.map((n, i) => (
            <motion.g key={n.label} initial={{ opacity: reduce ? 1 : 0, scale: reduce ? 1 : 0.6 }} animate={{ opacity: 1, scale: 1 }}
              transition={{ delay: d(0.6 + i * 0.8), type: 'spring', stiffness: 160, damping: 16 }} style={{ transformOrigin: `${n.x}px ${n.y}px` }}>
              {!reduce && <motion.circle cx={n.x} cy={n.y} r="22" fill="none" stroke={n.color} strokeWidth="1"
                animate={{ r: [20, 30], opacity: [0.5, 0] }} transition={{ repeat: Infinity, duration: 2.8, delay: i * 0.5 }} />}
              <circle cx={n.x} cy={n.y} r="20" fill="var(--ivory)" stroke={n.color} strokeWidth="2" />
              <n.Icon x={n.x - 9} y={n.y - 9} size={18} color={n.color} strokeWidth={1.8} />
              <text x={n.x + (n.side === 'r' ? 32 : -32)} y={n.y - 3} textAnchor={n.side === 'r' ? 'start' : 'end'} className="svg-label">{n.label}</text>
              <text x={n.x + (n.side === 'r' ? 32 : -32)} y={n.y + 12} textAnchor={n.side === 'r' ? 'start' : 'end'} className="svg-time">{n.time}</text>
            </motion.g>))}
          {!reduce && <motion.circle r="5" fill="var(--mustard)" initial={{ cx: 200, cy: 20 }}
            animate={{ cx: [200, ...NODES.map((n) => n.x), 200], cy: [20, ...NODES.map((n) => n.y), 485] }}
            transition={{ duration: 9, delay: 3.6, repeat: Infinity, ease: 'easeInOut' }} />}
        </motion.g>
      </svg>
    </div>);
}
