package com.example.gpxsplice.io

import com.example.gpxsplice.domain.GpxDocument
import com.example.gpxsplice.domain.Track
import com.example.gpxsplice.domain.TrackPoint
import com.example.gpxsplice.domain.TrackSegment
import java.io.InputStream
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import org.w3c.dom.Element
import org.xml.sax.EntityResolver
import org.xml.sax.InputSource
import org.xml.sax.SAXException

object GpxReader {
    fun read(input: InputStream): GpxDocument {
        return read(input, DocumentBuilderFactory.newInstance())
    }

    internal fun read(input: InputStream, documentBuilderFactory: DocumentBuilderFactory): GpxDocument {
        val document = try {
            newSecureDocumentBuilderFactory(documentBuilderFactory).newDocumentBuilder().apply {
                setEntityResolver(EntityResolver { _, _ -> InputSource(StringReader("")) })
            }.parse(input)
        } catch (error: SAXException) {
            throw IllegalArgumentException("Invalid GPX XML: ${error.message}", error)
        }
        val root = document.documentElement

        fun Element.elements(localName: String): List<Element> {
            val matches = mutableListOf<Element>()
            val children = childNodes
            for (index in 0 until children.length) {
                val child = children.item(index)
                if (child is Element && child.matchesName(localName)) {
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
                                latitude = pointElement.parseRequiredDoubleAttribute("lat"),
                                longitude = pointElement.parseRequiredDoubleAttribute("lon"),
                                elevationMeters = pointElement.firstElement("ele")?.parseDoubleText("ele"),
                                time = pointElement.firstElement("time")?.textContent?.takeIf(String::isNotBlank),
                            )
                        },
                    )
                },
            )
        }

        return GpxDocument(name = name, tracks = tracks)
    }

    private fun newSecureDocumentBuilderFactory(factory: DocumentBuilderFactory): DocumentBuilderFactory =
        factory.apply {
            isNamespaceAware = true
            trySetFeature("http://xml.org/sax/features/external-general-entities", false)
            trySetFeature("http://xml.org/sax/features/external-parameter-entities", false)
            trySetFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            trySetXIncludeAware(false)
            isExpandEntityReferences = false
        }

    private fun DocumentBuilderFactory.trySetFeature(name: String, value: Boolean) {
        try {
            setFeature(name, value)
        } catch (_: ParserConfigurationException) {
            // Android parser implementations vary; unsupported hardening flags should not block parsing.
        }
    }

    private fun DocumentBuilderFactory.trySetXIncludeAware(value: Boolean) {
        try {
            isXIncludeAware = value
        } catch (_: UnsupportedOperationException) {
            // Some Android parsers do not implement XInclude configuration.
        }
    }

    private fun Element.matchesName(name: String): Boolean = localName == name || tagName == name

    private fun Element.parseRequiredDoubleAttribute(attributeName: String): Double {
        val value = getAttribute(attributeName)
        return value.toDoubleOrNull()
            ?: throw IllegalArgumentException("Invalid $attributeName value '$value' in <${tagName}>")
    }

    private fun Element.parseDoubleText(elementName: String): Double {
        val value = textContent.trim()
        return value.toDoubleOrNull()
            ?: throw IllegalArgumentException("Invalid $elementName value '$value' in <${tagName}>")
    }
}
