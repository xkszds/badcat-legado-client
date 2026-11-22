package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals

class PaginationUrlIdentityTest {
    @Test
    fun removesOnlyFragmentForRelativeAndNonHttpUrls() {
        assertEquals("/chapter/1", paginationUrlIdentity("/chapter/1#part"))
        assertEquals("mailto:reader@example.test", paginationUrlIdentity("mailto:reader@example.test#top"))
    }

    @Test
    fun normalizesHttpSchemeHostAndDefaultPorts() {
        assertEquals(
            "https://reader.example/chapter/1?token=A%2Fb",
            paginationUrlIdentity("HTTPS://READER.EXAMPLE:443/chapter/1?token=A%2Fb#page"),
        )
        assertEquals(
            "http://reader.example:8080/list?page=2",
            paginationUrlIdentity("http://READER.EXAMPLE:8080/list?page=2#top"),
        )
    }

    @Test
    fun preservesQueryOrderingAndNonDefaultPorts() {
        assertEquals(
            "https://reader.example:8443/list?a=1&b=2",
            paginationUrlIdentity("https://reader.example:8443/list?a=1&b=2#x"),
        )
        assertEquals(
            "https://reader.example/list?b=2&a=1",
            paginationUrlIdentity("https://reader.example/list?b=2&a=1"),
        )
    }

    @Test
    fun malformedAndCredentialBearingUrlsUseConservativeFallback() {
        assertEquals("https:///missing", paginationUrlIdentity("https:///missing#x"))
        assertEquals(
            "https://user:pass@reader.example/chapter",
            paginationUrlIdentity("https://user:pass@reader.example/chapter#x"),
        )
    }
}
