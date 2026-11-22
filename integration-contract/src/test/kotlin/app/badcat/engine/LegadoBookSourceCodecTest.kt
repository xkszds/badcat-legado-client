package app.badcat.engine

import com.google.gson.JsonParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.assertNull

class LegadoBookSourceCodecTest {

    @Test
    fun decodesUtf8WithoutCorruptingChineseSourceNames() {
        val json = """{"bookSourceUrl":"https://utf8.example","bookSourceName":"雪猫书源"}"""
        val decoded = LegadoBookSourceCodec.decodeUtf8ToString(json.toByteArray(Charsets.UTF_8))
        assertEquals("雪猫书源", LegadoBookSourceCodec.decode(decoded).single().name)
    }

    @Test
    fun rejectsMalformedUtf8AndOversizedDocuments() {
        assertFailsWith<java.nio.charset.CharacterCodingException> {
            LegadoBookSourceCodec.decodeUtf8ToString(byteArrayOf(0xC3.toByte(), 0x28))
        }
        assertFailsWith<IllegalArgumentException> {
            LegadoBookSourceCodec.decodeUtf8ToString(
                ByteArray(LegadoBookSourceCodec.MAX_IMPORT_BYTES + 1)
            )
        }
        assertFailsWith<IllegalArgumentException> {
            LegadoBookSourceCodec.decode(" ".repeat(LegadoBookSourceCodec.MAX_IMPORT_BYTES + 1))
        }
        // Character count is below 5 MiB, but UTF-8 byte count is above it.
        assertFailsWith<IllegalArgumentException> {
            LegadoBookSourceCodec.decode("雪".repeat(LegadoBookSourceCodec.MAX_IMPORT_BYTES / 2))
        }
    }

    @Test
    fun decodesSingleSourceAndKeepsUnknownRuleFields() {
        val input = """
            {
              "bookSourceUrl": "https://reader.example/",
              "bookSourceName": "Example source",
              "bookSourceGroup": "test",
              "bookSourceType": 0,
              "enabled": true,
              "enabledExplore": false,
              "enabledCookieJar": true,
              "header": "{\"User-Agent\":\"fixture\"}",
              "jsLib": "https://reader.example/lib.js",
              "loginUrl": "https://reader.example/login",
              "loginUi": "{\"version\":2}",
              "loginCheckJs": "typeof login === 'function'",
              "exploreUrl": "https://reader.example/explore",
              "searchUrl": "https://reader.example/search?q={{key}}",
              "ruleExplore": {"bookList": "div.explore"},
              "ruleSearch": {"bookList": "div.book", "name": "h3@text"},
              "ruleBookInfo": {"name": "h1@text"},
              "ruleToc": {"chapterList": "a.chapter"},
              "ruleContent": {"content": "#content"},
              "mainJs": "function search() { return []; }",
              "coverDecodeJs": "function decode(x) { return x; }",
              "bookUrlPattern": "https://reader.example/book/*",
              "bookSourceComment": "source note",
              "variableComment": "token: access token",
              "exploreScreen": "category",
              "lastUpdateTime": 1760000000000,
              "respondTime": 2400,
              "weight": 3,
              "customOrder": 41,
              "eventListener": true,
              "customButton": false,
              "ruleReview": {"reviewList":"div.review"},
              "concurrentRate": "2/500",
              "futureField": {"keep": true}
            }
        """.trimIndent()

        val source = LegadoBookSourceCodec.decode(input).single()
        assertEquals("https://reader.example/", source.sourceId)
        assertEquals("Example source", source.name)
        assertEquals("test", source.group)
        assertTrue(source.enabled)
        val engineSource = source.toEngineSource()
        assertEquals(source.sourceId, engineSource.id)
        assertEquals(source.name, engineSource.name)
        assertEquals(source.group, engineSource.group)
        assertEquals(source.rawJson, engineSource.rawJson)
        assertEquals("2/500", source.concurrentRate)
        assertEquals("2/500", engineSource.concurrentRate)
        assertFalse(engineSource.enabledExplore)
        assertEquals(true, engineSource.enabledCookieJar)
        assertEquals("https://reader.example/search?q={{key}}", engineSource.searchUrl)
        assertEquals("https://reader.example/explore", engineSource.exploreUrl)
        assertEquals("https://reader.example/login", engineSource.loginUrl)
        assertEquals("{\"version\":2}", engineSource.loginUi)
        assertEquals("typeof login === 'function'", engineSource.loginCheckJs)
        assertEquals("https://reader.example/lib.js", engineSource.jsLib)
        assertEquals("function search() { return []; }", engineSource.mainJs)
        assertEquals("function decode(x) { return x; }", engineSource.coverDecodeJs)
        assertEquals("https://reader.example/book/*", engineSource.bookUrlPattern)
        assertEquals("source note", engineSource.bookSourceComment)
        assertEquals("token: access token", engineSource.variableComment)
        assertEquals("category", engineSource.exploreScreen)
        assertEquals(1760000000000L, engineSource.lastUpdateTime)
        assertEquals(2400L, engineSource.respondTime)
        assertEquals(3, engineSource.weight)
        assertEquals(41, source.customOrder)
        assertEquals(41, engineSource.customOrder)
        assertTrue(engineSource.eventListener)
        assertEquals(false, engineSource.customButton)
        assertTrue(engineSource.ruleReviewJson!!.contains("reviewList"))
        assertTrue(engineSource.ruleSearchJson!!.contains("div.book"))
        assertTrue(engineSource.ruleContentJson!!.contains("#content"))

        val exported = JsonParser.parseString(LegadoBookSourceCodec.encode(listOf(source)))
            .asJsonArray.single().asJsonObject
        assertEquals("https://reader.example/search?q={{key}}", exported.get("searchUrl").asString)
        assertEquals("function search() { return []; }", exported.get("mainJs").asString)
        assertEquals(41, exported.get("customOrder").asInt)
        assertTrue(exported.getAsJsonObject("futureField").get("keep").asBoolean)
    }

