# Debian ARM64 runtime

Batch 2 installs a Debian Bookworm ARM64 userspace separately from the APK.

## Source and verification

- Image source: Linux Containers image server, Debian Bookworm / ARM64 / default.
- The installer discovers the newest published image directory.
- It downloads `SHA256SUMS` and `rootfs.tar.xz` over HTTPS.
- The downloaded archive is accepted only when its SHA-256 matches the checksum listed by that image directory.
- The root filesystem is extracted into app-private internal storage at `files/debian`.
- Extraction rejects paths that escape the destination and rejects unsupported special files.

## Storage

The installer requires at least 1.5 GB of free internal storage before starting. The archive is downloaded into the app cache and removed after a successful installation. The Debian filesystem is not bundled in the APK.

## Runtime boundary

This batch installs Debian userspace files only. It does not provide a Linux kernel, root access, or a working shell. PRoot executable setup and launching the interactive shell are handled separately from the rootfs download and must be completed before the terminal can execute commands.


## PRoot launch contract

The terminal now looks for a separately supplied executable at
`files/runtime/proot`. Before using it, the app checks that:

- the executable is inside the app-private runtime directory;
- it is an executable ARM64 ELF binary (AArch64 machine type);
- the Debian rootfs contains `/etc/debian_version`.

When both checks pass, the terminal starts PRoot with the Debian rootfs, binds
`/dev`, `/proc`, and `/sys`, and launches `/bin/bash --login` with a clean
Debian environment. If either component is absent, the terminal remains on the
explicitly labelled Android host-shell fallback.

**Important:** this commit adds the launch integration, not the PRoot executable
or its installer. No binary is accepted from an unverified mirror. A reproducible,
license-compliant ARM64 PRoot build/package and its integrity verification are
still required before Debian Bash can start on a device.


## Reproducible ARM64 PRoot build

The workflow `.github/workflows/proot-runtime.yml` builds the official Termux
PRoot package for AArch64 from a pinned `termux/termux-packages` commit
(`8d52c3057bb01e89a1fb4ed0592b7602dce38731`). It uses the upstream package
recipe (PRoot GPL-2.0) and its declared `libandroid-shmem` and `libtalloc`
dependencies, then assembles an ARM64 runtime archive with the PRoot loader,
shared libraries, build provenance and SHA-256 checksums.

The workflow fails closed if the PRoot executable, unbundled loader, or either
required shared library is missing, or if the executable is not identified as
ARM64. The archive is a CI artifact for validation; it is not yet automatically
downloaded or installed by the Android app. Device testing and the runtime
installer remain required before enabling APT.
