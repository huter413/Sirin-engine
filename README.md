# Sirin Engine

Sirin Engine is a native Android mobile project manager. It is **not a Godot project**.

## Current build

- Native Android app
- Forced landscape orientation
- Touch-friendly controls
- ZIP project picker
- Project validation entry point
- APK build entry point
- Hardware acceleration
- Screen kept active while the app is open
- ARM64-compatible Android build configuration

## APK

GitHub Actions builds the app automatically on pushes to `main` and also supports manual workflow runs. The APK is published as the `SirinEngine-APK` workflow artifact.

## Important

A normal Android APK cannot safely compile every arbitrary ZIP project entirely by itself. A real compiler/build environment is required. Sirin Engine therefore provides the mobile project interface while the actual Android APK build runs in the CI build environment.

The app does not deliberately run a destructive CPU/GPU stress loop or force the phone to overheat.
