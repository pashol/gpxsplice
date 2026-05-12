# GPX Splice

Native Android GPX splitter inspired by GPXto GPX Split.

## Features

- Pick a GPX file from Android storage.
- Split tracks by distance, maximum points, or equal stages.
- Pick multiple GPX files and merge them into one GPX file.
- Preview split or merged tracks offline with colored line segments.
- Share generated GPX files, a ZIP of split files, or one merged GPX file through Android share sheet.

## Tech Stack

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose with Material 3
- **Architecture:** Single-module Android app
- **GPX Parsing:** [`me.bvn13.sdk.android.gpx:GpxAndroidSdk`](https://github.com/bvn13/GpxAndroidSdk)
- **Min SDK:** 26, **Target SDK:** 35

## Project Structure

```
app/src/main/java/com/example/gpxsplice/
├── MainActivity.kt          # Entry point, file picking and sharing
├── domain/                  # GPX models plus splitting and merging logic
│   ├── GpxModels.kt         # Data classes (GpxDocument, Track, TrackPoint, etc.)
│   ├── GpxMerger.kt         # Core merging algorithm
│   ├── GpxSplitter.kt       # Core splitting algorithm
│   └── Distance.kt          # Haversine distance calculation
├── io/                      # GPX reading, writing, and export
│   ├── GpxReader.kt         # GPX file parsing
│   ├── GpxWriter.kt         # GPX file serialization
│   └── ExportFiles.kt       # ZIP and file export utilities
├── ui/                      # Compose UI
│   ├── GpxMergeModels.kt    # Merge workflow UI state models
│   ├── GpxSplitApp.kt       # Main app UI scaffold and controls
│   ├── TrackPreviewCanvas.kt # Canvas preview of split tracks
│   └── theme/AppTheme.kt    # Material 3 theme
```

## Build

```bash
./gradlew :app:assembleDebug
```

> If Gradle cannot find the Android SDK, run with:
> ```bash
> ANDROID_HOME=/home/pascal/Android/Sdk ./gradlew :app:assembleDebug
> ```

## Test

```bash
./gradlew :app:testDebugUnitTest
```

### Test Coverage

Unit tests cover:
- Domain logic for splitting and merging (`DistanceTest`, `GpxSplitterTest`, `GpxMergerTest`)
- GPX I/O (`GpxReaderTest`, `GpxReaderWriterTest`, `ExportFilesTest`)
- UI components and merge UI state (`TrackPreviewCanvasTest`, `GpxMergeModelsTest`, `GpxSplitLayoutTest`, `AppThemeTest`)
- Error handling (`ImportErrorFormatterTest`, `MainActivityTest`)

## Splitting Modes

| Mode | Description |
|------|-------------|
| **Distance** | Split by a target distance in kilometers. Each file covers approximately that distance. |
| **Max Points** | Split so no file exceeds a given number of track points. |
| **Equal Stages** | Divide the track into a fixed number of roughly equal segments. |

## Sharing

After splitting, you can share the results as:
- **Individual GPX files** — each stage as a separate `.gpx` file
- **ZIP archive** — all stages packed into a single `.zip` file

After merging, you can share one combined `.gpx` file. Exported files are shared via the Android share sheet using `FileProvider`.
