# GPX Split Android App Design

## Goal

Build an installable Android app that matches the core GPXto GPX Split workflow: choose a GPX file, configure a split method, preview the resulting parts, and export GPX files or a ZIP. The app must process GPX data locally on the Android device. Map tiles are not required for offline use; the first version will use a simple offline preview.

## Target Platform

- Native Android app written in Kotlin.
- Single-activity app using Jetpack Compose.
- Offline splitting and exporting.
- Android share sheet for output.

## Feature Scope

The first version includes:

- GPX file selection through the Android file picker.
- Splitting by distance interval.
- Splitting by maximum number of points per output file.
- Splitting by number of equal stages.
- Simple offline track preview with colored split segments.
- Export of individual GPX files.
- Export of all split GPX files as one ZIP.

The first version focuses on track-based GPX files using `trk`, `trkseg`, and `trkpt`. Waypoint-only and route-only files are not primary targets.

## Architecture

The app will use a small layered structure:

- UI layer: Compose screens for upload, configuration, preview, and export.
- GPX domain layer: pure Kotlin models and splitting logic.
- File layer: Android document picker, temporary output files, and share-sheet export.
- Preview layer: Compose canvas drawing based on latitude/longitude bounds.

The splitting engine will not depend on Android APIs. This keeps GPX math and file generation testable with local unit tests.

## Components

- `MainActivity`: hosts the Compose app and Android file/share integrations.
- `GpxPickerScreen`: lets the user choose a GPX file and shows parsed file information.
- `SplitConfigScreen`: lets the user choose distance, max-points, or equal-stage splitting.
- `PreviewScreen`: shows split summaries and an offline visual track preview.
- `ExportScreen`: exposes share actions for individual GPX files or ZIP export.
- `GpxParser`: converts GPX XML into the app model.
- `GpxSplitter`: implements all split modes.
- `GpxWriter`: writes valid GPX XML for generated split results.
- `ZipExporter`: packages multiple GPX files into one ZIP.
- `TrackPreviewCanvas`: draws route lines and split colors from track coordinates.

Core domain models:

- `GpxDocument`
- `Track`
- `TrackSegment`
- `TrackPoint`
- `SplitResult`

## Library Choice

Existing libraries will be evaluated before implementing parser/writer internals.

Preferred candidate:

- `me.bvn13.sdk.android.gpx:GpxAndroidSdk`, because it is Kotlin/Android-oriented, supports GPX 1.1, reads and writes, has no external dependencies, and uses Apache-2.0.
- Use the latest stable Maven Central version available during implementation.

Fallback:

- If `GpxAndroidSdk` does not build cleanly or cannot support the needed track parsing/writing behavior, implement a small custom GPX parser/writer using Android/Kotlin XML APIs.

Rejected or lower-priority candidates:

- `me.himanshusoni.gpxparser:gpx-parser:1.13`, because the GitHub project is GPL-2.0 and older.
- `JazzyLazzy/Simple_GPX_Parser`, because it has low adoption and unclear Maven availability despite an MIT license.

The app will keep the split logic in our own code regardless of parser/writer choice.

## Data Flow

1. Android file picker returns a GPX document URI.
2. The app reads the URI stream into the GPX parser.
3. The parser produces a normalized in-memory track model.
4. The splitter flattens trackpoints in document order while preserving enough metadata for valid output.
5. The selected split mode produces `SplitResult` objects with point ranges, distance, point count, and display color.
6. The preview screen draws split results on a Compose canvas.
7. Export converts each `SplitResult` back to GPX XML.
8. The share sheet exports selected `.gpx` files or one `.zip` file.

## Split Behavior

Distance splitting:

- Uses haversine distance between adjacent trackpoints.
- Starts a new output when the configured interval is reached or exceeded.
- Allows the split boundary to fall on the nearest available trackpoint; v1 will not interpolate synthetic points.

Max-points splitting:

- Produces files with no more than the configured point count, except that a valid output must contain at least one point.
- Requires a max-points value of at least 1.

Equal-stage splitting:

- Divides the total ordered trackpoint list into the configured number of nearly equal point-count stages.
- This is not date-based or timestamp-based in v1.
- Requires a stage count from 1 through the total number of trackpoints.

Metadata:

- Preserve file and track metadata where practical.
- Preserve point latitude, longitude, elevation, and time when present.
- Omit absent fields rather than generating placeholders.
- Prioritize valid GPX output over byte-for-byte preservation.

## Error Handling

- Invalid or unreadable file: show a clear read error.
- No trackpoints found: tell the user no track points were found.
- Invalid split input: disable splitting and export until the input is valid.
- Very small files: allow export if at least one trackpoint exists.
- Excessive output count: warn before generating more than 100 output files.
- Multiple tracks and segments: process trackpoints in document order.
- Missing elevation/time: preserve existing values and omit missing values.
- Sharing failure: keep generated temporary files during the session and allow retry.
- Large files: parse and split off the UI thread and show loading state.

## Testing

Automated tests will focus on domain correctness:

- Haversine distance calculation.
- Distance splitting exact-boundary and over-boundary cases.
- Max-points splitting.
- Equal-stage splitting.
- Parser/writer round-trip with small GPX fixtures.
- Missing elevation and time fields.
- Multiple tracks and segments.
- Exported GPX can be parsed again.

Manual Android verification:

- Install a debug APK.
- Pick a real `.gpx` file from device storage.
- Split by each method.
- Confirm the preview renders.
- Share/export individual files and ZIP.
- Re-open exported GPX in the app to confirm it parses.

## Non-Goals For V1

- Full interactive tiled map.
- Server-side processing.
- Route-only or waypoint-only splitting.
- Date/day-based splitting.
- Synthetic interpolation points at exact distance boundaries.
- Byte-for-byte metadata preservation.
