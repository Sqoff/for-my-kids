package com.weparent.bbomakids;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import java.util.Map;

/**
 * 📍 위치 알림 (ADR 85): 진동·소리가 나는 '위치 알림' 채널.
 * 서버가 알림 메시지(notification)로 보내서 앱이 잠들어 있어도 시스템이 띄우고,
 * 앱을 보고 있을 땐 WidgetMessagingService 가 이 클래스로 버튼 달린 알림을 직접 띄움.
 */
public final class LocAlerts {
    public static final String CHANNEL = "loc_alert";
    static final long[] VIBRATE = {0, 450, 180, 450};
    /** 서버 알림 메시지의 tag (시스템이 띄운 것도 지울 수 있게) */
    static final String TAG_REQ = "loc_req";

    private LocAlerts() {}

    public static void ensureChannel(Context c) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm.getNotificationChannel(CHANNEL) != null) return;
        NotificationChannel ch = new NotificationChannel(CHANNEL, "위치 요청", NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("배우자가 위치를 물어보거나 보여줄 때 (진동)");
        ch.enableVibration(true);
        ch.setVibrationPattern(VIBRATE);
        ch.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
            new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
        nm.createNotificationChannel(ch);
        try { nm.deleteNotificationChannel("loc_request"); } catch (Exception ignored) {} // 예전 무진동 채널
    }

    public static void cancelRequest(Context c) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(LocationRequestActivity.NOTIF_REQ);
        nm.cancel(TAG_REQ, 0);
    }

    /** 앱을 보고 있을 때 온 서버 알림 메시지 → 같은 내용을 버튼까지 달아서 직접 띄움 */
    static void showForeground(Context c, String title, String body, Map<String, String> d) {
        ensureChannel(c);
        String kind = d.get("kind");
        int f = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        Intent open = new Intent(c, LocationRequestActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        for (Map.Entry<String, String> e : d.entrySet()) open.putExtra(e.getKey(), e.getValue());
        NotificationCompat.Builder b = new NotificationCompat.Builder(c, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_bboma)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVibrate(VIBRATE)
            .setDefaults(NotificationCompat.DEFAULT_SOUND)
            .setAutoCancel(true);
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if ("asked".equals(kind)) {
            Intent yes = new Intent(open).putExtra(LocationRequestActivity.EXTRA_ACCEPT, true);
            PendingIntent no = PendingIntent.getBroadcast(c, 21, new Intent(c, WidgetActionReceiver.class).setAction(WidgetActionReceiver.DECLINE), f);
            b.setContentIntent(PendingIntent.getActivity(c, 22, open, f))
                .addAction(0, "지금은 어려워요", no)
                .addAction(0, "15분 보여주기", PendingIntent.getActivity(c, 23, yes, f));
            nm.notify(LocationRequestActivity.NOTIF_REQ, b.build());
        } else if ("sharing".equals(kind)) {
            Intent back = new Intent(open).putExtra(LocationRequestActivity.EXTRA_BACK, true);
            b.setContentIntent(PendingIntent.getActivity(c, 24, back, f))
                .addAction(0, "내 위치도 보여주기", PendingIntent.getActivity(c, 25, new Intent(back).putExtra(LocationRequestActivity.EXTRA_ACCEPT, true), f));
            nm.notify(LocationRequestActivity.NOTIF_REQ + 1, b.build());
        } else {
            b.setContentIntent(PendingIntent.getActivity(c, 26, new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), f));
            nm.notify(LocationRequestActivity.NOTIF_REQ + 2, b.build());
        }
    }
}
