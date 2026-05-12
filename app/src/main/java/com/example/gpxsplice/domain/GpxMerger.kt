package com.example.gpxsplice.domain

object GpxMerger {
    fun merge(documents: List<GpxDocument>): GpxDocument {
        require(documents.size >= 2) { "At least 2 GPX files are required" }

        return GpxDocument(
            name = "Merged GPX",
            tracks = documents.flatMap { it.tracks },
        )
    }
}
