package com.chali2n.thelemicpanels

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** The Thelemic Date line, the one panel that needs the network. */
object Thelemic {
    private const val ENDPOINT =
        "https://date.eralegis.info/now?format=json&tz=America/New_York&location=${Astro.ERA_LEGIS_LOCATION}"
    const val CTA_LABEL = "Open Era Legis"
    const val CTA_URL = "https://eralegis.info/"

    private const val SEPARATOR = " : "

    fun fetchPlain(): String {
        val connection = URL(ENDPOINT).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/json")
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("Era Legis returned HTTP $code")
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            return JSONObject(body).getString("plain")
        } finally {
            connection.disconnect()
        }
    }

    /** Split the line into its four parts, exactly as the source writes them. */
    fun split(plain: String): List<String> = plain.split(SEPARATOR)

    fun panel(nowMs: Long): Panel {
        val parts = split(fetchPlain())
        val summary = if (parts.size == 4) {
            parts.flatMapIndexed { index, part ->
                if (index == 0) listOf(Seg.normal(part)) else listOf(Seg.muted(SEPARATOR), Seg.normal(part))
            }
        } else {
            listOf(Seg.normal(parts.joinToString(SEPARATOR)))
        }

        return Panel(
            kind = PanelKind.DATE,
            summary = summary,
            cta = CTA_LABEL,
            ctaUrl = CTA_URL,
            updatedAtMs = nowMs,
        )
    }
}
