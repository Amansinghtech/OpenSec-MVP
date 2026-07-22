'use client';

import { useEffect, useState } from 'react';
import { apiGet } from '@/lib/api';

type Alert = {
  id: string;
  title: string;
  riskScore: number;
  severity: string;
  status: string;
};

export default function AlertsPage() {
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const token = localStorage.getItem('opensec_token');
    if (!token) {
      setError('Login required — go to Dashboard first.');
      return;
    }
    apiGet<Alert[]>('/alerts', token)
      .then(setAlerts)
      .catch(() => setError('Failed to load alerts'));
  }, []);

  return (
    <div>
      <h1>Alerts</h1>
      {error && <p style={{ color: '#f87171' }}>{error}</p>}
      <table>
        <thead>
          <tr><th>Title</th><th>Risk</th><th>Severity</th><th>Status</th></tr>
        </thead>
        <tbody>
          {alerts.map((a) => (
            <tr key={a.id}>
              <td>{a.title}</td>
              <td>{a.riskScore}</td>
              <td>{a.severity}</td>
              <td>{a.status}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
