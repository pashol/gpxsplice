# Sanitize Export Option Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an unchecked sanitize export option for split and merge exports that removes GPX `<time>` tags only when selected.

**Architecture:** Keep split and merge domain logic unchanged. Add a `sanitize` option at the writer/export boundary, then thread the selected UI Boolean through export callbacks in `GpxSplitApp` and `MainActivity`. `GpxWriter` remains responsible for producing normalized GPX 1.1 XML.

**Tech Stack:** Kotlin, Android Jetpack Compose Material 3, JVM unit tests, Gradle wrapper with Android SDK at `/home/pascal/Android/Sdk`.

---

## File Structure

- Modify `app/src/main/java/com/example/gpxsplice/io/GpxWriter.kt`: add `sanitize` parameter and timestamp writing behavior.
- Modify `app/src/main/java/com/example/gpxsplice/io/ExportFiles.kt`: add sanitize parameters to export builders and pass them to `GpxWriter`.
- Modify `app/src/main/java/com/example/gpxsplice/ui/GpxSplitApp.kt`: add split and merge sanitize state, Material 3 checkbox UI, callback parameters, and neutral helper text.
- Modify `app/src/main/java/com/example/gpxsplice/MainActivity.kt`: receive sanitize callback values and pass them to export helpers.
- Modify `app/src/test/java/com/example/gpxsplice/io/GpxReaderWriterTest.kt`: update writer tests for default preservation and optional sanitization.
- Modify `app/src/test/java/com/example/gpxsplice/io/ExportFilesTest.kt`: cover split, ZIP input bytes, and merge export sanitize parameters at the export builder layer.
- Modify `app/src/test/java/com/example/gpxsplice/ui/GpxSplitLayoutTest.kt`: replace the merge timestamp warning test with neutral helper text coverage.
- Modify `README.md`: update the feature/note text so timestamp removal is described as optional.

The worktree currently contains unrelated uncommitted modifications in several of these files. Before editing, inspect each target file and only change the lines required for this feature. Do not revert or overwrite unrelated user changes.

---

### Task 1: Writer Option

**Files:**
- Modify: `app/src/test/java/com/example/gpxsplice/io/GpxReaderWriterTest.kt`
- Modify: `app/src/main/java/com/example/gpxsplice/io/GpxWriter.kt`

- [ ] **Step 1: Write the failing tests**

Update `writesReadableTrack()` so default writing preserves timestamps, and replace the current sanitize test body with an explicit `sanitize = true` call.

