import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { api, streamEvents } from '../api.js';
import { LiveCtx, useToast } from './ui.jsx';
const Value = createContext({ unread: 0, refreshUnread: () => {} });
export const useLive = () => useContext(Value);
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
/** Opens one live connection. On each finished sync: toast, bump the refresh counter, update the unread badge. */
export function LiveProvider({ children }) {
  const toast = useToast(); const [tick, setTick] = useState(0); const [unread, setUnread] = useState(0);
  const refreshUnread = useCallback(() => api('/notifications').then((r) => setUnread(r.unread)).catch(() => {}), []);
  useEffect(() => {
    refreshUnread(); const ctrl = new AbortController(); let stop = false;
    (async () => { let delay = 1000;
      while (!stop) {
        try { await streamEvents(ctrl.signal, (ev, d) => { delay = 1000;
          if (ev !== 'sync') return; setTick((t) => t + 1); refreshUnread();
          if (!d.success) toast(`${d.provider} sync failed: ${d.error}`, 'err'); else if (d.imported > 0) toast(`${d.provider}: ${d.imported} new`); }); }
        catch { if (stop) return; }
        await sleep(delay); delay = Math.min(delay * 2, 30000); // back off, then reconnect
      } })();
    return () => { stop = true; ctrl.abort(); };
  }, []); // eslint-disable-line
  return <LiveCtx.Provider value={tick}><Value.Provider value={{ unread, refreshUnread }}>{children}</Value.Provider></LiveCtx.Provider>;
}
