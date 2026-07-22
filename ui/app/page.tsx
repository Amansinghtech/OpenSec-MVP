'use client';

import { useState } from 'react';
import { login } from '@/lib/api';

export default function DashboardPage() {
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('');
  const [token, setToken] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function handleLogin() {
    try {
      setError(null);
      const res = await login(username, password);
      setToken(res.accessToken);
      localStorage.setItem('opensec_token', res.accessToken);
    } catch {
      setError('Login failed');
    }
  }

  return (
    <div>
      <h1>Command & Control</h1>
      <p>Operator console for alerts, sessions, detections, and agent fleet health.</p>
      <div className="card">
        <h2>Login</h2>
        <input value={username} onChange={(e) => setUsername(e.target.value)} placeholder="username" />
        <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="password" />
        <button onClick={handleLogin}>Sign in</button>
        {error && <p style={{ color: '#f87171' }}>{error}</p>}
        {token && <p style={{ color: '#4ade80' }}>Authenticated — token stored in localStorage.</p>}
      </div>
      <div className="grid">
        <div className="card"><h3>Alerts</h3><p>View and acknowledge security alerts.</p><a href="/alerts">Open →</a></div>
        <div className="card"><h3>Agents</h3><p>Monitor enrolled Wazuh agent fleet.</p><a href="/agents">Open →</a></div>
        <div className="card"><h3>Search</h3><p>Timeline search via OpenSearch API.</p></div>
      </div>
    </div>
  );
}
