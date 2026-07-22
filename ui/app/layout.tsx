import type { ReactNode } from 'react';
import './globals.css';

export const metadata = {
  title: 'OPENSEC Command & Control',
  description: 'Autonomous Security Operations Engine',
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="en">
      <body>
        <header className="header">
          <strong>OPENSEC</strong>
          <nav>
            <a href="/">Dashboard</a>
            <a href="/alerts">Alerts</a>
            <a href="/agents">Agents</a>
          </nav>
        </header>
        <main className="main">{children}</main>
      </body>
    </html>
  );
}
