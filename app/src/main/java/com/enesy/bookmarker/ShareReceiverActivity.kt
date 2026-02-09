package com.enesy.bookmarker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import androidx.room.withTransaction
import com.enesy.bookmarker.data.Bookmark
import com.enesy.bookmarker.data.Folder
import com.enesy.bookmarker.util.ContentExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

class ShareReceiverActivity : ComponentActivity() {

    private val TAG = "BookmarkerShare"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent?.action != Intent.ACTION_SEND || intent.type != "text/plain") {
            finish()
            return
        }

        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (sharedText == null) {
            finish()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val url = extractUrl(sharedText)
            if (url == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ShareReceiverActivity, "No URL found", Toast.LENGTH_SHORT).show()
                    finish()
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(this@ShareReceiverActivity, "Analyzing link... please wait.", Toast.LENGTH_SHORT).show()
            }

            try {
                val result = ContentExtractor.extract(url)
                
                val db = (application as BookmarkerApp).database
                db.withTransaction {
                    val dao = db.bookmarkerDao()
                    val folders = dao.getFolders()
                    val folderId = if (folders.isEmpty()) {
                        dao.insertFolder(Folder(name = "All"))
                    } else {
                        folders.first().id
                    }

                    val bookmark = Bookmark(
                        url = url,
                        title = result.title,
                        originalTitle = result.title,
                        platform = "",
                        folderId = folderId,
                        isSummarized = false,
                        rawContent = result.rawContent,
                        thumbnail = result.imageUrl
                    )
                    dao.insertBookmark(bookmark)
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ShareReceiverActivity, "Saved: ${result.title}", Toast.LENGTH_SHORT).show()
                    showSaveNotification(result.title)
                    finish()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving bookmark transaction", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ShareReceiverActivity, "Failed to save bookmark", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun extractUrl(text: String): String? {
        val urlPattern = Pattern.compile(
            """(https?://)?(www\.)?[-a-zA-Z0-9@:%._\+~#=]{2,256}\.[a-z]{2,6}\b([-a-zA-Z0-9@:%_\+.~#?&//=]*)?"""
        )
        val matcher = urlPattern.matcher(text)
        return if (matcher.find()) matcher.group(0) else null
    }

    private fun showSaveNotification(title: String) {
        val channelId = "bookmark_saves"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Bookmark Saves"
            val descriptionText = "Notifications for saved bookmarks"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Bookmark Saved 📥")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Bookmark saved!", Toast.LENGTH_SHORT).show()
            return
        }
        with(NotificationManagerCompat.from(this)) {
            notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }
}
