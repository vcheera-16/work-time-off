# DEVNOTES: Global security ignore (REVERTED)

The global ignore (web.ignoring("/**")) was used temporarily for debugging but has been reverted.
The current configuration ignores only static assets and the H2 console so API endpoints run
through the Spring Security filter chain and CSRF tokens are generated as expected.

If you previously relied on the global ignore for local debugging, do not enable it in
production. To restore the global ignore for a short debugging session, replace
WebSecurityIgnoreConfig with the previous version, but remember to revert it afterwards.
