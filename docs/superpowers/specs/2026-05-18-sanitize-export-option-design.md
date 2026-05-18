# Sanitize Export Option Design

## Goal

Make GPX sanitization available for both split and merge exports without applying it by default. Sanitization means preserving point order, coordinates, and elevation while removing all point `<time>` tags. It must not simplify tracks or remove points.

## Current Behavior

`GpxWriter.write()` currently omits timestamps from every export. Split exports, split ZIP exports, and merged exports are therefore always sanitized. Merge UI also warns that timestamps are always removed.

## Approach

Add an explicit writer/export option and pass it from the UI to export creation.

`GpxWriter.write(document, sanitize = false)` will be the central API:

- `sanitize = false` preserves timestamps, point order, coordinates, elevation, document name, and track names.
- `sanitize = true` preserves point order, coordinates, elevation, document name, and track names, but omits all point `<time>` tags.
- Both modes write normalized GPX 1.1 output with the standard `http://www.topografix.com/GPX/1/1` namespace.

Split and merge domain logic stays unchanged. Preview state remains based on the parsed and processed `GpxDocument`; only export bytes differ.

## UI

Add one unchecked Material 3 option near the export actions in each workflow: `Remove time tags from export`.

In split mode, the option applies to both `Share GPX files` and `Share ZIP`.

In merge mode, the option applies to `Share merged GPX`.

The merge timestamp warning will be replaced with neutral helper text near the option, because timestamps are no longer always removed.

## Data Flow

The UI owns two Boolean states: one for split export sanitization and one for merge export sanitization. Export callbacks receive the selected value and pass it to `ExportBuilder`.

`ExportBuilder.gpxFiles(...)` and `ExportBuilder.mergedGpxFile(...)` pass the value into `GpxWriter.write(...)`. ZIP export continues to zip the generated `ExportFile` objects, so it inherits the same split sanitize setting.

## Error Handling

No new runtime error states are required. Sanitization is a deterministic writer option. Existing share/export failure handling remains unchanged.

## Testing

Add or update JVM tests to cover:

- default writer/export behavior preserves `<time>` tags;
- sanitized writer/export behavior removes all `<time>` tags;
- coordinates and elevation remain present in sanitized output;
- split ZIP export uses the selected sanitize setting;
- merge export uses the selected sanitize setting;
- helper text no longer claims merged exports always remove timestamps.
