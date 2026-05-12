package com.example.gpxsplice.ui

import com.example.gpxsplice.domain.GpxDocument
import com.example.gpxsplice.domain.orderedPoints

data class MergeInput(
    val fileName: String,
    val document: GpxDocument,
) {
    val pointCount: Int = document.orderedPoints().size
    val earliestTimestamp: String? = document.orderedPoints()
        .mapNotNull { it.time?.trim()?.takeIf(String::isNotEmpty) }
        .minOrNull()
}

data class MergeOrderingResult(
    val items: List<MergeInput>,
    val wasChronologicallySorted: Boolean,
    val message: String?,
)

enum class MergeMoveDirection {
    UP,
    DOWN,
}

fun orderMergeInputs(items: List<MergeInput>): MergeOrderingResult {
    if (items.isEmpty()) {
        return MergeOrderingResult(items = emptyList(), wasChronologicallySorted = false, message = null)
    }

    val allHaveTimestamps = items.all { it.earliestTimestamp != null }
    if (!allHaveTimestamps) {
        return MergeOrderingResult(
            items = items,
            wasChronologicallySorted = false,
            message = "Some files have no timestamps, so selected order is preserved.",
        )
    }

    return MergeOrderingResult(
        items = items.sortedBy { it.earliestTimestamp },
        wasChronologicallySorted = true,
        message = null,
    )
}

fun moveMergeInput(items: List<MergeInput>, fromIndex: Int, direction: MergeMoveDirection): List<MergeInput> {
    val toIndex = when (direction) {
        MergeMoveDirection.UP -> fromIndex - 1
        MergeMoveDirection.DOWN -> fromIndex + 1
    }
    if (fromIndex !in items.indices || toIndex !in items.indices) return items

    return items.toMutableList().apply {
        val item = removeAt(fromIndex)
        add(toIndex, item)
    }
}

fun canMerge(items: List<MergeInput>): Boolean =
    items.size >= 2 && items.all { it.pointCount > 0 }
