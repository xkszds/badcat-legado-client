package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LegadoRuleExpressionDispatcherTest {

    @Test
    fun resolvesExplicitLegadoSelectorPrefixesCaseInsensitively() {
        val css = assertIs<EngineResult.Success<ResolvedLegadoRuleExpression>>(
            LegadoRuleExpressionDispatcher.resolve("@CSS: div.book"),
        ).value
        assertEquals(SelectorRuleType.CSS, css.type)
        assertEquals("div.book", css.expression)

        val xpath = assertIs<EngineResult.Success<ResolvedLegadoRuleExpression>>(
            LegadoRuleExpressionDispatcher.resolve("@XPath: //div[@class='book']"),
        ).value
        assertEquals(SelectorRuleType.XPATH, xpath.type)
        assertEquals("//div[@class='book']", xpath.expression)

        val jsonPath = assertIs<EngineResult.Success<ResolvedLegadoRuleExpression>>(
            LegadoRuleExpressionDispatcher.resolve("@Json: $.books[*].name"),
        ).value
        assertEquals(SelectorRuleType.JSON_PATH, jsonPath.type)
        assertEquals("$.books[*].name", jsonPath.expression)
    }

    @Test
    fun infersOnlyUnambiguousUnprefixedSelectorForms() {
        assertEquals(
            SelectorRuleType.CSS,
            assertIs<EngineResult.Success<ResolvedLegadoRuleExpression>>(
                LegadoRuleExpressionDispatcher.resolve("div.book > a.title"),
            ).value.type,
        )
        assertEquals(
            SelectorRuleType.XPATH,
            assertIs<EngineResult.Success<ResolvedLegadoRuleExpression>>(
                LegadoRuleExpressionDispatcher.resolve("//div[@class='book']"),
            ).value.type,
        )
        assertEquals(
            SelectorRuleType.JSON_PATH,
            assertIs<EngineResult.Success<ResolvedLegadoRuleExpression>>(
                LegadoRuleExpressionDispatcher.resolve("$.books[*].name"),
            ).value.type,
        )
    }

    @Test
    fun normalizesLegacySingleAtCssRootMarkerBeforeDispatch() {
        val resolved = assertIs<EngineResult.Success<ResolvedLegadoRuleExpression>>(
            LegadoRuleExpressionDispatcher.resolve("@.w_txt li.4@textNodes"),
        ).value
        assertEquals(SelectorRuleType.CSS, resolved.type)
        assertEquals(".w_txt li.4@textNodes", resolved.expression)
    }



    @Test
    fun evaluatesRealWorldLegadoChainedCollectionRoots() {
        val document = SourceDocument(
            url = "https://reader.example/search",
            body = """
                <div class="list">
                  <ul>
                    <li><a href="/book/1">Book One</a></li>
                    <li><a href="/book/2">Book Two</a></li>
                  </ul>
                </div>
                <div id="readerlist">
                  <ul><li><a href="/chapter/1">Chapter One</a></li></ul>
                </div>
            """.trimIndent(),
        )
        val bixiange = LegadoRuleExpressionDispatcher.evaluateElements(
            document,
            "class.list@tag.ul@tag.li",
        )
        assertEquals(
            listOf(
                """<li><a href="/book/1">Book One</a></li>""",
                """<li><a href="/book/2">Book Two</a></li>""",
            ),
            assertIs<EngineResult.Success<List<String>>>(bixiange).value,
        )
        val xuanhuange = LegadoRuleExpressionDispatcher.evaluateElements(
            document,
            "id.readerlist@tag.li",
        )
        assertEquals(
            listOf("""<li><a href="/chapter/1">Chapter One</a></li>"""),
            assertIs<EngineResult.Success<List<String>>>(xuanhuange).value,
        )
    }

    @Test
    fun evaluatesLegacyIndexedTextNodesInScalarRuleContext() {
        val document = SourceDocument(
            url = "https://reader.example/book/7",
            body = """
                <div class="w">
                  <txt>
                    <li>zero</li><li>one</li><li>two</li><li>three</li>
                    <li>Novel title</li>
                  </txt>
                </div>
            """.trimIndent(),
        )
        val result = LegadoRuleExpressionDispatcher.evaluate(
            document,
            "@.w txt li.4@textNodes",
        )
        assertEquals(
            listOf("Novel title"),
            assertIs<EngineResult.Success<List<String>>>(result).value,
        )
    }

    @Test
    fun evaluatesLegacySingleAtIndexedTextNodesRuleAsCollectionRoot() {
        val document = SourceDocument(
            url = "https://reader.example/book/7",
            body = """
                <ul class="w_txt">
                  <li>zero</li><li>one</li><li>two</li><li>three</li>
                  <li>Novel title</li>
                </ul>
            """.trimIndent(),
        )
        val result = LegadoRuleExpressionDispatcher.evaluateElements(
            document,
            "@.w_txt li.4@textNodes",
        )
        val values = assertIs<EngineResult.Success<List<String>>>(result).value
        assertEquals(1, values.size)
        kotlin.test.assertTrue(values.single().contains("Novel title"))
    }

    @Test
    fun resolvesDoubleAtAsLegadoForcedCssMode() {
        val forcedCss = assertIs<EngineResult.Success<ResolvedLegadoRuleExpression>>(
            LegadoRuleExpressionDispatcher.resolve("@@div.book > a.title"),
        ).value
        assertEquals(SelectorRuleType.CSS, forcedCss.type)
        assertEquals("div.book > a.title", forcedCss.expression)

        assertEquals(
            EngineErrorCode.UNSUPPORTED_RULE,
            assertIs<EngineResult.Failure>(
                LegadoRuleExpressionDispatcher.resolve("@@"),
            ).code,
        )
    }

    @Test
    fun javascriptAndRegexAreNotMisclassifiedAsSelectors() {
        assertEquals(
            EngineErrorCode.UNSUPPORTED_RULE,
            assertIs<EngineResult.Failure>(
                LegadoRuleExpressionDispatcher.resolve("@js:result"),
            ).code,
        )
        assertEquals(
            EngineErrorCode.UNSUPPORTED_RULE,
            assertIs<EngineResult.Failure>(
                LegadoRuleExpressionDispatcher.resolve("@regex:(\\\\d+)"),
            ).code,
        )
    }


    @Test
    fun combinesLeadingColonRegexWithOtherScalarBranches() {
        val document = SourceDocument(
            url = "https://reader.example/detail",
            body = "<h1>Book: Alpha</h1><p>Summary</p>",
        )
        val result = LegadoRuleExpressionDispatcher.evaluate(
            document,
            ":Book: (\\w+) && p@text",
        )
        assertEquals(
            listOf("Book: Alpha", "Alpha", "Summary"),
            assertIs<EngineResult.Success<List<String>>>(result).value,
        )
    }

    @Test
    fun evaluatesLeadingColonRegexForScalarRulesAndSelectsCaptureGroup() {
        val document = SourceDocument(
            url = "https://reader.example/detail",
            body = "<h1>Book: Alpha</h1>",
        )
        val result = LegadoRuleExpressionDispatcher.evaluate(document, ":Book: (\\w+)")
        val values = assertIs<EngineResult.Success<List<String>>>(result).value
        assertEquals(listOf("Book: Alpha", "Alpha"), values)
        assertEquals("Alpha", LegadoRuleExpressionDispatcher.firstScalarValue(":Book: (\\w+)", values))
    }

    @Test
    fun keepsQuotedAndNestedOperatorsInsideCombinedSelectorExpressions() {
        val document = SourceDocument(
            url = "https://reader.example/",
            body = """<div data-label="left&&right"><a href="/a||b">A</a></div>
                <div data-label="other"><a href="/c">C</a></div>""",
        )
        val result = LegadoRuleExpressionDispatcher.evaluateElements(
            document,
            """div[data-label="left&&right"] a[href="/a||b"]""",
        )
        assertEquals(
            listOf("""<a href="/a||b">A</a>"""),
            assertIs<EngineResult.Success<List<String>>>(result).value,
        )
    }


    @Test
    fun usesFirstNonEmptyFallbackBranchAndInterleavesParallelCollections() {
        val document = SourceDocument(
            url = "https://reader.example/",
            body = """<ul><li>A1</li><li>A2</li></ul>
                <ol><li>B1</li><li>B2</li><li>B3</li></ol>""",
        )
        val fallback = LegadoRuleExpressionDispatcher.evaluateElements(
            document,
            """@CSS: .missing || @CSS: ul > li""",
        )
        assertEquals(
            listOf("<li>A1</li>", "<li>A2</li>"),
            assertIs<EngineResult.Success<List<String>>>(fallback).value,
        )

        val interleaved = LegadoRuleExpressionDispatcher.evaluateElements(
            document,
            """@CSS: ul > li %% @CSS: ol > li""",
        )
        assertEquals(
            listOf("<li>A1</li>", "<li>B1</li>", "<li>A2</li>", "<li>B2</li>", "<li>B3</li>"),
            assertIs<EngineResult.Success<List<String>>>(interleaved).value,
        )
    }

}
