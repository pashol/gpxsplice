# GPX Merge Design

## Goal

Extend GPX Splice with a merge workflow inspired by GPXto GPX Merge. The first version lets users select multiple GPX files, review and optionally reorder them, merge their tracks into one GPX document, preview the result, and share one merged `.gpx` file. Processing remains local on the Android device.

## Scope

In scope:

- Select 2 or more GPX files with Android multi-select.
- Parse selected files locally with the existing GPX reader.
- Preserve each input file's existing tracks and segments in the merged output.
- Auto-sort files chronologically only when every selected file has at least one track-point timestamp.
- Let users manually move selected files up or down before merging.
- Preview the merged route using the existing offline preview behavior where practical.
- Export one merged `.gpx` file through the Android share sheet.

Out of scope for v1:

- Waypoint and route preservation, because the current domain model only represents tracks, segments, and points.
- Joining all files into a single track or single segment.
- Deduplicating overlapping points, interpolating points, or repairing gaps.
- ZIP export for merged output.
- Interactive map tiles or online processing.

## Architecture

Add merge as a second workflow inside the existing single Compose app. `GpxSplitApp` remains the shared shell with the app bar, intro, and a top-level Split/Merge selector. The current split UI can move into a private split workflow composable so the shell can show either split controls or merge controls.

`MainActivity` continues to own Android-specific work: file picking, URI streams, background parsing, export file creation, and share intents. It adds an `OpenMultipleDocuments` launcher for merge input while keeping the current single-file picker for split input.

Merge logic belongs in a pure Kotlin domain object, `GpxMerger`, similar to `GpxSplitter`. It accepts ordered `GpxDocument` values and returns one `GpxDocument`. The merge algorithm appends all source tracks in input order without modifying each track's segment structure.

The merge UI should use a small state model that carries the parsed document plus file metadata, such as display filename, point count, and earliest timestamp. This keeps UI ordering and timestamp messages separate from the domain merger.

## Merge Behavior

The merge workflow starts with a `Choose GPX files` action. The Android picker must allow selecting multiple files. Merge and export actions are disabled until at least 2 files are selected and parsed successfully.

After import, the UI shows selected files in their current merge order. Each row shows the filename and a concise summary such as track-point count. Move up and move down controls allow optional manual rearranging. The default order is the picker order unless chronological sorting applies.

Chronological sorting is automatic only when every selected file has at least one timestamp. The sort key is each file's earliest available track-point timestamp. If any selected file has no timestamp, the selected order is preserved and the UI shows a non-error message such as `Some files have no timestamps, so selected order is preserved.` Manual rearranging remains available in both cases.

The merged document uses a generated name such as `Merged GPX`. Its tracks are the concatenation of all source tracks in the selected order. Track names, segments, points, elevation values, and timestamp strings already represented by the current model are preserved. The merger does not combine tracks, flatten segments, or infer missing metadata.

## UI And Data Flow

The shared app shell contains:

- Existing Material 3 app bar and intro text.
- A Split/Merge workflow selector.
- The existing split controls when Split is selected.
- New merge controls when Merge is selected.

Merge data flow:

1. User taps `Choose GPX files`.
2. `MainActivity` launches `OpenMultipleDocuments`.
3. Each returned URI is opened and parsed off the UI thread with `GpxReader`.
4. Parsed merge items are returned to Compose in picker order.
5. Compose applies chronological sorting if every item has a timestamp; otherwise it keeps picker order and shows the timestamp message.
6. User optionally moves files up or down.
7. User presses `Merge`; the app runs `GpxMerger.merge` off the UI thread.
8. The merged result shows a point/file summary and offline preview.
9. User taps `Share merged GPX` to export one `.gpx` file.

For preview, reuse `TrackPreviewCanvas` rather than introducing a new renderer. The implementation can either adapt the canvas to accept document-like preview input or wrap the merged document in a lightweight preview result. The preview should preserve the existing coordinate normalization and colored route drawing behavior.

## Export

Merged export produces one GPX file. `GpxWriter` already writes multi-track documents, so export can reuse it. Add a filename helper for merged exports, for example:

- `<first-input-base>-merged.gpx` when a first input filename is available.
- `merged.gpx` when no usable input filename is available.

The share intent uses `ACTION_SEND` with GPX MIME type and the existing `FileProvider` cache export path. Share failures show `Could not share merged GPX`.

## Error Handling

Error handling follows the existing split flow: background work, concise user-facing messages, and no app crash for invalid input.

Merge-specific cases:

- Fewer than 2 files selected: show that at least 2 GPX files are required.
- A selected file cannot be opened or parsed: fail the import batch and include the display filename when available.
- No selected file contains track points: disable merge/export and show a useful message.
- Some files lack timestamps: preserve selected order and show an informational message, not an error.
- Share failure: show `Could not share merged GPX`.

## Testing

Add local unit tests in the existing style.

Domain tests:

- `GpxMerger` appends tracks in input order.
- Empty input and single-document input are rejected.
- Track segment and point metadata represented by the model are preserved.

Ordering tests:

- Automatic chronological sorting uses the earliest timestamp from each file.
- Sorting applies only when all files have timestamps.
- Missing timestamps preserve selected order.
- Manual move up/down changes order predictably.

I/O and export tests:

- A merged GPX writes and parses back through `GpxWriter` and `GpxReader`.
- Merged export filename generation handles normal filenames, extensionless filenames, and missing filenames.

UI helper tests:

- Merge is enabled only with at least 2 parsed files containing track points.
- Timestamp message appears only when chronological sort is skipped because of missing timestamps.

## Success Criteria

- Users can select 2 or more GPX files in one picker action.
- Files are merged locally without network access.
- Source tracks remain separate and ordered according to automatic sorting or manual reordering.
- The merged GPX can be shared as one `.gpx` file and parsed again by the app.
- Existing split behavior and tests continue to pass.
