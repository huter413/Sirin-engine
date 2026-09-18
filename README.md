# Sirin Engine

Sirin Engine is a Godot-based Android starter project focused on a landscape mobile editor interface.

## Included

- Forced landscape orientation
- Touch-friendly controls
- ZIP project workflow UI
- Android APK export configuration
- GitHub Actions APK build
- ARM64 Android target

## Build

Open **Actions → Build Sirin Engine APK → Run workflow** in GitHub. The generated APK is uploaded as the workflow artifact.

## Safety/performance

The project uses Godot's mobile Compatibility renderer. It does not intentionally run a destructive CPU/GPU stress loop or force the phone to overheat. Graphics quality can be increased later through explicit engine settings.
