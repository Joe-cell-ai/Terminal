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
- [x] PRoot ARM64 runtime installer with checksum verification, guarded extraction and atomic activation
- [ ] End-to-end first Debian Bash launch on physical ARM64 device

## Batch 3 — real terminal
- [x] PTY-backed interactive session integrated with the Termux terminal engine
- [x] ANSI parsing, cursor handling, terminal resize and scrollback provided by the terminal engine
- [x] PTY launch wired to the installed ARM64 PRoot runtime with honest host-shell fallback
- [ ] Confirm interactive Debian Bash on physical ARM64 device
- [ ] Fira Code font asset and ligature-enabled rendering
- [ ] Dedicated extra-key row and terminal input ergonomics

## Batch 4 — packages and utilities
- [x] Package manager screen with searchable developer package catalogue
- [x] Safe APT argument builder with package-name validation (no shell interpolation)
- [x] Execute APT update/upgrade/install/remove through PRoot with argument validation
- [x] Stream command output and show operation progress/error state (cancellation remains pending)

## Batch 5 — files
- [x] Debian home/rootfs file browser with directory navigation and metadata
- [x] Android Storage Access Framework import/export without broad storage permission
- [x] File create/rename/delete with path validation and delete confirmation
- [x] Bounded UTF-8 text preview/edit for supported text files (512 KiB limit)

## Batch 6 — developer tools
- [x] Optional Python, Git, Node.js, SSH, Nano, Vim and build-essential catalogue
- [x] Debian APT install command guidance for each tool
- [x] One-tap developer package installation through Debian APT
- [ ] Installed package-state detection

## Batch 7 — sessions and customization
- [x] Up to four independent in-memory PTY sessions while the Terminal screen remains open
- [ ] Keep PTY sessions alive across navigation/process recreation and add session rename
- [x] Persist Classic/Premium theme selection
- [x] Persist terminal font size and scrollback preferences

## Batch 8 — reliability and security
- [x] Preserve coroutine cancellation and clean incomplete Debian staging on failure
- [x] Debian install storage preflight and app-private rootfs storage
- [ ] Persistent background install recovery and explicit process/session lifecycle recovery
- [x] Reject absolute/traversal paths, duplicate entries, hard links and special archive entries; cap archive entries and expanded size
- [ ] Complete PRoot isolation review and physical-device permission audit

## Batch 9 — release readiness
- [ ] Unit/instrumentation tests and ARM64 device testing
- [ ] Debug/release APKs, signing, and install instructions
