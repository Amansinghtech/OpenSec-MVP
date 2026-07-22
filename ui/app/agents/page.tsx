'use client';

import { useEffect, useState } from 'react';
import { apiGet } from '@/lib/api';

type Agent = {
  id: string;
  name: string;
  status: string;
  platform: string;
  lastHeartbeatAt: string | null;
};

export default function AgentsPage() {
  const [agents, setAgents] = useState<Agent[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const token = localStorage.getItem('opensec_token');
    if (!token) {
      setError('Login required — go to Dashboard first.');
      return;
    }
    apiGet<Agent[]>('/agents', token)
      .then(setAgents)
      .catch(() => setError('Failed to load agents'));
  }, []);

  return (
    <div>
      <h1>Agent Fleet</h1>
      {error && <p style={{ color: '#f87171' }}>{error}</p>}
      <table>
        <thead>
          <tr><th>Name</th><th>Platform</th><th>Status</th><th>Last heartbeat</th></tr>
        </thead>
        <tbody>
          {agents.map((a) => (
            <tr key={a.id}>
              <td>{a.name}</td>
              <td>{a.platform}</td>
              <td>{a.status}</td>
              <td>{a.lastHeartbeatAt ?? '—'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
