package com.example.gpxsplice.io

import com.example.gpxsplice.domain.GpxDocument
import com.example.gpxsplice.domain.Track
import com.example.gpxsplice.domain.TrackPoint
import com.example.gpxsplice.domain.TrackSegment
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

object GpxReader {
    fun read(input: InputStream): GpxDocument {
        val document = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }.newDocumentBuilder().parse(input)
        val root = document.documentElement

        fun Element.elements(localName: String): List<Element> {
            val matches = mutableListOf<Element>()
            val children = childNodes
            for (index in 0 until children.length) {
                val child = children.item(index)
                if (child is Element && child.localName == localName) {
                    matches += child
                }
            }
            return matches
        }

        fun Element.firstElement(localName: String): Element? = elements(localName).firstOrNull()

        val name = root.firstElement("metadata")
            ?.firstElement("name")
            ?.textContent
            ?.takeIf(String::isNotBlank)

        val tracks = root.elements("trk").map { trackElement ->
            Track(
                name = trackElement.firstElement("name")?.textContent?.takeIf(String::isNotBlank),
                segments = trackElement.elements("trkseg").map { segmentElement ->
                    TrackSegment(
                        points = segmentElement.elements("trkpt").map { pointElement ->
                            TrackPoint(
                                latitude = pointElement.getAttribute("lat").toDouble(),
                                longitude = pointElement.getAttribute("lon").toDouble(),
                                elevationMeters = pointElement.firstElement("ele")?.textContent?.toDouble(),
                                time = pointElement.firstElement("time")?.textContent?.takeIf(String::isNotBlank),
                            )
                        },
                    )
                },
            )
        }

        return GpxDocument(name = name, tracks = tracks)
    }
}
