import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
export default function AuthPage({ mode }) {
  const isRegister = mode === 'register';
  const { login, register } = useAuth(); const nav = useNavigate();
  const [f, setF] = useState({ email: '', name: '', password: '' });
  const [errors, setErrors] = useState({}); const [serverError, setServerError] = useState(''); const [busy, setBusy] = useState(false);
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });
  function validate() {
    const x = {};
    if (!/^\S+@\S+\.\S+$/.test(f.email)) x.email = 'Enter a valid email address.';
    if (isRegister && !f.name.trim()) x.name = 'Tell us what to call you.';
    if (f.password.length < 8) x.password = 'Use at least 8 characters.';
    setErrors(x); return Object.keys(x).length === 0;
  }
  async function submit(e) {
    e.preventDefault(); setServerError('');
    if (!validate()) return;
    setBusy(true);
    try { isRegister ? await register(f.email, f.name, f.password) : await login(f.email, f.password); nav('/app'); }
    catch (err) { setServerError(err.message); } finally { setBusy(false); }
  }
  const field = (k, label, type, auto) => (
    <label className="field"><span>{label}</span>
      <input type={type} value={f[k]} onChange={set(k)} autoComplete={auto} aria-invalid={!!errors[k]} aria-describedby={errors[k] ? k + '-err' : undefined} />
      {errors[k] && <em id={k + '-err'} role="alert">{errors[k]}</em>}</label>);
  return (<main className="auth"><aside className="auth-side"><Link to="/" className="brand">DigitalTimeline</Link>
      <h2 className="display">{isRegister ? 'Start with one honest timeline.' : 'Welcome back to your timeline.'}</h2></aside>
    <form onSubmit={submit} noValidate className="auth-form">
      <p className="eyebrow">{isRegister ? 'Create account' : 'Log in'}</p>
      {isRegister && field('name', 'Display name', 'text', 'name')}
      {field('email', 'Email', 'email', 'email')}
      {field('password', 'Password', 'password', isRegister ? 'new-password' : 'current-password')}
      {serverError && <p className="form-error" role="alert">{serverError}</p>}
      <button className="btn" disabled={busy}>{busy ? 'Please wait…' : isRegister ? 'Create account' : 'Log in'}</button>
      <p className="muted">{isRegister ? <>Already registered? <Link to="/login">Log in</Link></> : <>New here? <Link to="/register">Create an account</Link></>}</p>
    </form></main>);
}