    @Test
    fun decodesArrayAndUsesUpstreamDefaults() {
        val input = """
            [
              {"bookSourceUrl":"https://a.example","bookSourceName":"A"},
              {"bookSourceUrl":"https://b.example","bookSourceName":"B","enabled":false,"bookSourceType":4}
            ]
        """.trimIndent()

        val sources = LegadoBookSourceCodec.decode(input)
        assertEquals(2, sources.size)
        assertTrue(sources[0].enabled)
        assertEquals(0, sources[0].customOrder)
        assertEquals(0, sources[0].sourceType)
        assertEquals(true, sources[0].toEngineSource().enabledCookieJar)
        assertFalse(sources[1].enabled)
        assertEquals(4, sources[1].sourceType)
        assertEquals(0, sources[1].toEngineSource().customOrder)
    }


    @Test
    fun preservesNullableEnabledCookieJarMissingNullAndBooleanValues() {
        val missing = LegadoBookSourceCodec.decode(
            """{"bookSourceUrl":"https://missing-cookie-jar.example","bookSourceName":"Missing"}"""
        ).single()
        val explicitNull = LegadoBookSourceCodec.decode(
            """{"bookSourceUrl":"https://null-cookie-jar.example","bookSourceName":"Null","enabledCookieJar":null}"""
        ).single()
        val explicitTrue = LegadoBookSourceCodec.decode(
            """{"bookSourceUrl":"https://true-cookie-jar.example","bookSourceName":"True","enabledCookieJar":true}"""
        ).single()
        val explicitFalse = LegadoBookSourceCodec.decode(
            """{"bookSourceUrl":"https://false-cookie-jar.example","bookSourceName":"False","enabledCookieJar":false}"""
        ).single()

        assertEquals(true, missing.enabledCookieJar)
        assertNull(explicitNull.enabledCookieJar)
        assertNull(explicitNull.toEngineSource().enabledCookieJar)
        assertEquals(true, explicitTrue.enabledCookieJar)
        assertEquals(false, explicitFalse.enabledCookieJar)

        val exportedNull = JsonParser.parseString(
            LegadoBookSourceCodec.encode(listOf(explicitNull))
        ).asJsonArray.single().asJsonObject
        assertTrue(exportedNull.get("enabledCookieJar").isJsonNull)

        listOf(
            """{"bookSourceUrl":"https://bad-cookie-jar.example","bookSourceName":"Bad","enabledCookieJar":"true"}""",
            """{"bookSourceUrl":"https://bad-cookie-jar.example","bookSourceName":"Bad","enabledCookieJar":1}""",
        ).forEach { malformed ->
            assertFailsWith<IllegalArgumentException> { LegadoBookSourceCodec.decode(malformed) }
        }
    }

    @Test
    fun changingEnabledFlagPreservesUnknownFieldsAndRules() {
        val original = LegadoBookSourceCodec.decode(
            """{"bookSourceUrl":"https://toggle.example","bookSourceName":"Toggle","enabled":true,"customOrder":27,"future":{"x":7},"ruleSearch":{"bookList":"main"}}"""
        ).single()

        val disabled = LegadoBookSourceCodec.withEnabled(original, false)
        assertFalse(disabled.enabled)
        assertEquals(27, disabled.customOrder)
        val exported = JsonParser.parseString(
            LegadoBookSourceCodec.encode(listOf(disabled))
        ).asJsonArray.single().asJsonObject
        assertFalse(exported.get("enabled").asBoolean)
        assertEquals(27, exported.get("customOrder").asInt)
        assertEquals(7, exported.getAsJsonObject("future").get("x").asInt)
        assertEquals("main", exported.getAsJsonObject("ruleSearch").get("bookList").asString)
    }

