# Compose Workbench

A small Android starter app for previewing a Compose screen and changing a few properties.

## Build on GitHub Actions
1. Create a GitHub repository and upload these files.
2. Push to `main` or run **Actions → Android APK → Run workflow**.
3. Download the `compose-workbench-debug-apk` artifact.

## Local build
Install JDK 17 and Android SDK, then run `gradle assembleDebug` from the repository root.

## Notes
- This is a starter, not a full visual IDE. The app preview and property controls are real; the Kotlin source field is a text editor and does not compile code dynamically.
- GitHub Actions builds a debug APK, suitable for testing. For release distribution, configure signing secrets and a release workflow.
