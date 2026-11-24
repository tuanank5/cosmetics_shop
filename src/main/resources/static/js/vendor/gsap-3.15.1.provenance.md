# GSAP Core 3.15.1

- Upstream: https://github.com/greensock/GSAP/tree/3.15.1
- Upstream distribution path: `dist/gsap.min.js`.
- Received from owner: `D:\Downloads\gsap.min.js`, 22 November 2026.
- Owner confirmed official download/version and supplied expected SHA-256. The agent verified the local bytes against that value; it did not independently re-download the artifact or verify npm tarball integrity in this network-restricted environment.
- File: `gsap-3.15.1.min.js`, unchanged, 73,357 bytes.
- SHA-256: `3D1BEA374AB1B703CC5C4105358EEC63C4465EFDC573931DC116E34A0A190C1B`.
- License: Standard "No Charge" GSAP License, https://gsap.com/standard-license/ (redirects to https://gsap.com/community/standard-license/).
- Original copyright/license/author header retained byte for byte. GSAP is not MIT-licensed. Consult the linked license for the full terms, permitted website use, restrictions and notice obligations.
- Only the core browser distribution is included. CSSPlugin ships inside this official core distribution; no additional plugin files are loaded or registered by BeautyShop.
- Runtime: local Home-only script. No CDN, npm runtime dependency or bundler.

Measured with Node v26.13.0, zlib 1.3.2.1-motley-456ae73 and Brotli 1.2.0: raw 73,357 bytes; gzip level 9: 28,511 bytes; Brotli quality 11: 25,842 bytes. These are offline compressed sizes, not assertions about deployment HTTP Content-Encoding.

See `docs/reviews/gsap-c19-pilot.md` for adapter sizes, test evidence, limitations and rollback.
