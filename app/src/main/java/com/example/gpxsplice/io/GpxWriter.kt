package com.example.gpxsplice.io

import com.example.gpxsplice.domain.GpxDocument

object GpxWriter {
    fun write(document: GpxDocument): String {
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
                    xml.append("      <trkpt lat=\"${point.latitude}\" lon=\"${point.longitude}\"")
                    if (point.elevationMeters == null && point.time == null) {
                        xml.appendLine(" />")
                    } else {
                        xml.append('>')
                        point.elevationMeters?.let { xml.append("<ele>$it</ele>") }
                        point.time?.let { xml.append("<time>${it.escapeXml()}</time>") }
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