```kotlin
@Test
fun writesReadableTrack() {
    val document = GpxDocument(
        name = "Export & Verify <doc>",
        tracks = listOf(
            Track(
                name = "Part & 1 <north>",
                segments = listOf(
                    TrackSegment(
                        listOf(
                            TrackPoint(52.0, 5.0, 10.0, "2026-04-26T08:00:00Z"),
                            TrackPoint(52.1, 5.1),
                        ),
                    ),
                    TrackSegment(
                        listOf(
                            TrackPoint(52.2, 5.2, 20.5, "2026-04-26T08:10:00Z"),
                        ),
                    ),
                ),
            ),
        ),
    )

    val xml = GpxWriter.write(document)
    val reparsed = GpxReader.read(xml.byteInputStream())

    assertTrue(xml.contains("Export &amp; Verify &lt;doc&gt;"))
    assertTrue(xml.contains("Part &amp; 1 &lt;north&gt;"))
    assertTrue(xml.contains("<time>2026-04-26T08:00:00Z</time>"))
    assertEquals("Export & Verify <doc>", reparsed.name)
    assertEquals("Part & 1 <north>", reparsed.tracks.single().name)
    assertEquals(2, reparsed.tracks.single().segments.size)
    assertEquals(2, reparsed.tracks.single().segments.first().points.size)
    assertEquals(1, reparsed.tracks.single().segments.last().points.size)
    assertEquals(3, reparsed.orderedPoints().size)
    assertEquals(10.0, reparsed.orderedPoints().first().elevationMeters!!, 0.001)
    assertEquals("2026-04-26T08:00:00Z", reparsed.orderedPoints().first().time)
    assertEquals(20.5, reparsed.orderedPoints().last().elevationMeters!!, 0.001)
    assertEquals("2026-04-26T08:10:00Z", reparsed.orderedPoints().last().time)
}

@Test
fun writesSanitizedGpxWithoutTimesWhilePreservingPoints() {
    val firstPoint = TrackPoint(52.0, 5.0, elevationMeters = 10.0, time = "2026-04-26T08:00:00Z")
    val secondPoint = TrackPoint(52.1, 5.1, elevationMeters = null, time = "2026-04-26T08:05:00Z")
    val thirdPoint = TrackPoint(52.2, 5.2, elevationMeters = 20.5, time = null)
    val document = GpxDocument(
        name = "Sanitized",
        tracks = listOf(
            Track(
                name = "Ordered",
                segments = listOf(
                    TrackSegment(listOf(firstPoint, secondPoint, thirdPoint)),
                ),
            ),
        ),
    )

    val xml = GpxWriter.write(document, sanitize = true)
    val reparsed = GpxReader.read(xml.byteInputStream())

    assertTrue(xml.contains("xmlns=\"http://www.topografix.com/GPX/1/1\""))
    assertFalse(xml.contains("<time>"))
    assertEquals(listOf(52.0, 52.1, 52.2), reparsed.orderedPoints().map { it.latitude })
    assertEquals(listOf(5.0, 5.1, 5.2), reparsed.orderedPoints().map { it.longitude })
    assertEquals(listOf(10.0, null, 20.5), reparsed.orderedPoints().map { it.elevationMeters })
    assertEquals(listOf(null, null, null), reparsed.orderedPoints().map { it.time })
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `ANDROID_HOME=/home/pascal/Android/Sdk ./gradlew :app:testDebugUnitTest --tests com.example.gpxsplice.io.GpxReaderWriterTest`

Expected: FAIL because `GpxWriter.write(document, sanitize = true)` does not exist and default writing still omits `<time>` tags.

- [ ] **Step 3: Implement the writer option**

Replace `GpxWriter.write` with this implementation shape, preserving the existing `escapeXml()` helper.

```kotlin
object GpxWriter {
    fun write(document: GpxDocument, sanitize: Boolean = false): String {
        val xml = StringBuilder()
        xml.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        xml.appendLine("<gpx version=\"1.1\" creator=\"gpxsplice\" xmlns=\"http://www.topografix.com/GPX/1/1\">")

        document.name?.let { xml.appendLine("  <metadata><name>${it.escapeXml()}</name></metadata>") }

        for (track in document.tracks) {
            xml.appendLine("  <trk>")
            track.name?.let { xml.appendLine("    <name>${it.escapeXml()}</name>") }

            for (segment in track.segments) {
                xml.appendLine("    <trkseg>")

                for (point in segment.points) {
                    val time = point.time.takeUnless { sanitize }
                    xml.append("      <trkpt lat=\"${point.latitude}\" lon=\"${point.longitude}\"")
                    if (point.elevationMeters == null && time == null) {
                        xml.appendLine(" />")
                    } else {
                        xml.append('>')
                        point.elevationMeters?.let { xml.append("<ele>$it</ele>") }
                        time?.let { xml.append("<time>${it.escapeXml()}</time>") }
                        xml.appendLine("</trkpt>")
                    }
                }

                xml.appendLine("    </trkseg>")
            }

            xml.appendLine("  </trk>")
        }

        xml.append("</gpx>")
        return xml.toString()
    }

