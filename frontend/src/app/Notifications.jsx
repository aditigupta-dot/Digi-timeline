import { api } from '../api.js';
import { useLive } from './Live.jsx';
import { Empty, Skeleton, fmt, useLoad, useToast } from './ui.jsx';
export default function Notifications() {
  const { data, loading, error, reload } = useLoad('/notifications'); const { refreshUnread } = useLive(); const toast = useToast();
  const readAll = () => api('/notifications/read-all', { method: 'POST' }).then(() => { reload(); refreshUnread(); }).catch((e) => toast(e.message, 'err'));
  if (loading && !data) return <Skeleton />; if (error) return <p className="form-error" role="alert">{error}</p>;
  return (<><p className="eyebrow">UPDATES</p><h1 className="display sm">Notifications</h1>
    {data.unread > 0 && <div className="bar"><button className="btn small ghost" onClick={readAll}>Mark all read</button></div>}
    {!data.items.length ? <Empty title="Nothing yet">You'll see a note here when a sync imports new activity or fails.</Empty> :
      <ul className="rows">{data.items.map((n) => <li key={n.id}><span style={{ fontWeight: n.read ? 400 : 600 }}>{n.message}</span><span className="mono">{fmt(n.createdAt)}</span></li>)}</ul>}</>);
}
