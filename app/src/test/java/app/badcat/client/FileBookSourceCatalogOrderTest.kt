package app.badcat.client

import app.badcat.engine.LegadoBookSourceCodec
import org.junit.Assert.assertEquals
import org.junit.Test

class FileBookSourceCatalogOrderTest {
    @Test
    fun sortsCustomOrderAscendingAndPreservesOriginalOrderForTies() {
        val sources = LegadoBookSourceCodec.decode(
            """[
                {"bookSourceUrl":"https://late.example","bookSourceName":"Late","customOrder":8},
                {"bookSourceUrl":"https://zero-first.example","bookSourceName":"Zero first","customOrder":0},
                {"bookSourceUrl":"https://negative-first.example","bookSourceName":"Negative first","customOrder":-3},
                {"bookSourceUrl":"https://zero-second.example","bookSourceName":"Zero second","customOrder":0},
                {"bookSourceUrl":"https://negative-second.example","bookSourceName":"Negative second","customOrder":-3}
            ]""".trimIndent()
        )

        val ordered = orderBookSourcesForCatalog(sources)

        assertEquals(
            listOf(
                "https://negative-first.example",
                "https://negative-second.example",
                "https://zero-first.example",
                "https://zero-second.example",
                "https://late.example",
            ),
            ordered.map { it.sourceId },
        )
    }
}
