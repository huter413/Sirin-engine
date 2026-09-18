# Sirin Engine

Native Android mobile project-to-APK tool. **No Godot project skeleton.**

## App flow

1. Open a Sirin ZIP.
2. The ZIP is unpacked locally.
3. Use **2D Editor** or **3D Editor**.
4. Tap **▶ Oyun Oyna** to test the project.
5. JavaScript, console, WebView and HTTP errors are collected in **⚠ Hata Paneli**.
6. Tap **🚀 APK Derle**. The ZIP is uploaded to the configured GitHub repository and GitHub Actions builds the release APK.
7. After a successful build, Android's document saver opens with the APK filename ready. On modern Android, broad storage permission is not needed for this system picker. Save the file under **Download**.

## Project ZIP format

Required:
- project.json
- main.html

Optional:
- scene.json
- icon.png
- icon.svg
- other assets

The build reads the icon declared by project.json first, then icon.png, then icon.svg, and finally uses the built-in Sirin icon.

## Included sample

The repository contains **samples/3d-racing**, a touch-controlled WebGL 3D racing sample with traffic and N2O. The matching ZIP is supplied separately.

## Build

GitHub Actions workflow: **Build Sirin Engine APK**.
