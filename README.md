# Terminal

A lightweight Android terminal app targeting a Debian ARM64 userspace through PRoot. It is designed to run without root access.

## Current development status

**Batches 1–4 are in progress on `main`.** The package manager screen now includes a searchable developer package catalogue and validated APT command construction. Actual APT execution is gated until the Debian PRoot runtime is connected.

- Jetpack Compose Android app foundation
- Home, Terminal, Files, Packages, and Settings navigation
- Classic black-and-green terminal appearance by default
- Debian environment status clearly shown as not installed
- Debian root filesystem is planned as a separate download to keep the APK small

Batch 2 adds an optional Debian Bookworm ARM64 rootfs download with SHA-256 verification and app-private extraction. The terminal currently runs the Android host shell; Debian PRoot execution remains pending. Batch 4's UI and safe command builder are ready, but package operations are not yet executable.

## Build

Requires JDK 17 and Android SDK 35. Use Android Studio or Gradle 8.9+:

```bash
gradle assembleDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Roadmap

See [docs/ROADMAP.md](docs/ROADMAP.md).
