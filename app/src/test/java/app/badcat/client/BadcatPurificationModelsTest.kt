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
    fun adMarkCodecImportsOriginalNestedIssuedResponseAndExportsRoundTrip() {
        val original = """{
            "version": 7,
            "markItems": [
                {
                    "host": "https://reader.example/book/1",
                    "ruleItems": [
                        {"urlPattern": [1, 2, 3], "markRules": [".ad-card", "#sponsor"]},
                        {"urlPattern": [4, 5], "markRules": ["[data-ad-slot]"]}
                    ]
                }
            ],
            "adBlockJsInject": "window.untrustedServerScript = true;"
        }""".trimIndent()

        val imported = BadcatAdMarkJsonCodec.decode(original)
        assertEquals(
            listOf(
                BadcatAdMarkItem("reader.example", "[1,2,3]", listOf(".ad-card", "#sponsor")),
                BadcatAdMarkItem("reader.example", "[4,5]", listOf("[data-ad-slot]")),
            ),
            imported,
        )

        val exported = BadcatAdMarkJsonCodec.encode(imported)
        assertEquals(imported, BadcatAdMarkJsonCodec.decode(exported))
        val root = com.google.gson.JsonParser.parseString(exported).asJsonObject
        assertTrue(root.has("markItems"))
        assertFalse(root.has("adBlockJsInject"))
    }

    @Test
    fun adMarkCodecKeepsCompatibilityWithEarlierFlatExports() {
        val legacy = """[
            {"host":"reader.example","urlPattern":"[1,2,3]","markRules":[".ad-card"]}
        ]""".trimIndent()
        assertEquals(
            listOf(BadcatAdMarkItem("reader.example", "[1,2,3]", listOf(".ad-card"))),
            BadcatAdMarkJsonCodec.decode(legacy),
        )
    }

    @Test
    fun adMarkCreatesOneItemJsonPayloadPerVerifiedWebViewDeleteRule() {
        val grouped = mark.copy(markRules = listOf(".ad-card", "#sponsor"))
        assertEquals(
            listOf("""[".ad-card"]""", """["#sponsor"]"""),
            grouped.webViewDeleteCallbackPayloads(),
        )
    }

    @Test
    fun adMarkUrlPatternRequiresExactNonNegativeIntegers() {
        assertFalse(mark.copy(urlPattern = "[1.5,2,3]").isValid())
        assertFalse(mark.copy(urlPattern = "[-1,2,3]").isValid())
        assertFalse(mark.copy(urlPattern = "[2147483648]").isValid())
        assertTrue(mark.copy(urlPattern = "[1,2,3]").isValid())
        assertTrue(mark.copy(urlPattern = "[1e2,2,3]").isValid())
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

    @Test
    fun injectionRejectsOversizedScriptsAndMalformedPageUrls() {
        val enabled = BadcatPurificationSnapshot(
            webViewAdFilterEnabled = true,
            webViewInjectionScript = "x".repeat(BadcatPurificationUrlPolicy.MAX_SCRIPT_CHARS + 1),
            adMarks = emptyList(),
        )
        assertNull(BadcatPurificationUrlPolicy.injectionForPage(enabled))
        assertNull(BadcatPurificationUrlPolicy.injectionForPage(enabled, "not a url"))
        assertNull(BadcatPurificationUrlPolicy.injectionForPage(enabled, "file:///tmp/page.html"))
    }
}
