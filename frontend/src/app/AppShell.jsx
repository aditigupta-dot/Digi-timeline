import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { AnimatePresence, motion } from 'framer-motion';
import { BarChart3, Bell, CheckSquare, FolderKanban, LayoutDashboard, LogOut, MessageSquare, Plug, Rows3, Settings as SettingsIcon } from 'lucide-react';
import { useAuth } from '../auth/AuthContext.jsx';
import { LiveProvider, useLive } from './Live.jsx';
import { ToastProvider } from './ui.jsx';
const LINKS = [['/app', 'Dashboard', LayoutDashboard, true], ['/app/timeline', 'Timeline', Rows3], ['/app/tasks', 'Tasks', CheckSquare], ['/app/projects', 'Projects', FolderKanban],
  ['/app/assistant', 'Assistant', MessageSquare], ['/app/analytics', 'Analytics', BarChart3], ['/app/integrations', 'Integrations', Plug], ['/app/settings', 'Settings', SettingsIcon]];
function Shell() {
  const { logout } = useAuth(); const loc = useLocation(); const { unread } = useLive();
  return (<div className="shell">
    <nav className="side" aria-label="Main"><span className="brand">DigitalTimeline</span>
      {LINKS.map(([to, label, Icon, end]) => <NavLink key={to} to={to} end={end}><Icon size={17} />{label}</NavLink>)}
      <NavLink to="/app/notifications"><Bell size={17} />Notifications{unread > 0 && <span className="badge" aria-label={`${unread} unread`}>{unread}</span>}</NavLink>
      <button className="linkbtn" onClick={logout}><LogOut size={17} />Log out</button></nav>
    <AnimatePresence mode="wait"><motion.main key={loc.pathname} className="main" initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }} transition={{ duration: 0.18 }}><Outlet /></motion.main></AnimatePresence></div>);
}
export default function AppShell() { return <ToastProvider><LiveProvider><Shell /></LiveProvider></ToastProvider>; }
