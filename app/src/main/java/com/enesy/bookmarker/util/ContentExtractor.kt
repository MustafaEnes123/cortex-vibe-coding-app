package com.enesy.bookmarker.util

import androidx.compose.runtime.Stable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.HttpURLConnection
import java.net.URL

object ContentExtractor {

    private const val YOUTUBE_API_KEY = "AIzaSyDRGZhDboD6-eAbjROTur0O5LOYvgbX7Wk"

    suspend fun extract(url: String): ExtractedResult = withContext(Dispatchers.IO) {
        when {
            url.contains("youtube.com") || url.contains("youtu.be") -> extractYouTubeData(url)
            url.contains("reddit.com") -> extractRedditData(url)
            else -> extractWebData(url)
        }
    }

    private fun extractYouTubeData(url: String): ExtractedResult {
        return try {
            val videoId = extractVideoId(url)
            val apiUrl = "https://www.googleapis.com/youtube/v3/videos?part=snippet&id=$videoId&key=$YOUTUBE_API_KEY"

            val json = URL(apiUrl).readText()
            val jsonObject = JSONObject(json)
            val snippet = jsonObject.getJSONArray("items").getJSONObject(0).getJSONObject("snippet")
            val title = snippet.getString("title")
            val description = snippet.getString("description")
            val thumbnailUrl = snippet.getJSONObject("thumbnails").getJSONObject("high").getString("url")
            ExtractedResult(title, description, thumbnailUrl)
        } catch (e: Exception) {
            // Fallback to oEmbed
            try {
                val oembedUrl = "https://www.youtube.com/oembed?url=$url&format=json"
                val json = URL(oembedUrl).readText()
                val jsonObject = JSONObject(json)
                val title = jsonObject.getString("title")
                ExtractedResult(title, "Video Link: $title", null)
            } catch (e: Exception) {
                ExtractedResult(url, "", null)
            }
        }
    }

    private fun extractVideoId(url: String): String? {
        val pattern = "(?:https?://)?(?:www\\.)?(?:m\\.)?(?:youtube(?:-nocookie)?\\.com/(?:(?:watch\\?v=)|(?:embed/)|(?:v/))|youtu\\.be/)([\\w-]{11})(?:.+)?"
        val compiledPattern = java.util.regex.Pattern.compile(pattern)
        val matcher = compiledPattern.matcher(url)
        return if (matcher.find()) matcher.group(1) else null
    }

    private fun extractRedditData(url: String): ExtractedResult {
        return try {
            val jsonUrl = if (url.endsWith("/")) "${url.dropLast(1)}.json" else "$url.json"
            val connection = URL(jsonUrl).openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            val json = connection.inputStream.bufferedReader().readText()
            val jsonArray = org.json.JSONArray(json)
            val post = jsonArray.getJSONObject(0).getJSONObject("data").getJSONArray("children").getJSONObject(0).getJSONObject("data")
            val title = post.getString("title")
            val selftext = post.getString("selftext")
            ExtractedResult(title, selftext, null)
        } catch (e: Exception) {
            ExtractedResult(url, "", null)
        }
    }

    private fun extractWebData(url: String): ExtractedResult {
        return try {
            val doc = Jsoup.connect(url).userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36").get()
            val title = doc.select("title").text()
            val description = doc.select("meta[name=description]").attr("content").ifEmpty {
                doc.select("p").first()?.text() ?: ""
            }
            ExtractedResult(title, description, null)
        } catch (e: Exception) {
            ExtractedResult(url, "", null)
        }
    }
}

@Stable
data class ExtractedResult(
    val title: String, 
    val rawContent: String, 
    val imageUrl: String? = null
)
