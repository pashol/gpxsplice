package com.example.gpxsplice.domain

object GpxSplitter {
    fun split(document: GpxDocument, options: SplitOptions): List<SplitResult> {
        val points = document.orderedPointRefs()
        require(points.isNotEmpty()) { "No track points found" }

        val chunks = when (options.mode) {
            SplitMode.MAX_POINTS -> splitByMaxPoints(points, options.maxPoints!!)
            SplitMode.EQUAL_STAGES -> splitByStages(points, options.stages!!)
            SplitMode.DISTANCE -> splitByDistance(points, options.distanceMeters!!)
        }

        return chunks.mapIndexed { index, chunk ->
            SplitResult(
                index = index + 1,
                document = chunk.toDocument(document),
                pointCount = chunk.size,
                distanceMeters = chunk.totalTrackedDistanceMeters(),
            )
        }
    }

    private fun splitByMaxPoints(points: List<TrackPointRef>, maxPoints: Int): List<List<TrackPointRef>> {
        require(maxPoints >= 1)

        return points.chunked(maxPoints)
    }

    private fun splitByStages(points: List<TrackPointRef>, stages: Int): List<List<TrackPointRef>> {
        require(stages in 1..points.size)

        val baseSize = points.size / stages
        val remainder = points.size % stages
        var startIndex = 0

        return List(stages) { stageIndex ->
            val chunkSize = baseSize + if (stageIndex < remainder) 1 else 0
            val endIndex = startIndex + chunkSize
            points.subList(startIndex, endIndex).also {
                startIndex = endIndex
            }
        }
    }

    private fun splitByDistance(points: List<TrackPointRef>, maxDistanceMeters: Double): List<List<TrackPointRef>> {
        val chunks = mutableListOf<MutableList<TrackPointRef>>()
        var currentChunk = mutableListOf(points.first())
        var currentDistanceMeters = 0.0

        for (index in 1 until points.size) {
            val previousPoint = points[index - 1]
            val point = points[index]
            currentChunk += point
            if (point.isContinuationOf(previousPoint)) {
                currentDistanceMeters += haversineMeters(previousPoint.point, point.point)
            }

            if (currentDistanceMeters >= maxDistanceMeters && index < points.lastIndex) {
                chunks += currentChunk
                currentChunk = mutableListOf(point)
                currentDistanceMeters = 0.0
            }
        }

        chunks += currentChunk
        return chunks
    }
}

fun GpxDocument.orderedPoints(): List<TrackPoint> =
    tracks.flatMap { track ->
        track.segments.flatMap(TrackSegment::points)
    }

private fun GpxDocument.orderedPointRefs(): List<TrackPointRef> =
    tracks.flatMapIndexed { trackIndex, track ->
        track.segments.flatMapIndexed { segmentIndex, segment ->
            segment.points.mapIndexed { pointIndex, point ->
                TrackPointRef(
                    trackIndex = trackIndex,
                    segmentIndex = segmentIndex,
                    pointIndex = pointIndex,
                    point = point,
                )
            }
        }
    }

fun List<TrackPoint>.totalDistanceMeters(): Double =
    zipWithNext().sumOf { (start, end) -> haversineMeters(start, end) }

private fun List<TrackPointRef>.totalTrackedDistanceMeters(): Double =
    zipWithNext().sumOf { (start, end) ->
        if (end.isContinuationOf(start)) haversineMeters(start.point, end.point) else 0.0
    }

private fun List<TrackPointRef>.toDocument(source: GpxDocument): GpxDocument =
    GpxDocument(
        name = source.name,
        tracks =
            groupBy(TrackPointRef::trackIndex)
                .toSortedMap()
                .map { (trackIndex, trackPoints) ->
                    val sourceTrack = source.tracks[trackIndex]
                    Track(
                        name = sourceTrack.name,
                        segments =
                            trackPoints
                                .groupBy(TrackPointRef::segmentIndex)
                                .toSortedMap()
                                .map { (_, segmentPoints) ->
                                    TrackSegment(segmentPoints.map(TrackPointRef::point))
                                },
                    )
                },
    )

private data class TrackPointRef(
    val trackIndex: Int,
    val segmentIndex: Int,
    val pointIndex: Int,
    val point: TrackPoint,
)

private fun TrackPointRef.isContinuationOf(previous: TrackPointRef): Boolean =
    trackIndex == previous.trackIndex &&
        segmentIndex == previous.segmentIndex &&
        pointIndex == previous.pointIndex + 1
