/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  experimental: {
    typedRoutes: false
  },
  async rewrites() {
    const backend = process.env.BACKEND_PROXY_TARGET;
    if (!backend) return [];
    // In dev we proxy /api/* to the Spring Boot backend so MSW bypasses
    // unmocked domains hit it without CORS. Set BACKEND_PROXY_TARGET
    // (e.g. http://localhost:8080) when running Phase 1+ flows.
    return [
      { source: "/api/:path*", destination: `${backend}/api/:path*` },
      { source: "/.well-known/:path*", destination: `${backend}/.well-known/:path*` }
    ];
  }
};

export default nextConfig;
