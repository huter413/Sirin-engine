# Şirin Engine project format

A Şirin Engine project is a directory or ZIP package whose identity is defined by `project.sr`.

## Required

- `project.sr`
- The entry file named by `entry` inside `project.sr`

The recommended project layout is:

```text
MyProject/
├── project.sr
├── project_icon.png
├── scenes/
├── assets/
├── scripts/
├── worlds/
├── ui/
└── settings/
```

## project.sr

`project.sr` uses UTF-8 JSON syntax so it can be edited with a normal text editor while keeping a distinct Şirin Engine project extension.

Example:

```json
{
  "format": 1,
  "engine": "Sirin Engine",
  "engine_version": "1.0.0",
  "project_name": "My Project",
  "entry": "main.html",
  "editor": "3d",
  "platforms": ["android", "windows"]
}
```

## Editor modes

The project format supports the planned editor families:

- `1d`: timelines and linear data
- `2d`: interface, sprites and touch controls
- `3d`: worlds, voxel scenes, models, cameras and lights
- `4d`: 3D scenes evaluated over time

## Compatibility

The repository still contains the existing `project.json` based sample/build flow. New Şirin Engine project tooling should treat `project.sr` as the canonical project manifest and can generate a compatibility `project.json` when an older build path requires it.
