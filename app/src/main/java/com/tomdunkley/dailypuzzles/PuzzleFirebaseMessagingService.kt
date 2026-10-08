package com.tomdunkley.dailypuzzles

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.tomdunkley.dailypuzzles.data.auth.AuthRepository
import com.tomdunkley.dailypuzzles.data.network.dto.RegisterFcmTokenRequestDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PuzzleFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                AuthRepository.apiServiceForExistingSession()
                    ?.registerFcmToken(RegisterFcmTokenRequestDto(token))
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: return
        val body = message.notification?.body ?: ""
        val game = message.data["game"]
        val navigateTo = message.data["navigate_to"]
        showNotification(title, body, game, navigateTo)
    }

    private fun gameDrawableRes(game: String?): Int? = when (game) {
        "boggle" -> R.drawable.ic_notif_words
        "numbers" -> R.drawable.ic_notif_numbers
        "routes" -> R.drawable.ic_notif_routes
        else -> null
    }

    private fun drawableToBitmap(context: Context, resId: Int): Bitmap? {
        val drawable = ContextCompat.getDrawable(context, resId) ?: return null
        val size = (context.resources.displayMetrics.density * 40).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        return bitmap
    }

    private fun showNotification(title: String, body: String, game: String?, navigateTo: String?) {
        val channelId = "challenges"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(channelId, "Challenges", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (navigateTo != null) putExtra("navigate_to", navigateTo)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        gameDrawableRes(game)?.let { resId ->
            drawableToBitmap(this, resId)?.let { builder.setLargeIcon(it) }
        }

        manager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
