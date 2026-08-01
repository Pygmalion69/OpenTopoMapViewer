package org.nitri.opentopo.nearby

import org.junit.Assert.assertEquals
import org.junit.Test

class WikiRequestHeadersTest {

    @Test
    fun testUserAgent() {
        val versionName = "1.33.1"
        val expected = "OpenTopoMapViewer/1.33.1 (https://github.com/Pygmalion69/OpenTopoMapViewer)"
        assertEquals(expected, WikiRequestHeaders.userAgent(versionName))
    }
}