    @Test
    fun rejectsMissingIdentityFields() {
        assertFailsWith<IllegalArgumentException> {
            LegadoBookSourceCodec.decode("""{"bookSourceName":"missing url"}""")
        }
        assertFailsWith<IllegalArgumentException> {
            LegadoBookSourceCodec.decode("""{"bookSourceUrl":"https://a.example"}""")
        }
    }

    @Test
    fun rejectsMalformedKnownFieldTypesInsteadOfApplyingDefaults() {
        val malformedSources = listOf(
            """{"bookSourceUrl":42,"bookSourceName":"Numeric URL"}""",
            """{"bookSourceUrl":"https://bad.example","bookSourceName":"Bad enabled","enabled":"yes"}""",
            """{"bookSourceUrl":"https://bad.example","bookSourceName":"Bad type","bookSourceType":"web"}""",
            """{"bookSourceUrl":"https://bad.example","bookSourceName":"Fractional type","bookSourceType":1.5}""",
            """{"bookSourceUrl":"https://bad.example","bookSourceName":"Overflow type","bookSourceType":2147483648}""",
            """{"bookSourceUrl":"https://bad.example","bookSourceName":"Bad header","header":{"User-Agent":"x"}}""",
            """{"bookSourceUrl":"https://bad.example","bookSourceName":"Bad review flag","enabledReview":"true"}""",
            """{"bookSourceUrl":"https://bad.example","bookSourceName":"Bad order","customOrder":1.25}""",
            """{"bookSourceUrl":"https://bad.example","bookSourceName":"Bad pattern","bookUrlPattern":42}"""
        )
        malformedSources.forEach { json ->
            assertFailsWith<IllegalArgumentException> {
                LegadoBookSourceCodec.decode(json)
            }
        }
    }

    @Test
    fun validatesAllPinnedBookSourceMetadataFields() {
        val accepted = """{
            "bookSourceUrl":"https://full.example",
            "bookSourceName":"Full metadata",
            "coverDecodeJs":"function decode(x) { return x; }",
            "variableComment":"token: access token",
            "exploreScreen":"category",
            "eventListener":true,
            "customButton":false,
            "ruleReview":{"reviewList":"div.review","reviewContent":"p@text"}
        }""".trimIndent()
        assertEquals(
            "Full metadata",
            LegadoBookSourceCodec.decode(accepted).single().name,
        )

        val malformed = listOf(
            """{"bookSourceUrl":"https://bad.example","bookSourceName":"Bad cover JS","coverDecodeJs":7}""",
            """{"bookSourceUrl":"https://bad.example","bookSourceName":"Bad variable comment","variableComment":false}""",
            """{"bookSourceUrl":"https://bad.example","bookSourceName":"Bad explore screen","exploreScreen":[]}""",
            """{"bookSourceUrl":"https://bad.example","bookSourceName":"Bad event listener","eventListener":"true"}""",
            """{"bookSourceUrl":"https://bad.example","bookSourceName":"Bad custom button","customButton":1}""",
            """{"bookSourceUrl":"https://bad.example","bookSourceName":"Bad review rule","ruleReview":"div.review"}"""
        )
        malformed.forEach { json ->
            assertFailsWith<IllegalArgumentException> {
                LegadoBookSourceCodec.decode(json)
            }
        }
    }

    @Test
    fun rejectsMalformedRuleObjectsInsteadOfSilentlyDroppingThem() {
        val malformedSources = listOf(
            """{"bookSourceUrl":"https://bad.example","bookSourceName":"Bad search","ruleSearch":"div.book"}""",
            """{"bookSourceUrl":"https://bad.example","bookSourceName":"Bad content","ruleContent":[]}"""
        )
        malformedSources.forEach { json ->
            assertFailsWith<IllegalArgumentException> {
                LegadoBookSourceCodec.decode(json)
            }
        }
    }

