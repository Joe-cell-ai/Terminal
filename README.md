# Terminal

A lightweight Android terminal app targeting a Debian ARM64 userspace through PRoot. It is designed to run without root access.

## Current development status

**Batch 1 — app foundation and Classic UI** is being built on `main`.

- Jetpack Compose Android app foundation
- Home, Terminal, Files, Packages, and Settings navigation
- Classic black-and-green terminal appearance by default
- Debian environment status clearly shown as not installed
- Debian root filesystem is planned as a separate download to keep the APK small

The terminal screen in this batch is a UI preview only. It does not execute shell commands yet. PRoot/rootfs setup and a real PTY-backed interactive shell are later batches.

## Build

Requires JDK 17 and Android SDK 35. Use Android Studio or Gradle 8.9+:

```bash
gradle assembleDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Roadmap

See [docs/ROADMAP.md](docs/ROADMAP.md).
