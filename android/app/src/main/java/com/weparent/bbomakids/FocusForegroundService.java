package com.weparent.bbomakids;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Vibrator;
import androidx.core.app.NotificationCompat;

public class FocusForegroundService extends Service {

    public static final String ACTION_START = "com.weparent.bbomakids.ACTION_START_FOCUS";
    public static final String ACTION_STOP = "com.weparent.bbomakids.ACTION_STOP_FOCUS";

    public static final String CHANNEL_ID = "bboma_focus_foreground_channel";
    public static final String COMPLETE_CHANNEL_ID = "bboma_focus_complete_channel";
    public static final int NOTIFICATION_ID = 9001;
    public static final int COMPLETE_NOTIFICATION_ID = 9002;

    private Handler timerHandler;
    private Runnable timerRunnable;
    private int secondsRemaining = 1500;
    private int totalSeconds = 1500;
    private String userName = "지은";
    private String userRole = "엄마";
    private boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
        timerHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }

        String action = intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopFocusTimer();
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        if (ACTION_START.equals(action)) {
            secondsRemaining = intent.getIntExtra("secondsLeft", 1500);
            totalSeconds = intent.getIntExtra("totalSeconds", 1500);
            userName = intent.getStringExtra("userName") != null ? intent.getStringExtra("userName") : "지은";
            userRole = intent.getStringExtra("userRole") != null ? intent.getStringExtra("userRole") : "엄마";

            startFocusTimer();
        }

        return START_STICKY;
    }

    private void startFocusTimer() {
        isRunning = true;
        Notification notification = buildOngoingNotification(formatTime(secondsRemaining));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isRunning) return;

                if (secondsRemaining > 0) {
                    secondsRemaining--;
                    // Update ongoing notification
                    updateNotification(formatTime(secondsRemaining));
                    timerHandler.postDelayed(this, 1000);
                } else {
                    // Completed!
                    onFocusCompleted();
                }
            }
        };

        timerHandler.postDelayed(timerRunnable, 1000);
    }

    private void stopFocusTimer() {
        isRunning = false;
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }

    private void onFocusCompleted() {
        stopFocusTimer();
        stopForeground(true);

        // Gentle Vibration on complete
        try {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                v.vibrate(500);
            }
        } catch (Exception ignored) {}

        // Show Complete Notification
        int mins = Math.max(1, totalSeconds / 60);
        Notification completeNotif = new NotificationCompat.Builder(this, COMPLETE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_bboma)
                .setContentTitle("🎉 [뽀마키즈] 육아 집중 완주 성공!")
                .setContentText(userName + "(" + userRole + ")님, " + mins + "분 동안 아이에게 온전히 집중하셨습니다! (+50 EXP)")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(createContentIntent())
                .build();

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(COMPLETE_NOTIFICATION_ID, completeNotif);
        }

        stopSelf();
    }

    private Notification buildOngoingNotification(String timeFormatted) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_bboma)
                .setContentTitle("👶 뽀마키즈 | " + userName + "(" + userRole + ") 육아 집중 가동 중")
                .setContentText("아이와 눈맞춤 집중 중 • 남은 시간: " + timeFormatted)
                .setSubText("동작 중")
                .setTicker("👶 뽀마키즈 육아 집중 모드 동작 중")
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void updateNotification(String timeFormatted) {
        Notification notification = buildOngoingNotification(timeFormatted);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, notification);
        }
    }

    private PendingIntent createContentIntent() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );
    }

    private String formatTime(int totalSecs) {
        int m = totalSecs / 60;
        int s = totalSecs % 60;
        return String.format("%02d:%02d", m, s);
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null) return;

            // 1. Ongoing Foreground Channel
            NotificationChannel focusChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "뽀마키즈 육아 집중 상주 알림",
                    NotificationManager.IMPORTANCE_LOW
            );
            focusChannel.setDescription("육아 집중 모드 실행 중 상단 상태바에 상주하며 남은 시간을 표시합니다.");
            focusChannel.setShowBadge(true);
            manager.createNotificationChannel(focusChannel);

            // 2. Complete Alert Channel
            NotificationChannel completeChannel = new NotificationChannel(
                    COMPLETE_CHANNEL_ID,
                    "뽀마키즈 육아 집중 완료 알림",
                    NotificationManager.IMPORTANCE_HIGH
            );
            completeChannel.setDescription("육아 집중 시간 완주 시 알림을 보냅니다.");
            completeChannel.enableVibration(true);
            manager.createNotificationChannel(completeChannel);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
