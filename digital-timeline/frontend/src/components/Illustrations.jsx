// Hand-built SVG illustrations (books, pen, brain, paper, notebook). Their motion lives in styles.css and
// is switched off automatically for people who prefer reduced motion.

export function BookStack({ className = '' }) {
  return (
    <svg viewBox="0 0 200 150" className={`ill ${className}`} aria-hidden="true">
      <ellipse cx="100" cy="138" rx="82" ry="8" fill="#5a1d14" opacity=".16" />
      <rect x="10" y="92" width="170" height="38" rx="6" fill="#6f1f19" />
      <rect x="10" y="92" width="170" height="9" rx="4" fill="#8f2a21" />
      <rect x="10" y="92" width="11" height="38" rx="4" fill="#591611" />
      <rect x="22" y="108" width="150" height="16" rx="3" fill="#f4e4d6" />
      <path d="M26 112h140M26 116h140M26 120h140" stroke="#dcc3b2" strokeWidth="1" />
      <rect x="26" y="58" width="150" height="36" rx="6" fill="#b3382b" />
      <rect x="26" y="58" width="150" height="8" rx="4" fill="#cc4a3b" />
      <rect x="26" y="58" width="10" height="36" rx="4" fill="#8d2b21" />
      <rect x="62" y="64" width="62" height="4" rx="2" fill="#f0c2ac" opacity=".85" />
      <rect x="38" y="76" width="132" height="14" rx="3" fill="#f6e8dc" />
      <path d="M42 80h124M42 84h124M42 88h124" stroke="#e0c9b9" strokeWidth="1" />
      <g transform="rotate(-6 100 40)">
        <rect x="40" y="22" width="122" height="34" rx="5" fill="#4b1914" />
        <rect x="40" y="22" width="122" height="8" rx="4" fill="#64231b" />
        <rect x="48" y="40" width="106" height="12" rx="3" fill="#f2e1d3" />
        <circle cx="101" cy="31" r="3.500" fill="#e8b89a" />
      </g>
    </svg>
  )
}

export function Pen({ className = '' }) {
  return (
    <svg viewBox="0 0 220 40" className={`ill ${className}`} aria-hidden="true">
      <path d="M62 13 L18 20 L62 27 Z" fill="#e3c9b8" />
      <path d="M22 20 L6 20" stroke="#3b1613" strokeWidth="2.500" strokeLinecap="round" />
      <rect x="60" y="11" width="140" height="18" rx="9" fill="#7d241d" />
      <rect x="64" y="14" width="130" height="4" rx="2" fill="#a8392c" opacity=".7" />
      <rect x="150" y="11" width="12" height="18" fill="#e8b89a" />
      <rect x="172" y="9" width="40" height="22" rx="10" fill="#a8392c" />
    </svg>
  )
}

export function Squiggle({ className = '' }) {
  return (
    <svg viewBox="0 0 200 40" className={className} aria-hidden="true">
      <path className="write" d="M4 26 C20 2 30 38 46 18 S74 4 90 22 S120 36 136 14 S170 6 196 20"
            fill="none" stroke="#b3382b" strokeWidth="2.200" strokeLinecap="round" />
    </svg>
  )
}

export function Brain({ className = '' }) {
  const rays = [-78, -56, -34, -12, 12, 34, 56, 78].map((deg, i) => {
    const a = (deg * Math.PI) / 180
    const p = (r) => [100 + r * Math.sin(a), 104 - r * Math.cos(a)]
    const [x1, y1] = p(66), [x2, y2] = p(84)
    return <line key={i} x1={x1} y1={y1} x2={x2} y2={y2} style={{ animationDelay: `${i * 0.18}s` }} />
  })
  const lobe = 'M98 66 C88 48 62 46 50 62 C34 62 24 78 30 92 C20 102 24 120 40 124 C42 138 60 146 74 138 C82 146 96 144 98 134 Z'
  return (
    <svg viewBox="0 0 200 160" className={`ill brain ${className}`} aria-hidden="true">
      <defs>
        <linearGradient id="brainG" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#e0705a" /><stop offset="1" stopColor="#a8301f" />
        </linearGradient>
      </defs>
      <g className="rays" stroke="#d0513c" strokeWidth="3" strokeLinecap="round">{rays}</g>
      <path d={lobe} fill="url(#brainG)" />
      <path d={lobe} fill="url(#brainG)" transform="translate(200 0) scale(-1 1)" />
      <g fill="none" stroke="#f7c9b8" strokeWidth="2.200" strokeLinecap="round" opacity=".8">
        <path d="M40 88 C52 82 58 94 70 88" /><path d="M46 108 C58 102 66 114 80 106" />
        <path d="M70 66 C74 76 66 80 72 92" /><path d="M160 88 C148 82 142 94 130 88" />
        <path d="M154 108 C142 102 134 114 120 106" /><path d="M130 66 C126 76 134 80 128 92" />
        <path d="M100 70 V132" strokeDasharray="3 5" />
      </g>
    </svg>
  )
}

