# GPX Splice

GPX Splice is a native Android app for splitting and merging GPX tracks.

Use it to pick GPX files from Android storage, split long tracks into stages, merge multiple tracks into one file, preview the result offline, and share clean GPX exports through the Android share sheet.

[Download the latest APK](https://github.com/pashol/gpxsplice/releases/latest)

## Features

- Pick GPX files from Android storage.
- Split tracks by distance, maximum point count, or equal stages.
- Merge multiple GPX files into one GPX file.
- Reorder merge inputs with buttons or long-press drag.
- Preview split or merged tracks offline with colored line segments.
- Share individual GPX files, a ZIP of split files, or one merged GPX file.
- Export GPX files without timestamps, so shared files do not leak original timing data.

## Install

Download the latest APK from the [GitHub releases page](https://github.com/pashol/gpxsplice/releases/latest).

To build and install a debug APK locally:

```bash
ANDROID_HOME=/home/pascal/Android/Sdk ./gradlew :app:assembleDebug
/home/pascal/Android/Sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Usage

### Split A Track

1. Open GPX Splice.
2. Pick a GPX file.
3. Choose a split mode.
4. Preview the generated stages.
5. Share the stages as individual GPX files or one ZIP archive.

| Mode | Description |
|------|-------------|
| **Distance** | Split by a target distance in kilometers. |
| **Max Points** | Split so no file exceeds a chosen number of track points. |
| **Equal Stages** | Divide the track into a fixed number of roughly equal stages. |

### Merge Tracks

1. Pick two or more GPX files.
2. Reorder the files if needed.
3. Preview the merged track.
4. Share one combined GPX file.

## Development

GPX Splice is a single-module Android project using Kotlin, Jetpack Compose, and Material 3.

| Tool | Version |
|------|---------|
| Android Gradle Plugin | 8.7.3 |
| Gradle wrapper | 8.10.2 |
| Kotlin | 2.0.21 |
| Java target | 17 |
| Min SDK | 26 |
| Target SDK | 35 |

Build the debug APK:

```bash
ANDROID_HOME=/home/pascal/Android/Sdk ./gradlew :app:assembleDebug
```

Run JVM unit tests:

```bash
ANDROID_HOME=/home/pascal/Android/Sdk ./gradlew :app:testDebugUnitTest
```

Run one test class:

```bash
ANDROID_HOME=/home/pascal/Android/Sdk ./gradlew :app:testDebugUnitTest --tests com.example.gpxsplice.domain.DistanceTest
```

Run Android lint for the debug variant:

```bash
ANDROID_HOME=/home/pascal/Android/Sdk ./gradlew :app:lintDebug
```

## Project Structure

```text
app/src/main/java/com/example/gpxsplice/
|-- MainActivity.kt           # Android entry point, file picking, sharing
|-- domain/                   # GPX models plus split and merge logic
|-- io/                       # GPX parsing, writing, ZIP and file export
`-- ui/                       # Compose UI and Material 3 theme
```

## Test Coverage

Unit tests cover:

- Domain logic for splitting and merging (`DistanceTest`, `GpxSplitterTest`, `GpxMergerTest`)
- GPX I/O (`GpxReaderWriterTest`, `ExportFilesTest`)
- UI components and merge UI state (`TrackPreviewCanvasTest`, `GpxMergeModelsTest`, `GpxSplitLayoutTest`, `AppThemeTest`)
- Activity formatting and helpers (`MainActivityTest`)

## Notes

- GPX parsing accepts namespaced and non-namespaced GPX elements.
- Exported files are shared through Android `FileProvider`.
- Exported GPX files intentionally omit `<time>` values.
