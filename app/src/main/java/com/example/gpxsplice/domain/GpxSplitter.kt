package com.example.gpxsplice.domain

object GpxSplitter {
    fun split(document: GpxDocument, options: SplitOptions): List<SplitResult> {
        require(document.tracks.size <= 1) { "Only single-track documents are supported" }
        document.tracks.firstOrNull()?.let { track ->
            require(track.segments.size <= 1) { "Only single-segment tracks are supported" }
        }

        val points = document.orderedPoints()
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
                distanceMeters = chunk.totalDistanceMeters(),
            )
        }
    }

    private fun splitByMaxPoints(points: List<TrackPoint>, maxPoints: Int): List<List<TrackPoint>> {
        require(maxPoints >= 1)

        return points.chunked(maxPoints)
    }

    private fun splitByStages(points: List<TrackPoint>, stages: Int): List<List<TrackPoint>> {
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

    private fun splitByDistance(points: List<TrackPoint>, maxDistanceMeters: Double): List<List<TrackPoint>> {
        val chunks = mutableListOf<MutableList<TrackPoint>>()
        var currentChunk = mutableListOf(points.first())
        var currentDistanceMeters = 0.0

        for (index in 1 until points.size) {
            val previousPoint = points[index - 1]
            val point = points[index]
            currentChunk += point
            currentDistanceMeters += haversineMeters(previousPoint, point)

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

fun List<TrackPoint>.totalDistanceMeters(): Double =
    zipWithNext().sumOf { (start, end) -> haversineMeters(start, end) }

private fun List<TrackPoint>.toDocument(source: GpxDocument): GpxDocument =
    GpxDocument(
        name = source.name,
        tracks = listOf(Track(name = source.tracks.firstOrNull()?.name, segments = listOf(TrackSegment(this)))),
    )
