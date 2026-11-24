# Development roadmap

## Batch 1 — foundation and UI
- [x] Compose application skeleton and Android navigation
- [x] Classic theme as the default
- [x] Home, terminal preview, Files, Packages, Settings screens
- [x] Explain runtime status honestly; no fake shell execution
- [ ] Bundle Fira Code font and wire ligatures into the terminal renderer

## Batch 2 — Debian runtime
- [x] ARM64 Debian Bookworm rootfs download outside the APK
- [x] Download progress reporting and retry after failure
- [x] SHA-256 verification against the image's published checksum
- [x] Path-checked extraction into app-private storage
- [x] Free-storage preflight and installed-state restoration
- [ ] PRoot executable/bootstrap and first shell launch (runtime execution batch)

## Batch 3 — real terminal
- [ ] PTY-backed shell process and interactive input
- [ ] ANSI colors, cursor control, resize, signals, scrollback
- [ ] Fira Code font asset and ligature-enabled rendering
- [ ] Keyboard row and terminal input ergonomics

## Batch 4 — packages and utilities
- [ ] APT update/install/remove flows
- [ ] Package output and error handling

## Batch 5 — files
- [ ] Debian home file browser
- [ ] Android Storage Access Framework integration

## Batch 6 — developer tools
- [ ] Optional Python, Git, Node.js, SSH, Nano, Vim installation guidance

## Batch 7 — sessions and customization
- [ ] Multiple terminal sessions
- [ ] Font size, theme, and scrollback settings

## Batch 8 — reliability and security
- [ ] Lifecycle/process recovery and storage validation
- [ ] Runtime isolation and permission review

## Batch 9 — release readiness
- [ ] Unit/instrumentation tests and ARM64 device testing
- [ ] Debug/release APKs, signing, and install instructions
