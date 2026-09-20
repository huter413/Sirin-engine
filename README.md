# Şirin Engine

Native Android mobile project-to-APK tool and the home of the Şirin Engine project format.

## Current app flow

1. Open a Şirin project ZIP.
2. The project is unpacked locally.
3. Use the 1D, 2D, 3D or 4D editor mode supported by the project.
4. Run the project for testing.
5. Inspect JavaScript, console, WebView and HTTP errors in the error panel.
6. Build an Android APK.
7. GitHub Actions can store the generated APK as a workflow artifact.

GitHub Actions is the build runner. The repository contains the Şirin Engine project format and build integration rather than a copied Godot or Unity game project.

## Project format

New Şirin Engine projects use project.sr as the canonical manifest.
The project package should contain project.sr and the entry file declared by it, with optional scenes, assets, scripts, worlds, ui and settings folders.
The racing sample also keeps project.json as a compatibility manifest for the existing build path.

## Editors

- 1D — timelines and linear data
- 2D — UI, sprites and touch controls
- 3D — voxel/world scenes, models, cameras and lighting
- 4D — time-aware 3D scene editing

## Separate projects

The engine repository contains the editor/runtime and Android build pipeline. Example game projects are distributed separately and are not bundled into the engine repository.

## Engine architecture

See docs/engine-architecture.svg for the visual overview and docs/project-format.md for the project.sr contract.

## Android builds

GitHub documents workflow artifacts as files produced by a workflow run, including binary build outputs. Android requires APKs to be digitally signed before installation or update, so release builds should use a controlled signing key or keystore.

## Repository

Owner: huter413
Repository: Sirin-engine