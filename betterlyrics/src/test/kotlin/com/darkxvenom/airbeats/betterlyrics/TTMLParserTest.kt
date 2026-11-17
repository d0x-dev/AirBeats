package com.darkxvenom.airbeats.betterlyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TTMLParserTest {
    @Test
    fun testParseTTML() {
        val ttml = """
        <?xml version="1.0" encoding="utf-8"?>
        <tt xmlns="http://www.w3.org/ns/ttml">
          <body>
            <div>
              <p begin="00:00:01.000" end="00:00:05.000">
                <span begin="00:00:01.000" end="00:00:02.000">mi</span>
                <span begin="00:00:02.000" end="00:00:03.000">ne,</span>
              </p>
            </div>
          </body>
        </tt>
        """.trimIndent()

        val lines = TTMLParser.parseTTML(ttml)
        assertNotNull(lines)
        assertEquals(1, lines.size)
        assertTrue(lines[0].words.isNotEmpty())
        assertEquals("mine,", lines[0].words[0].text)
    }
}
