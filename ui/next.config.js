/** @type {import('next').NextConfig} */
const nextConfig = {
  async rewrites() {
    return [
      {
        source: '/api/:path*',
        destination: `${process.env.OPENSEC_API_URL || 'http://localhost:8001/api/v1'}/:path*`,
      },
    ];
  },
};

module.exports = nextConfig;