    private fun String.escapeXml(): String =
        replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `ANDROID_HOME=/home/pascal/Android/Sdk ./gradlew :app:testDebugUnitTest --tests com.example.gpxsplice.io.GpxReaderWriterTest`

Expected: PASS for `GpxReaderWriterTest`.

- [ ] **Step 5: Commit**

Only include the writer and writer test changes from this task.

```bash
git add app/src/main/java/com/example/gpxsplice/io/GpxWriter.kt app/src/test/java/com/example/gpxsplice/io/GpxReaderWriterTest.kt
git commit -m "Add optional GPX timestamp sanitization"
```

---

### Task 2: Export Builder Parameters

**Files:**
- Modify: `app/src/test/java/com/example/gpxsplice/io/ExportFilesTest.kt`
- Modify: `app/src/main/java/com/example/gpxsplice/io/ExportFiles.kt`

- [ ] **Step 1: Write the failing tests**

Add these tests to `ExportFilesTest` near the existing export builder tests.

```kotlin
@Test
fun gpxFilesPreserveTimesByDefault() {
    val files = ExportBuilder.gpxFiles(results = listOf(splitResult(index = 1)))
    val xml = files.single().bytes.toString(Charsets.UTF_8)

    assertTrue(xml.contains("<time>2024-01-01T00:00:00Z</time>"))
}

@Test
fun gpxFilesCanRemoveTimesForSanitizedExport() {
    val files = ExportBuilder.gpxFiles(results = listOf(splitResult(index = 1)), sanitize = true)
    val xml = files.single().bytes.toString(Charsets.UTF_8)

    assertFalse(xml.contains("<time>"))
    assertTrue(xml.contains("lat=\"11.0\""))
    assertTrue(xml.contains("lon=\"21.0\""))
    assertTrue(xml.contains("<ele>101.0</ele>"))
}

@Test
fun zipBytesUseSanitizedSplitFilesWhenRequested() {
    val files = ExportBuilder.gpxFiles(results = listOf(splitResult(index = 1)), sanitize = true)
    val entries = unzipEntries(ZipExporter.zipBytes(files))
    val xml = entries.getValue("split-001.gpx").toString(Charsets.UTF_8)

    assertFalse(xml.contains("<time>"))
    assertTrue(xml.contains("lat=\"11.0\""))
}

@Test
fun mergedGpxFileCanRemoveTimesForSanitizedExport() {
    val document = GpxDocument(
        name = "Merged GPX",
        tracks = listOf(
            Track(
                "merged-track",
                listOf(
                    TrackSegment(
                        listOf(
                            TrackPoint(52.0, 5.0, elevationMeters = 12.0, time = "2026-04-26T08:00:00Z"),
                        ),
                    ),
                ),
            ),
        ),
    )

    val file = ExportBuilder.mergedGpxFile(document, firstInputFileName = "ride-day-1.gpx", sanitize = true)
    val xml = file.bytes.toString(Charsets.UTF_8)

    assertEquals("ride-day-1-merged.gpx", file.fileName)
    assertFalse(xml.contains("<time>"))
    assertTrue(xml.contains("lat=\"52.0\""))
    assertTrue(xml.contains("<ele>12.0</ele>"))
}
```

Update the existing `buildsGpxFilesWithSplitNamesAndWriterBytes()` and `buildsMergedGpxFileWithWriterBytes()` assertions to keep comparing against `GpxWriter.write(document)` with default sanitize false.

- [ ] **Step 2: Run test to verify it fails**

Run: `ANDROID_HOME=/home/pascal/Android/Sdk ./gradlew :app:testDebugUnitTest --tests com.example.gpxsplice.io.ExportFilesTest`

Expected: FAIL because `ExportBuilder.gpxFiles(..., sanitize = true)` and `ExportBuilder.mergedGpxFile(..., sanitize = true)` do not exist.

- [ ] **Step 3: Implement export builder parameters**

Update `ExportBuilder` in `ExportFiles.kt`.

```kotlin
object ExportBuilder {
    fun gpxFiles(
        results: List<SplitResult>,
        inputFileName: String? = null,
        sanitize: Boolean = false,
    ): List<ExportFile> = results.map { result ->
        val splitIndex = result.index.toString().padStart(3, '0')
        ExportFile(
            fileName = exportFileName(
                inputFileName = inputFileName,
                suffix = "-split-$splitIndex",
                defaultName = "split-$splitIndex.gpx",
                outputExtension = ".gpx",
            ),
            bytes = GpxWriter.write(result.document, sanitize = sanitize).toByteArray(Charsets.UTF_8),
        )
    }

    fun mergedGpxFile(document: GpxDocument, firstInputFileName: String?, sanitize: Boolean = false): ExportFile =
        ExportFile(
            fileName = mergedGpxFileName(firstInputFileName),
            bytes = GpxWriter.write(document, sanitize = sanitize).toByteArray(Charsets.UTF_8),
        )
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `ANDROID_HOME=/home/pascal/Android/Sdk ./gradlew :app:testDebugUnitTest --tests com.example.gpxsplice.io.ExportFilesTest`

Expected: PASS for `ExportFilesTest`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/gpxsplice/io/ExportFiles.kt app/src/test/java/com/example/gpxsplice/io/ExportFilesTest.kt
git commit -m "Thread sanitize option through export builders"
```

---

### Task 3: Split And Merge UI Option

**Files:**
- Modify: `app/src/test/java/com/example/gpxsplice/ui/GpxSplitLayoutTest.kt`
- Modify: `app/src/main/java/com/example/gpxsplice/ui/GpxSplitApp.kt`

- [ ] **Step 1: Write the failing helper text test**

Replace `mergeTimestampWarningExplainsExportSanitation()` in `GpxSplitLayoutTest.kt` with this test.

```kotlin
@Test
fun sanitizeExportHelpTextExplainsOptionalTimestampRemoval() {
    assertEquals(
        "Optional: remove time tags before sharing. Point order, coordinates, and elevation stay unchanged.",
        sanitizeExportHelpText(),
    )
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `ANDROID_HOME=/home/pascal/Android/Sdk ./gradlew :app:testDebugUnitTest --tests com.example.gpxsplice.ui.GpxSplitLayoutTest`

Expected: FAIL because `sanitizeExportHelpText()` does not exist and `mergeTimestampWarning()` is still present.

- [ ] **Step 3: Implement UI state, callbacks, checkbox, and helper text**

Update imports in `GpxSplitApp.kt`.

```kotlin
import androidx.compose.material3.Checkbox
```

Change callback types in `GpxSplitApp`.

```kotlin
onShareFiles: (List<SplitResult>, Boolean) -> Unit,
onShareZip: (List<SplitResult>, Boolean) -> Unit,
...
onShareMergedFile: (GpxDocument, String?, Boolean) -> Unit = { _, _, _ -> },
```

Add state near the existing workflow state.

```kotlin
var sanitizeSplitExport by remember { mutableStateOf(false) }
var sanitizeMergeExport by remember { mutableStateOf(false) }
```

Pass split sanitize state into `SplitWorkflow`.

```kotlin
sanitizeExport = sanitizeSplitExport,
onSanitizeExportChange = { sanitizeSplitExport = it },
onShareFiles = { shareResults -> onShareFiles(shareResults, sanitizeSplitExport) },
onShareZip = { shareResults -> onShareZip(shareResults, sanitizeSplitExport) },
```

Pass merge sanitize state into `MergeWorkflow`.

```kotlin
sanitizeExport = sanitizeMergeExport,
onSanitizeExportChange = { sanitizeMergeExport = it },
onShareMergedFile = { merged -> onShareMergedFile(merged, orderedMergeInputs.firstOrNull()?.fileName, sanitizeMergeExport) },
```

Update `SplitWorkflow` parameters.

```kotlin
sanitizeExport: Boolean,
onSanitizeExportChange: (Boolean) -> Unit,
onShareFiles: (List<SplitResult>) -> Unit,
onShareZip: (List<SplitResult>) -> Unit,
```

Add the option immediately above split export buttons.

```kotlin
SanitizeExportOption(
    checked = sanitizeExport,
    onCheckedChange = onSanitizeExportChange,
)
```

Update `MergeWorkflow` parameters.

```kotlin
sanitizeExport: Boolean,
onSanitizeExportChange: (Boolean) -> Unit,
onShareMergedFile: (GpxDocument) -> Unit,
```

Remove the error-colored timestamp warning `Surface` from the top of `MergeWorkflow`. Add the option immediately above `Share merged GPX`.

```kotlin
SanitizeExportOption(
    checked = sanitizeExport,
    onCheckedChange = onSanitizeExportChange,
)
Button(onClick = { onShareMergedFile(merged) }, modifier = Modifier.fillMaxWidth()) { Text("Share merged GPX") }
```

Add this composable near `ErrorText`.

```kotlin
@Composable
private fun SanitizeExportOption(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = checked,
                    onClick = { onCheckedChange(!checked) },
                    role = Role.Checkbox,
                )
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = checked, onCheckedChange = null)
            Text("Remove time tags from export", style = MaterialTheme.typography.bodyLarge)
        }
        Text(
            text = sanitizeExportHelpText(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
```

Replace `mergeTimestampWarning()` with this helper.

```kotlin
internal fun sanitizeExportHelpText(): String =
    "Optional: remove time tags before sharing. Point order, coordinates, and elevation stay unchanged."
```

- [ ] **Step 4: Run test to verify it passes**

Run: `ANDROID_HOME=/home/pascal/Android/Sdk ./gradlew :app:testDebugUnitTest --tests com.example.gpxsplice.ui.GpxSplitLayoutTest`

Expected: PASS for `GpxSplitLayoutTest`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/gpxsplice/ui/GpxSplitApp.kt app/src/test/java/com/example/gpxsplice/ui/GpxSplitLayoutTest.kt
git commit -m "Add optional sanitize controls to export UI"
```

---

### Task 4: Activity Export Plumbing

**Files:**
- Modify: `app/src/main/java/com/example/gpxsplice/MainActivity.kt`

- [ ] **Step 1: Run compile to expose callback mismatches**

Run: `ANDROID_HOME=/home/pascal/Android/Sdk ./gradlew :app:compileDebugKotlin`

Expected: FAIL because `MainActivity` still provides old callback lambdas to `GpxSplitApp`.

- [ ] **Step 2: Thread sanitize values through MainActivity**

Update the `GpxSplitApp` callback lambdas.

```kotlin
onShareFiles = { results, sanitize ->
    lifecycleScope.launch {
        runCatchingPreservingCancellation {
            shareFiles(ExportBuilder.gpxFiles(results, inputFileName, sanitize = sanitize))
        }.onSuccess {
            errorMessage = null
        }.onFailure {
            errorMessage = "Could not share GPX files"
        }
    }
},
onShareZip = { results, sanitize ->
    lifecycleScope.launch {
        runCatchingPreservingCancellation {
            shareZip(results, inputFileName, sanitize)
        }.onSuccess {
            errorMessage = null
        }.onFailure {
            errorMessage = "Could not share ZIP"
        }
    }
},
...
onShareMergedFile = { mergedDocument, firstInputFileName, sanitize ->
    lifecycleScope.launch {
        runCatchingPreservingCancellation {
            shareMergedFile(mergedDocument, firstInputFileName, sanitize)
        }.onSuccess {
            errorMessage = null
        }.onFailure {
            errorMessage = "Could not share merged GPX"
        }
    }
},
```

Update helper signatures and export builder calls.

```kotlin
private suspend fun shareZip(results: List<SplitResult>, inputFileName: String?, sanitize: Boolean) {
    val uri = withContext(Dispatchers.IO) {
        val exportDir = createExportDir()
        val zipFile = writeExportFile(
            exportDir,
            ExportFile(exportZipFileName(inputFileName), ZipExporter.zipBytes(ExportBuilder.gpxFiles(results, inputFileName, sanitize = sanitize))),
        )
        FileProvider.getUriForFile(this@MainActivity, "$packageName.fileprovider", zipFile)
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/zip"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newUri(contentResolver, "ZIP", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(intent, "Share ZIP"))
}

private suspend fun shareMergedFile(document: GpxDocument, firstInputFileName: String?, sanitize: Boolean) {
    val uri = withContext(Dispatchers.IO) {
        val exportDir = createExportDir()
        val output = writeExportFile(exportDir, ExportBuilder.mergedGpxFile(document, firstInputFileName, sanitize = sanitize))
        FileProvider.getUriForFile(this@MainActivity, "$packageName.fileprovider", output)
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/gpx+xml"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newUri(contentResolver, "Merged GPX", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(intent, "Share merged GPX"))
}
```

- [ ] **Step 3: Run compile to verify it passes**

Run: `ANDROID_HOME=/home/pascal/Android/Sdk ./gradlew :app:compileDebugKotlin`

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/gpxsplice/MainActivity.kt
git commit -m "Pass sanitize option into Android exports"
```

---

### Task 5: README Update

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Update user-facing docs**

Change the features list timestamp bullet from always sanitized to optional sanitization.

```markdown
- Optionally remove GPX point timestamps from split or merged exports before sharing.
```

Change the notes bullet from always omitted timestamps to optional behavior.

```markdown
- Exported GPX files preserve point timestamps by default. Enable the sanitize option to remove `<time>` values while keeping point order, coordinates, and elevation.
```

- [ ] **Step 2: Check README diff**

Run: `git diff -- README.md`

Expected: diff only changes the timestamp export wording and preserves unrelated README edits if any are present.

- [ ] **Step 3: Commit**

```bash
git add README.md
git commit -m "Document optional timestamp sanitization"
```

---

### Task 6: Full Verification

**Files:**
- No source edits expected.

- [ ] **Step 1: Run all JVM unit tests**

Run: `ANDROID_HOME=/home/pascal/Android/Sdk ./gradlew :app:testDebugUnitTest`

Expected: PASS.

- [ ] **Step 2: Run debug build**

Run: `ANDROID_HOME=/home/pascal/Android/Sdk ./gradlew :app:assembleDebug`

Expected: PASS.

- [ ] **Step 3: Run lint**

Run: `ANDROID_HOME=/home/pascal/Android/Sdk ./gradlew :app:lintDebug`

Expected: PASS or existing unrelated warnings only. If lint fails on code changed by this plan, fix and rerun lint before committing.

- [ ] **Step 4: Inspect final status**

Run: `git status --short`

Expected: clean except for unrelated pre-existing user changes that were intentionally not included.

- [ ] **Step 5: Final commit if verification required fixes**

If verification required any source fixes, commit only those fixes.

```bash
git add <changed-files-from-verification-fixes>
git commit -m "Fix sanitize export verification issues"
```

If no verification fixes were needed, do not create an empty commit.

---

## Self-Review

- Spec coverage: writer default preservation is covered in Task 1; optional timestamp removal is covered in Tasks 1 and 2; split and merge UI options are covered in Task 3; export plumbing is covered in Task 4; README wording is covered in Task 5; verification is covered in Task 6.
- Placeholder scan: no placeholders remain. Each code-changing step includes exact code or exact text replacement.
- Type consistency: `sanitize: Boolean = false` is used consistently for `GpxWriter.write`, `ExportBuilder.gpxFiles`, and `ExportBuilder.mergedGpxFile`; UI callbacks pass Boolean values through to `MainActivity`.