    @Test
    fun decodesLegacyStringEncodedRuleObjects() {
        val rule = JsonParser.parseString(
            """{"bookList":"div.book","name":"h2@text"}"""
        ).asJsonObject
        val sourceJson = JsonParser.parseString(
            """{"bookSourceUrl":"https://legacy.example","bookSourceName":"Legacy"}"""
        ).asJsonObject.apply {
            addProperty("ruleSearch", rule.toString())
        }
        val source = LegadoBookSourceCodec.decode(sourceJson.toString()).single()
        assertTrue(source.ruleSearchJson!!.contains("div.book"))
        assertTrue(source.ruleSearchJson!!.contains("h2@text"))
        val exported = JsonParser.parseString(
            LegadoBookSourceCodec.encode(listOf(source))
        ).asJsonArray.single().asJsonObject
        assertTrue(exported.get("ruleSearch").asString.contains("div.book"))
    }
    @Test
    fun rejectsInvalidContentBatchSizes() {
        val malformedRules = listOf(
            """{"maxBatchSize":1.5}""",
            """{"maxBatchSize":2147483648}""",
            """{"maxBatchSize":"10"}""",
            """{"maxBatchSize":true}"""
        )
        malformedRules.forEach { rule ->
            val source = JsonParser.parseString(
                """{"bookSourceUrl":"https://batch.example","bookSourceName":"Batch","ruleContent":$rule}"""
            )
            assertFailsWith<IllegalArgumentException> {
                LegadoBookSourceCodec.decode(source.toString())
            }
        }
        val legacyStringRule = """{"maxBatchSize":1.5}"""
        val legacySource = JsonParser.parseString(
            """{"bookSourceUrl":"https://batch-legacy.example","bookSourceName":"Batch legacy"}"""
        ).asJsonObject.apply {
            addProperty("ruleContent", legacyStringRule)
        }
        assertFailsWith<IllegalArgumentException> {
            LegadoBookSourceCodec.decode(legacySource.toString())
        }
    }

    @Test
    fun rejectsDuplicateSourceUrlsInsteadOfSilentlyOverwriting() {
        val duplicate = """
            [
              {"bookSourceUrl":"https://same.example","bookSourceName":"First"},
              {"bookSourceUrl":"https://same.example","bookSourceName":"Second"}
            ]
        """.trimIndent()
        assertFailsWith<IllegalArgumentException> {
            LegadoBookSourceCodec.decode(duplicate)
        }
    }

    @Test
    fun rejectsExcessiveJsonNestingBeforeParsing() {
        val json = """{"bookSourceUrl":"https://depth.example","bookSourceName":"Depth","future":""" +
            "[".repeat(LegadoBookSourceCodec.MAX_JSON_NESTING) + "0" +
            "]".repeat(LegadoBookSourceCodec.MAX_JSON_NESTING) + "}"
        assertFailsWith<IllegalArgumentException> {
            LegadoBookSourceCodec.decode(json)
        }
    }

    @Test
    fun boundsSourceCountAndPerSourceFieldCount() {
        val sources = List(LegadoBookSourceCodec.MAX_SOURCES + 1) { index ->
            """{"bookSourceUrl":"https://$index.example","bookSourceName":"Source $index"}"""
        }
        assertFailsWith<IllegalArgumentException> {
            LegadoBookSourceCodec.decode(sources.joinToString(prefix = "[", postfix = "]"))
        }

        val fields = buildString {
            append("""{"bookSourceUrl":"https://fields.example","bookSourceName":"Fields"""")
            repeat(LegadoBookSourceCodec.MAX_FIELDS_PER_SOURCE) { index ->
                append(""","future$index":$index""")
            }
            append("}")
        }
        assertFailsWith<IllegalArgumentException> {
            LegadoBookSourceCodec.decode(fields)
        }
    }

    @Test
    fun rejectsOversizedAndDuplicateSourceExports() {
        val source = LegadoBookSourceCodec.decode(
            """{"bookSourceUrl":"https://export.example","bookSourceName":"Export"}"""
        ).single()
        assertFailsWith<IllegalArgumentException> {
            LegadoBookSourceCodec.encode(List(LegadoBookSourceCodec.MAX_SOURCES + 1) { source })
        }
        val duplicateError = assertFailsWith<IllegalArgumentException> {
            LegadoBookSourceCodec.encode(listOf(source, source))
        }
        assertTrue(duplicateError.message.orEmpty().contains(source.sourceId))
    }


    @Test
    fun acceptsLongTimestampsAndValidUpstreamMetadata() {
        val source = LegadoBookSourceCodec.decode(
            """{"bookSourceUrl":"https://metadata.example","bookSourceName":"Metadata","bookUrlPattern":"https://metadata.example/book/*","enabledReview":false,"customOrder":12,"weight":3,"lastUpdateTime":1760000000000,"respondTime":2400}"""
        ).single()
        assertEquals("Metadata", source.name)
        assertEquals(12, source.customOrder)
        assertEquals(12, source.toEngineSource().customOrder)
    }

}
