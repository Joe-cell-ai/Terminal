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
- [x] PTY-backed interactive session integrated with the Termux terminal engine
- [x] ANSI parsing, cursor handling, terminal resize and scrollback provided by the terminal engine
- [ ] Connect PTY session to Debian through a verified Android ARM64 PRoot runtime (current session is explicitly labelled Android host shell)
- [ ] Fira Code font asset and ligature-enabled rendering
- [ ] Dedicated extra-key row and terminal input ergonomics

## Batch 4 — packages and utilities
- [x] Package manager screen with searchable developer package catalogue
- [x] Safe APT argument builder with package-name validation (no shell interpolation)
- [ ] Execute APT update/install/remove/upgrade inside Debian PRoot
- [ ] Stream package output, progress, cancellation and error handling

## Batch 5 — files
- [x] Debian home/rootfs file browser with directory navigation and metadata
- [x] Android Storage Access Framework import/export without broad storage permission
- [ ] File create/rename/delete and preview/edit support

## Batch 6 — developer tools
- [x] Optional Python, Git, Node.js, SSH, Nano, Vim and build-essential catalogue
- [x] Debian APT install command guidance for each tool
- [ ] One-tap installation and package-state detection after PRoot/APT execution is available

## Batch 7 — sessions and customization
- [x] Terminal session tab controls (up to four visible slots; independent persistent PTY sessions still need implementation)
- [x] Persist Classic/Premium theme selection
- [x] Persist terminal font size and scrollback preferences

## Batch 8 — reliability and security
- [x] Preserve coroutine cancellation and clean incomplete Debian staging on failure
- [x] Debian install storage preflight and app-private rootfs storage
- [ ] Persistent background install recovery and process/session cleanup
- [ ] Runtime isolation, symlink/archive edge-case review and permission audit

## Batch 9 — release readiness
- [ ] Unit/instrumentation tests and ARM64 device testing
- [ ] Debug/release APKs, signing, and install instructions
