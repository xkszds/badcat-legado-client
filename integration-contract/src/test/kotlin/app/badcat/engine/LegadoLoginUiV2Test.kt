package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LegadoLoginUiV2Test {
    @Test
    fun validatesTypedInvocationContextAndPreservesCombinedBookTypeFlags() {
        val context = LegadoLoginUiV2.InvocationContext(
            sourceId = "https://login.example/",
            bookType = 8 or 32,
            bookUrl = "https://book.example/book",
            chapterUrl = "https://book.example/chapter/1",
        )
        assertNull(context.validationError("https://login.example/"))
        assertEquals(
            """{"sourceId":"https://login.example/","bookType":40,"bookUrl":"https://book.example/book","chapterUrl":"https://book.example/chapter/1"}""",
            context.toJson(),
        )
        assertNull(LegadoLoginUiV2.InvocationContext("https://login.example/").validationError("https://login.example/"))
    }

    @Test
    fun rejectsInvalidInvocationContextWithoutGuessingBookType() {
        val sourceId = "https://login.example/"
        assertTrue(LegadoLoginUiV2.InvocationContext(sourceId, bookType = 1).validationError(sourceId) != null)
        assertTrue(LegadoLoginUiV2.InvocationContext(sourceId, bookType = -1).validationError(sourceId) != null)
        assertTrue(LegadoLoginUiV2.InvocationContext(sourceId).validationError("https://other.example/") != null)
        assertTrue(LegadoLoginUiV2.InvocationContext(" ").validationError(sourceId) != null)
        assertTrue(LegadoLoginUiV2.InvocationContext(sourceId, chapterUrl = "chapter").validationError(sourceId) != null)
        assertTrue(LegadoLoginUiV2.InvocationContext(sourceId, bookUrl = " ").validationError(sourceId) != null)
        assertTrue(
            LegadoLoginUiV2.InvocationContext(sourceId, bookUrl = "x".repeat(4_097))
                .validationError(sourceId) != null
        )
    }

    @Test
    fun identifiesV2LoginUiAndParsesSupportedRows() {
        val render = """{"rows":[
            {"name":"账号","type":"text","key":"username","hint":"请输入账号"},
            {"name":"密码","type":"password","key":"password"},
            {"name":"说明","type":"label"},
            {"name":"地区","type":"select","key":"region","options":["CN","US"]},
            {"name":"记住我","type":"toggle","key":"remember","value":"true","action":"remember"},
            {"name":"发送验证码","type":"button","action":"sendCode","countdown":60}
        ]}"""
        val rows = LegadoLoginUiV2.parseRows(render)
        assertEquals(6, rows?.size)
        assertEquals(LegadoLoginUiV2.RowType.PASSWORD, rows?.get(1)?.type)
        assertEquals(listOf("CN", "US"), rows?.get(3)?.options)
        assertTrue(LegadoLoginUiV2.isV2("""{"version":2}"""))
        assertFalse(LegadoLoginUiV2.isV2("""{"version":1}"""))
    }

    @Test
    fun requiresExactIntegerValuesForVersionAndCountdown() {
        assertTrue(LegadoLoginUiV2.isV2("""{"version":2.0}"""))
        assertFalse(LegadoLoginUiV2.isV2("""{"version":2.5}"""))
        assertFalse(LegadoLoginUiV2.isV2("""{"version":2147483648}"""))

        val integralCountdown = LegadoLoginUiV2.parseRows(
            """{"rows":[{"name":"Wait","type":"button","action":"wait","countdown":60.0}]}"""
        )
        assertEquals(60, integralCountdown?.single()?.countdown)

        val fractionalCountdown = LegadoLoginUiV2.parseRows(
            """{"rows":[{"name":"Wait","type":"button","action":"wait","countdown":60.5}]}"""
        )
        assertEquals(null, fractionalCountdown)
        assertEquals(
            null,
            LegadoLoginUiV2.parseRows(
                """{"rows":[{"name":"Wait","type":"button","action":"wait","countdown":2147483648}]}"""
            ),
        )
    }

    @Test
    fun rejectsDuplicateKeysActionsAndMalformedRows() {
        assertNull(LegadoLoginUiV2.parseRows("""{"rows":[
            {"name":"A","type":"text","key":"same"},
            {"name":"B","type":"password","key":"same"}
        ]}"""))
        assertNull(LegadoLoginUiV2.parseRows("""{"rows":[
            {"name":"A","type":"button","action":"same"},
            {"name":"B","type":"button","action":"same"}
        ]}"""))
        assertNull(LegadoLoginUiV2.parseRows("""{"rows":[{"name":"A","type":"select","key":"x","options":[]}]}"""))
        assertNull(LegadoLoginUiV2.parseRows("""{"rows":[{"name":"A","type":"unknown"}]}"""))
    }

    @Test
    fun parsesActionResultsAndKeepsUnknownCommandNames() {
        val result = LegadoLoginUiV2.parseActionResult(
            """{"state":{"step":2},"error":{"code":"验证码错误"},"login":{"cookie":"abc"},"close":true,"extra":1}"""
        )
        assertEquals("""{"step":2}""", result?.stateJson)
        assertEquals("验证码错误", result?.errors?.get("code"))
        assertEquals("""{"cookie":"abc"}""", result?.loginJson)
        assertTrue(result?.close == true)
        assertEquals(setOf("extra"), result?.unknownKeys)
        assertNull(LegadoLoginUiV2.parseActionResult("""{"close":"yes"}"""))
    }
}
