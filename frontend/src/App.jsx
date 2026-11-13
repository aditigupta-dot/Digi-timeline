import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth/AuthContext.jsx';
import Landing from './landing/Landing.jsx';
import AuthPage from './pages/AuthPage.jsx';
import AppShell from './app/AppShell.jsx';
import Dashboard from './app/Dashboard.jsx';
import Timeline from './app/Timeline.jsx';
import Tasks from './app/Tasks.jsx';
import Projects from './app/Projects.jsx';
import Integrations from './app/Integrations.jsx';
import Analytics from './app/Analytics.jsx';
import Assistant from './app/Assistant.jsx';
import Notifications from './app/Notifications.jsx';
import Settings from './app/Settings.jsx';
function Protected({ children }) {
  const { status } = useAuth();
  if (status === 'loading') return <p className="center mono">Checking session…</p>;
  return status === 'in' ? children : <Navigate to="/login" replace />;
}
export default function App() {
  return (<Routes>
    <Route path="/" element={<Landing />} />
    <Route path="/login" element={<AuthPage mode="login" />} />
    <Route path="/register" element={<AuthPage mode="register" />} />
    <Route path="/app" element={<Protected><AppShell /></Protected>}>
      <Route index element={<Dashboard />} /><Route path="timeline" element={<Timeline />} />
      <Route path="tasks" element={<Tasks />} /><Route path="projects" element={<Projects />} /><Route path="integrations" element={<Integrations />} /><Route path="analytics" element={<Analytics />} /><Route path="assistant" element={<Assistant />} /><Route path="notifications" element={<Notifications />} /><Route path="settings" element={<Settings />} /></Route>
    <Route path="*" element={<Navigate to="/" replace />} />
  </Routes>);
}
