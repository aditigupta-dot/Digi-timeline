import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api, download } from '../api.js';
import { useAuth } from '../auth/AuthContext.jsx';
import { Skeleton, useLoad, useToast } from './ui.jsx';
export default function Settings() {
  const { logout } = useAuth(); const nav = useNavigate(); const toast = useToast(); const { data, loading, error } = useLoad('/settings');
  const [form, setForm] = useState(null); const [pw, setPw] = useState({ current: '', next: '', again: '' }); const [del, setDel] = useState({ password: '', confirm: '' });
  useEffect(() => { if (data && !form) setForm({ displayName: data.displayName, autoSync: data.autoSync, aiExternalDefault: data.aiExternalDefault }); }, [data]); // eslint-disable-line
  if ((loading && !data) || !form) return error ? <p className="form-error" role="alert">{error}</p> : <Skeleton lines={5} />;
  const fail = (e) => toast(e.message, 'err');
  const save = (e) => { e.preventDefault(); if (!form.displayName.trim()) return toast('Display name cannot be empty', 'err');
    api('/settings', { method: 'PATCH', body: form }).then(() => toast('Settings saved')).catch(fail); };
  const changePw = (e) => { e.preventDefault();
    if (pw.next.length < 8) return toast('New password needs at least 8 characters', 'err'); if (pw.next !== pw.again) return toast('New passwords do not match', 'err');
    api('/settings/password', { method: 'POST', body: { currentPassword: pw.current, newPassword: pw.next } }).then(() => logout()).catch(fail); };
  const everywhere = () => api('/settings/sign-out-everywhere', { method: 'POST' }).then(() => logout()).catch(fail);
  const remove = (e) => { e.preventDefault(); api('/settings/delete-account', { method: 'POST', body: { password: del.password } }).then(async () => { await logout(); nav('/'); }).catch(fail); };
  const Check = ({ k, children }) => <label className="muted" style={{ display: 'block', margin: '.5rem 0' }}><input type="checkbox" checked={form[k]} onChange={(e) => setForm({ ...form, [k]: e.target.checked })} /> {children}</label>;
  return (<><p className="eyebrow">ACCOUNT</p><h1 className="display sm">Settings</h1>
    <form onSubmit={save}><h2>Profile</h2><p className="muted">Signed in as <span className="mono">{data.email}</span></p>
      <div className="bar"><input aria-label="Display name" maxLength={80} value={form.displayName} onChange={(e) => setForm({ ...form, displayName: e.target.value })} /></div>
      <h2>Synchronization and AI</h2>
      <Check k="autoSync">Sync connected sources automatically (turn off to sync only when you press Sync now)</Check>
      <Check k="aiExternalDefault">Pre-tick "Use Gemini" in the Assistant (only has an effect if a Gemini key is configured on the server; sends titles and dates, never email content)</Check>
      <button className="btn small">Save</button></form>
    <form onSubmit={changePw}><h2>Security</h2>
      {[['current', 'Current password', 'current-password'], ['next', 'New password', 'new-password'], ['again', 'Repeat new password', 'new-password']].map(([k, l, a]) => (
        <label className="field" key={k} style={{ maxWidth: '22rem', marginBottom: '.6rem' }}><span>{l}</span><input type="password" autoComplete={a} value={pw[k]} onChange={(e) => setPw({ ...pw, [k]: e.target.value })} /></label>))}
      <div className="bar"><button className="btn small">Change password</button><button type="button" className="btn small ghost" onClick={everywhere}>Sign out everywhere</button></div>
      <p className="muted">Changing your password signs you out on every device.</p></form>
    <section><h2>Your data</h2><div className="bar">
      <button className="btn small ghost" onClick={() => download('/export/all.json', 'digitaltimeline-data.json').catch(fail)}>Download everything (JSON)</button>
      <button className="btn small ghost" onClick={() => download('/export/timeline.csv', 'digitaltimeline-export.csv').catch(fail)}>Timeline (CSV)</button></div>
      <p className="muted">Exports hold your latest 5,000 activities and never include passwords or provider tokens.</p></section>
    <form onSubmit={remove} className="empty danger"><h3>Delete account</h3>
      <p>This permanently deletes your account, activities, tasks, projects, notifications, AI chats and stored provider tokens. It cannot be undone. Access you granted to GitHub or Google stays authorised on their side until you revoke it in their settings.</p>
      <div className="bar"><input aria-label="Password" type="password" placeholder="Your password" value={del.password} onChange={(e) => setDel({ ...del, password: e.target.value })} />
        <input aria-label="Type DELETE" placeholder="Type DELETE" value={del.confirm} onChange={(e) => setDel({ ...del, confirm: e.target.value })} />
        <button className="btn small" disabled={del.confirm !== 'DELETE' || !del.password}>Delete my account</button></div></form></>);
}
