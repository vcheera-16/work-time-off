# Notes: web.ignoring() for static assets and H2 console

This file adds a WebSecurityCustomizer to remove static asset paths and the H2 console from
Spring Security's filter chain in development. This avoids MvcRequestMatcher ambiguities when
multiple servlets (DispatcherServlet and the H2 servlet) are present.

SecurityImplications:
- This is intended for local development only. Do NOT enable the H2 console in production.
- Static assets are typically safe to ignore in dev when served from the same origin. In production,
  serve static assets via a CDN or reverse-proxy and keep strict security rules in place.
