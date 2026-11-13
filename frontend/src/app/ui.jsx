import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { api } from '../api.js';
export const Skeleton = ({ lines = 3 }) => (<div aria-busy="true" aria-label="Loading">{Array.from({ length: lines }, (_, i) => <div key={i} className="skel" style={{ width: `${90 - i * 12}%` }} />)}</div>);
export const Empty = ({ title, children }) => (<div className="empty"><h3>{title}</h3><p>{children}</p></div>);
const ToastCtx = createContext(() => {});
export const useToast = () => useContext(ToastCtx);
export function ToastProvider({ children }) {
  const [items, setItems] = useState([]);
  const push = useCallback((message, kind = 'ok') => { const id = Math.random();
    setItems((x) => [...x, { id, message, kind }]); setTimeout(() => setItems((x) => x.filter((i) => i.id !== id)), 4000); }, []);
  return (<ToastCtx.Provider value={push}>{children}
    <div className="toasts" role="status" aria-live="polite"><AnimatePresence>{items.map((t) => (
      <motion.div key={t.id} className={'toast ' + t.kind} initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }}>{t.message}</motion.div>))}</AnimatePresence></div>
  </ToastCtx.Provider>);
}
/** Loads a GET endpoint; returns {data, error, loading, reload}. */
/** Changes every time the server reports a finished sync; useLoad refetches when it changes. */
export const LiveCtx = createContext(0);
export function useLoad(path) {
  const tick = useContext(LiveCtx);
  const [s, setS] = useState({ data: null, error: null, loading: true });
  const load = useCallback(() => { setS((x) => ({ ...x, loading: true })); api(path).then((data) => setS({ data, error: null, loading: false }))
    .catch((e) => setS({ data: null, error: e.message, loading: false })); }, [path]);
  useEffect(load, [load, tick]);
  return { ...s, reload: load };
}
export const fmt = (iso) => (iso ? new Date(iso).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' }) : '—');