export function Paper({ className = '' }) {
  return (
    <svg viewBox="0 0 120 150" className={`ill ${className}`} aria-hidden="true">
      <path d="M10 8H88L112 32V142H10Z" fill="#fbefe6" stroke="#e2c6b8" strokeWidth="1.500" />
      <path d="M88 8V32H112Z" fill="#edd3c5" />
      <path d="M24 56h76M24 70h76M24 84h76M24 98h54" stroke="#d3b3a2" strokeWidth="2" strokeLinecap="round" />
      <path d="M24 118c8-8 12 6 20-2s12 6 22-2" fill="none" stroke="#b3382b" strokeWidth="2" strokeLinecap="round" />
    </svg>
  )
}

export function Notebook({ className = '' }) {
  return (
    <svg viewBox="0 0 280 200" className={`ill ${className}`} aria-hidden="true">
      <path d="M14 40 L136 22 V176 L14 190Z" fill="#6f1f19" />
      <path d="M266 40 L144 22 V176 L266 190Z" fill="#6f1f19" />
      <path d="M22 42 L134 28 V170 L22 182Z" fill="#f6e8dc" />
      <path d="M258 42 L146 28 V170 L258 182Z" fill="#fbf0e7" />
      <path d="M34 62l90-10M34 80l90-10M34 98l90-10M34 116l90-10M34 134l90-10" stroke="#e2c9ba" strokeWidth="1.500" />
      <text x="176" y="76" fontFamily="Caveat, cursive" fontSize="25" fontWeight="700" fill="#4b1914" transform="rotate(7 176 76)">Better</text>
      <text x="180" y="102" fontFamily="Caveat, cursive" fontSize="25" fontWeight="700" fill="#4b1914" transform="rotate(7 180 102)">Ideas</text>
      <text x="184" y="128" fontFamily="Caveat, cursive" fontSize="25" fontWeight="700" fill="#4b1914" transform="rotate(7 184 128)">Ahead…</text>
      <path className="heart" d="M214 156c-9-7-14-12-14-17 0-4 3-6 6-6 3 0 6 2 8 5 2-3 5-5 8-5 3 0 6 2 6 6 0 5-5 10-14 17z" fill="#d2453a" transform="translate(-4 4)" />
    </svg>
  )
}

export function Sparkles() {
  const dots = [[8, 18], [22, 62], [40, 30], [58, 80], [72, 22], [88, 54], [14, 88], [66, 44]]
  return (
    <div className="sparkles" aria-hidden="true">
      {dots.map(([x, y], i) => <i key={i} style={{ left: `${x}%`, top: `${y}%`, animationDelay: `${i * 0.7}s` }} />)}
    </div>
  )
}

export function Logo({ light = false }) {
  return (
    <div className={`logo ${light ? 'light' : ''}`}>
      <svg width="34" height="34" viewBox="0 0 34 34" aria-hidden="true">
        <rect x="2" y="2" width="14" height="14" rx="5" fill="#e9684f" /><rect x="18" y="2" width="14" height="14" rx="5" fill="#f4d9cd" />
        <rect x="2" y="18" width="14" height="14" rx="5" fill="#f4d9cd" /><rect x="18" y="18" width="14" height="14" rx="5" fill="#c4402f" />
      </svg>
      <span>DigiTimeline</span>
    </div>
  )
}
