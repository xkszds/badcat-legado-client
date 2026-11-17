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
    fun adMarkMatcherRejectsNonWebSchemesAndKeepsExactHostSemantics() {
        assertFalse(mark.appliesToHost("file:///reader.example/1/2/3"))
        assertFalse(mark.appliesToHost("content://reader.example/1/2/3"))
        assertFalse(mark.appliesToHost("javascript:https://reader.example/1/2/3"))
        assertTrue(mark.appliesToHost("HTTPS://READER.EXAMPLE:8443/1/2/3"))
        assertFalse(mark.appliesToHost("https://reader.example.attacker.test/1/2/3"))
        assertFalse(mark.appliesToHost("https://attacker.test/reader.example/1/2/3"))
    }

    @Test
    fun badcatInjectionRequiresOriginalToggleAndNonBlankScript() {
        val disabled = BadcatPurificationSnapshot(
            webViewAdFilterEnabled = false,
            webViewInjectionScript = "window.badcatFilter = true;",
            adMarks = listOf(mark),
        )
        assertNull(BadcatPurificationUrlPolicy.injectionForPage(disabled))
        assertNull(BadcatPurificationUrlPolicy.injectionForPage(disabled, "https://reader.example/book"))

        val enabled = disabled.copy(webViewAdFilterEnabled = true)
        assertEquals(
            "window.badcatFilter = true;",
            BadcatPurificationUrlPolicy.injectionForPage(enabled),
        )
        assertEquals(
            "window.badcatFilter = true;",
            BadcatPurificationUrlPolicy.injectionForPage(enabled, "https://reader.example/book"),
        )
        assertNull(
            BadcatPurificationUrlPolicy.injectionForPage(
                enabled.copy(webViewInjectionScript = " "),
                "https://reader.example/book",
            ),
        )
    }

    @Test
    fun injectionIsRejectedForLocalAndNonWebDocuments() {
        val enabled = BadcatPurificationSnapshot(
            webViewAdFilterEnabled = true,
            webViewInjectionScript = "window.badcatFilter = true;",
            adMarks = emptyList(),
        )
        assertNull(BadcatPurificationUrlPolicy.injectionForPage(enabled, "file:///data/user/0/app/page.html"))
        assertNull(BadcatPurificationUrlPolicy.injectionForPage(enabled, "content://provider/page"))
        assertNull(BadcatPurificationUrlPolicy.injectionForPage(enabled, "data:text/html,hello"))
        assertNull(BadcatPurificationUrlPolicy.injectionForPage(enabled, "about:blank"))
        assertNull(BadcatPurificationUrlPolicy.injectionForPage(enabled, "javascript:alert(1)"))
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
