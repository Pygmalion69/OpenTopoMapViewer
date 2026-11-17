package org.nitri.opentopo.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class LocalMapTranslationsTest {
    @Test fun everyMaintainedLocaleHasAllLocalMapStringsAndMatchingArguments() {
        val res = listOf(File("src/main/res"), File("app/src/main/res")).first { it.isDirectory }
        fun strings(directory: File): Map<String, String> {
            val document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(File(directory, "strings.xml"))
            val nodes = document.getElementsByTagName("string")
            return (0 until nodes.length).associate { index ->
                val node = nodes.item(index)
                node.attributes.getNamedItem("name").nodeValue to node.textContent
            }
        }
        val base = strings(File(res, "values")).filterKeys { it.startsWith("local_") }
        assertEquals(16, base.size)
        val arguments = Regex("%[0-9]+\\$[ds]")
        res.listFiles()!!.filter { it.name.startsWith("values") && File(it, "strings.xml").exists() }
            .forEach { directory ->
                val translated = strings(directory)
                base.forEach { (key, value) ->
                    assertTrue("${directory.name}: missing $key", !translated[key].isNullOrBlank())
                    assertEquals("${directory.name}: $key arguments",
                        arguments.findAll(value).map { it.value }.toSet(),
                        arguments.findAll(translated.getValue(key)).map { it.value }.toSet())
                }
            }
    }
}
