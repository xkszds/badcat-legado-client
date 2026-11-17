package app.badcat.client

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BadcatPurificationModelsTest {
    private val mark = BadcatAdMarkItem(
        host = "reader.example",
        urlPattern = "/ads/",
        markRules = listOf("data-ad-container"),
    )

    @Test
    fun adMarkUsesExactHostAndRetainsOpaqueRuleValues() {
        assertTrue(mark.isValid())
        assertTrue(mark.appliesToHost("https://reader.example/book/1"))
        assertFalse(mark.appliesToHost("https://sub.reader.example/book/1"))
        assertEquals(listOf(mark), BadcatPurificationUrlPolicy.marksForUrl(
            "https://reader.example/book/1",
            listOf(mark),
        ))
        assertTrue(mark.markRules.contains("data-ad-container"))
    }

    @Test
    fun badcatInjectionRequiresOriginalToggleAndNonBlankScript() {
        val disabled = BadcatPurificationSnapshot(
            webViewAdFilterEnabled = false,
            webViewInjectionScript = "window.badcatFilter = true;",
            adMarks = listOf(mark),
        )
        assertNull(BadcatPurificationUrlPolicy.injectionForPage(disabled))

        val enabled = disabled.copy(webViewAdFilterEnabled = true)
        assertEquals(
            "window.badcatFilter = true;",
            BadcatPurificationUrlPolicy.injectionForPage(enabled),
        )
        assertNull(
            BadcatPurificationUrlPolicy.injectionForPage(
                enabled.copy(webViewInjectionScript = " "),
            ),
        )
    }

    @Test
    fun invalidMarkRecordsAreNotSelectedForPage() {
        val invalid = mark.copy(urlPattern = "")
        assertFalse(invalid.isValid())
        assertTrue(
            BadcatPurificationUrlPolicy.marksForUrl(
                "https://reader.example/book/1",
                listOf(invalid),
            ).isEmpty(),
        )
    }
}
