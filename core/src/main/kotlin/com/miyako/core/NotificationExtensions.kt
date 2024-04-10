package com.miyako.core

import android.app.NotificationChannel
import android.os.Build
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat


@RequiresApi(Build.VERSION_CODES.O)
fun NotificationManagerCompat.createChannel(
  channelId: String,
  channelName: String,
  description: String,
  importance: Int,
  silent: Boolean
): NotificationChannel {
  val notificationChannel = getNotificationChannel(channelId).init {
    NotificationChannel(channelId, channelName, importance)
  }

  notificationChannel.description = description
  notificationChannel.setShowBadge(true)

  if (silent) {
    notificationChannel.enableVibration(false)
    notificationChannel.setSound(null, null)
    createNotificationChannel(notificationChannel)
  }
  return notificationChannel
}

/**
 * Android 9以下，设置小布局和大布局。如果只想通过 setCustomContentView() 设置大布局会有适配问题。
 * Android 9-11，设置大布局。可以通过 setCustomContentView() 直接设置大布局，无适配问题。
 * Android 12及以上，设置小布局和大布局。如果只想通过 setCustomContentView() 设置大布局会有适配问题。
 * */
fun NotificationCompat.Builder.applyRemoteViewCompat(smallRemoteViews: RemoteViews, bigRemoteViews: RemoteViews?) {
  when {
    Build.VERSION.SDK_INT < Build.VERSION_CODES.P -> {
      setCustomContentView(smallRemoteViews)
      setCustomBigContentView(bigRemoteViews)
      // 设置从屏幕顶部弹出来的自定义布局，此处应设置小布局，否则在 Android 8 上会显示不全。
      // 另外经测试在 Android 7、6、5 不会在屏幕顶部弹出该方法设置的布局。
      setCustomHeadsUpContentView(smallRemoteViews)
    }

    Build.VERSION.SDK_INT in Build.VERSION_CODES.P..Build.VERSION_CODES.R -> {
      setCustomContentView(bigRemoteViews)
      // 设置从屏幕顶部弹出来的自定义布局
      setCustomHeadsUpContentView(bigRemoteViews)
    }

    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      setCustomContentView(smallRemoteViews)
      setCustomBigContentView(bigRemoteViews)
      // 设置从屏幕顶部弹出来的自定义布局, 此处应设置小布局，否则会显示不全
      setCustomHeadsUpContentView(smallRemoteViews)
    }
  }
}

/**
 * 自定义通知想设置 badge 必须设置以下参数
 * */
fun NotificationCompat.Builder.addBadgeForCustom(
  title: String = "",
  content: String = ""
) {
  setContentTitle(title)
  setContentText(content)
  setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
}
