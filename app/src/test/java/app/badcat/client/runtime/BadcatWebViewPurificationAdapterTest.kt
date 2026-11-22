package app.badcat.client.runtime

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BadcatWebViewPurificationAdapterTest {
    @Test
    fun acceptsOnlyTheCurrentDocumentCallback() {
        val current = "https://reader.example/chapter/2"

        assertTrue(isCurrentWebViewDocument(current, current))
        assertFalse(
            isCurrentWebViewDocument(
                current,
                "https://reader.example/chapter/1",
            ),
        )
    }

    @Test
    fun rejectsMissingOrBlankDocumentUrls() {
        assertFalse(isCurrentWebViewDocument(null, "https://reader.example/chapter/1"))
        assertFalse(isCurrentWebViewDocument("", "https://reader.example/chapter/1"))
        assertFalse(isCurrentWebViewDocument("https://reader.example/chapter/1", ""))
        assertFalse(isCurrentWebViewDocument(null, ""))
    }

    @Test
    fun doesNotNormalizeDifferentUrlsIntoTheSameDocument() {
        assertFalse(
            isCurrentWebViewDocument(
                "https://reader.example/chapter/1",
                "https://reader.example/chapter/1#section",
            ),
        )
        assertFalse(
            isCurrentWebViewDocument(
                "https://reader.example/chapter/1/",
                "https://reader.example/chapter/1",
            ),
        )
    }
}
