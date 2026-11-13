import { useEffect, useRef } from 'react';
import { useSearchParams } from 'react-router-dom';
import { api } from '../api.js';
import { Skeleton, fmt, useLoad, useToast } from './ui.jsx';
const NAMES = { GITHUB: 'GitHub', GMAIL: 'Gmail', CALENDAR: 'Google Calendar', CLASSROOM: 'Google Classroom' };
export default function Integrations() {
  const { data, loading, error, reload } = useLoad('/integrations'); const toast = useToast(); const [params, setParams] = useSearchParams();
  const timer = useRef();
  useEffect(() => { // result of the OAuth redirect
    if (params.get('connected')) toast(NAMES[params.get('connected').toUpperCase()] + ' connected'); if (params.get('error')) toast(params.get('error'), 'err');
    if (params.toString()) setParams({}, { replace: true });
  }, []); // eslint-disable-line
  useEffect(() => { clearTimeout(timer.current); if (data?.some((i) => i.syncing)) timer.current = setTimeout(reload, 2000); return () => clearTimeout(timer.current); }, [data]); // eslint-disable-line
  const act = async (fn, msg) => { try { await fn(); if (msg) toast(msg); reload(); } catch (e) { toast(e.message, 'err'); } };
  const connect = async (p) => { try { const r = await api(`/integrations/${p.toLowerCase()}/connect`, { method: 'POST' }); window.location.href = r.authorizeUrl; } catch (e) { toast(e.message, 'err'); } };
  if (loading && !data) return <Skeleton lines={4} />;
  if (error) return <p className="form-error" role="alert">{error}</p>;
  return (<><p className="eyebrow">SOURCES</p><h1 className="display sm">Integrations</h1>
    <p className="muted">A provider shows as connected only after real authorization succeeds. Syncs are manual for now; scheduled polling comes later.</p>
    <ul className="rows">{data.map((i) => (<li key={i.provider}><span><b>{NAMES[i.provider]}</b><br />
      <span className="mono">{i.status}{i.account ? ' · ' + i.account : ''}{i.lastSyncAt ? ' · last sync ' + fmt(i.lastSyncAt) : ''}{i.syncing ? ' · syncing…' : ''}</span>
      {i.lastError && <><br /><span className="form-error" role="alert">{i.lastError}</span></>}</span>
      <span className="acts">{!i.available ? <span className="muted">Not implemented yet</span> : i.status === 'DISCONNECTED' ? <button className="btn small" onClick={() => connect(i.provider)}>Connect</button> : <>
        {i.status === 'ERROR' && <button className="btn small" onClick={() => connect(i.provider)}>Reconnect</button>}
        <button className="btn small ghost" disabled={i.syncing} onClick={() => act(() => api('/sync/' + i.provider.toLowerCase(), { method: 'POST' }), 'Sync started')}>Sync now</button>
        <button className="linkbtn" onClick={() => act(() => api(`/integrations/${i.provider.toLowerCase()}/disconnect`, { method: 'POST' }), 'Disconnected')}>Disconnect</button></>}</span></li>))}</ul></>);
}
