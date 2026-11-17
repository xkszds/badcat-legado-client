package app.badcat.client

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BadcatPurificationModelsTest {
    private val mark = BadcatAdMarkItem(
        host = "reader.example",
        urlPattern = "[1,2,3]",
        markRules = listOf("data-ad-container"),
    )

    @Test
    fun adMarkUsesExactHostAndRetainsOpaqueRuleValues() {
        assertTrue(mark.isValid())
        assertTrue(mark.appliesToHost("https://reader.example/book/1"))
        assertFalse(mark.appliesToHost("https://sub.reader.example/book/1"))
        assertTrue(mark.matchesUrl("https://reader.example/1/2/3/ad"))
        assertFalse(mark.matchesUrl("https://reader.example/1/2/4/ad"))
        assertEquals(listOf(mark), BadcatPurificationUrlPolicy.marksForUrl(
            "https://reader.example/1/2/3/ad",
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
        val invalid = mark.copy(urlPattern = "not-json")
        assertFalse(invalid.isValid())
        assertTrue(
            BadcatPurificationUrlPolicy.marksForUrl(
                "https://reader.example/book/1",
                listOf(invalid),
            ).isEmpty(),
        )
    }
}
