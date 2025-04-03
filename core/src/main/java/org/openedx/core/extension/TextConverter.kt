package org.openedx.core.extension

import android.util.Patterns
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.openedx.core.config.Config

object TextConverter : KoinComponent {

    private val config by inject<Config>()

    fun htmlTextToLinkedText(html: String): LinkedText {
        val doc: Document =
            Jsoup.parse(html)
        val links: Elements = doc.select("a[href]")
        val text = doc.text()
        val linksMap = mutableMapOf<String, String>()
        for (link in links) {
            var resultLink = if (link.attr("href").isNotEmpty() && link.attr("href")[0] == '/') {
                link.attr("href").substring(1)
            } else {
                link.attr("href")
            }
            if (!resultLink.startsWith("http")) {
                resultLink = config.getApiHostURL() + resultLink
            }
            if (resultLink.isNotEmpty() && isLinkValid(resultLink)) {
                linksMap[link.text()] = resultLink
            }
        }
        return LinkedText(text, linksMap.toMap())
    }

    fun isLinkValid(link: String) = Patterns.WEB_URL.matcher(link.lowercase()).matches()
}

data class LinkedText(
    val text: String,
    val links: Map<String, String>
)
