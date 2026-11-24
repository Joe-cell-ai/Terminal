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
