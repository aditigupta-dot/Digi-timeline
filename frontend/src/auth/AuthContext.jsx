import { createContext, useContext, useEffect, useState } from 'react';
import { authCall, hasRefreshToken, refreshSession, setTokens } from '../api.js';
const Ctx = createContext(null);
export const useAuth = () => useContext(Ctx);
/** status: 'loading' (checking saved session) | 'in' | 'out' */
export function AuthProvider({ children }) {
  const [status, setStatus] = useState('loading');
  useEffect(() => {
    if (!hasRefreshToken()) { setStatus('out'); return; }
    refreshSession().then(() => setStatus('in')).catch(() => { setTokens(null); setStatus('out'); });
  }, []);
  const finish = (t) => { setTokens(t); setStatus('in'); };
  const value = { status,
    login: async (email, password) => finish(await authCall('/auth/login', { email, password })),
    register: async (email, displayName, password) => finish(await authCall('/auth/register', { email, displayName, password })),
    logout: async () => {
      const rt = localStorage.getItem('dt_refresh');
      if (rt) await authCall('/auth/logout', { refreshToken: rt }).catch(() => {});
      setTokens(null); setStatus('out');
    } };
  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}
