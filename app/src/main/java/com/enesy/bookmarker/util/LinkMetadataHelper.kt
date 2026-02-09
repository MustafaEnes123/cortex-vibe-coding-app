package com.enesy.bookmarker.util

import com.enesy.bookmarker.data.LinkPreviewData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.IOException
import java.net.URL

class LinkMetadataHelper {

    suspend fun getLinkPreviewData(url: String): LinkPreviewData {
        return withContext(Dispatchers.IO) {
            when {
                isYoutubeUrl(url) -> getYoutubeLinkPreviewData(url)
                isRedditUrl(url) -> getRedditLinkPreviewData(url)
                isXUrl(url) -> getXLinkPreviewData(url)
                isInstagramUrl(url) -> getInstagramLinkPreviewData(url)
                else -> getGenericLinkPreviewData(url)
            }
        }
    }

    suspend fun fetchCleanContent(url: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val doc = Jsoup.connect(url).get()

                // 1. Look for semantic tags first
                var mainContent: Element? = doc.select("article").first()
                if (mainContent == null) {
                    mainContent = doc.select("[role=main]").first()
                }

                // 2. If not found, find the div with the most <p> tags
                if (mainContent == null) {
                    mainContent = doc.select("div").maxByOrNull { it.select("p").size }
                }

                // 3. Extract text and add double newlines
                val text = mainContent?.select("p")?.joinToString("\n\n") { it.text() }

                if (text.isNullOrBlank()) {
                    Result.failure(Exception("Could not extract main content."))
                } else {
                    Result.success(text)
                }
            } catch (e: IOException) {
                Result.failure(e)
            }
        }
    }

    private fun isYoutubeUrl(url: String): Boolean {
        // Correctly escaped regex special characters.
        val pattern = """^(https?://)?(www\.)?(youtube\.com|youtu\.be)/.+""".toRegex()
        return pattern.matches(url)
    }

    private fun isRedditUrl(url: String): Boolean {
        return url.contains("reddit.com")
    }

    private fun isXUrl(url: String): Boolean {
        return url.contains("x.com") || url.contains("twitter.com")
    }

    private fun isInstagramUrl(url: String): Boolean {
        return url.contains("instagram.com")
    }

    private fun getYoutubeLinkPreviewData(url: String): LinkPreviewData {
        val videoId = extractYoutubeVideoId(url)
        val thumbnailUrl = "https://img.youtube.com/vi/$videoId/maxresdefault.jpg"
        // We can\'t easily get the title/description without an API call,
        // so we\'ll use the URL as the title and provide a generic description.
        return LinkPreviewData(
            url = url,
            title = url,
            description = "YouTube Video",
            imageUrl = thumbnailUrl,
            siteName = "YouTube"
        )
    }

    private fun extractYoutubeVideoId(url: String): String? {
        // Correctly escaped regex special characters.
        val pattern =
            """(?<=watch\?v=|/videos/|embed/|youtu\.be/|/v/|/e/|watch\?v%3D|watch\?feature=player_embedded&v=|%2Fvideos%2F|embed%2Fvideos%2F|youtu\.be/|/v/)[^#&?]*""".toRegex()
        return pattern.find(url)?.value
    }

    private fun getRedditLinkPreviewData(url: String): LinkPreviewData {
        return try {
            val jsonUrl = if (url.endsWith(".json")) url else "${url.removeSuffix("/")}.json"
            val jsonString = URL(jsonUrl).readText()
            val postData = org.json.JSONArray(jsonString)
                .getJSONObject(0)
                .getJSONObject("data")
                .getJSONArray("children")
                .getJSONObject(0)
                .getJSONObject("data")

            val title = postData.getString("title")

            LinkPreviewData(
                url = url,
                title = title,
                description = "",
                imageUrl = null,
                siteName = "Reddit"
            )
        } catch (e: Exception) {
            getGenericLinkPreviewData(url) // Fallback to generic scraper
        }
    }

    private fun getXLinkPreviewData(url: String): LinkPreviewData {
        return try {
            val oEmbedUrl = "https://publish.twitter.com/oembed?url=$url"
            val jsonString = URL(oEmbedUrl).readText()
            val json = JSONObject(jsonString)

            val authorName = json.optString("author_name", "X")
            val html = json.getString("html")
            // Use Jsoup to parse the HTML snippet and extract text
            val title = Jsoup.parse(html).text()

            LinkPreviewData(
                url = url,
                title = title,
                description = "", // Keep description empty as per instruction
                imageUrl = null, // No reliable image from oEmbed
                siteName = authorName
            )
        } catch (e: Exception) {
            getGenericLinkPreviewData(url) // Fallback
        }
    }

    private fun getInstagramLinkPreviewData(url: String): LinkPreviewData {
        return try {
            val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36"
            val doc = Jsoup.connect(url).userAgent(userAgent).get()

            val imageUrl = doc.select("meta[property=og:image]").attr("content")
            val ogDescription = doc.select("meta[property=og:description]").attr("content")

            val title = if (ogDescription.isNotEmpty()) ogDescription else doc.title()

            LinkPreviewData(
                url = url,
                title = title,
                description = "", // Not requested
                imageUrl = if (imageUrl.isNotEmpty()) imageUrl else null,
                siteName = "Instagram"
            )
        } catch (e: Exception) {
            getGenericLinkPreviewData(url)
        }
    }

    private fun getGenericLinkPreviewData(url: String): LinkPreviewData {
        return try {
            val doc = Jsoup.connect(url).get()
            val title = doc.select("meta[property=og:title]").attr("content")
            val description = doc.select("meta[property=og:description]").attr("content")
            val imageUrl = doc.select("meta[property=og:image]").attr("content")
            val siteName = doc.select("meta[property=og:site_name]").attr("content")

            LinkPreviewData(
                url = url,
                title = if (title.isNotEmpty()) title else doc.title(),
                description = description,
                imageUrl = imageUrl,
                siteName = siteName
            )
        } catch (e: IOException) {
            LinkPreviewData(
                url = url,
                title = url,
                description = "Could not fetch preview",
                imageUrl = null,
                siteName = null
            )
        }
    }
}
