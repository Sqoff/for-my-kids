package com.weparent.bbomakids;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Vibrator;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import androidx.core.app.NotificationCompat;
import androidx.media.app.NotificationCompat.MediaStyle;

public class FocusForegroundService extends Service {

    public static final String ACTION_START = "com.weparent.bbomakids.ACTION_START_FOCUS";
    public static final String ACTION_STOP = "com.weparent.bbomakids.ACTION_STOP_FOCUS";
    public static final String ACTION_PAUSE = "com.weparent.bbomakids.ACTION_PAUSE_FOCUS";
    public static final String ACTION_RESUME = "com.weparent.bbomakids.ACTION_RESUME_FOCUS";

    public static final String CHANNEL_ID = "bboma_focus_media_session_v1";
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
    private boolean isPaused = false;

    // Official Android MediaSession for system-level status bar / quick settings media player integration
    private MediaSessionCompat mediaSession;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
        initMediaSession();
        timerHandler = new Handler(Looper.getMainLooper());
    }

    private void initMediaSession() {
        mediaSession = new MediaSessionCompat(this, "BbomaFocusMediaSession");
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPause() {
                pauseFocusTimer();
            }

            @Override
            public void onPlay() {
                resumeFocusTimer();
            }

            @Override
            public void onStop() {
                stopFocusTimer();
                stopForeground(true);
                stopSelf();
            }
        });

        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING);
        mediaSession.setActive(true);
    }

    private void updatePlaybackState(int state) {
        if (mediaSession == null) return;

        long actions = PlaybackStateCompat.ACTION_PLAY
                | PlaybackStateCompat.ACTION_PAUSE
                | PlaybackStateCompat.ACTION_PLAY_PAUSE
                | PlaybackStateCompat.ACTION_STOP;

        long position = (totalSeconds - secondsRemaining) * 1000L;

        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(actions)
                .setState(state, position, state == PlaybackStateCompat.STATE_PLAYING ? 1.0f : 0.0f);

        mediaSession.setPlaybackState(stateBuilder.build());
    }

    private void updateMediaMetadata() {
        if (mediaSession == null) return;

        MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "👶 육아 집중 모드 (" + formatTime(secondsRemaining) + ")")
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, userName + "(" + userRole + ") • 아이와 함께하는 시간")
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "뽀마키즈 부부 평화 육아")
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, totalSeconds * 1000L);

        try {
            metadataBuilder.putBitmap(
                    MediaMetadataCompat.METADATA_KEY_ALBUM_ART,
                    BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher)
            );
        } catch (Exception ignored) {}

        mediaSession.setMetadata(metadataBuilder.build());
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

        if (ACTION_PAUSE.equals(action)) {
            pauseFocusTimer();
            return START_STICKY;
        }

        if (ACTION_RESUME.equals(action)) {
            resumeFocusTimer();
            return START_STICKY;
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
        isPaused = false;
        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING);
        updateMediaMetadata();

        Notification notification = buildMediaNotification(formatTime(secondsRemaining));

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
                if (!isRunning || isPaused) return;

                if (secondsRemaining > 0) {
                    secondsRemaining--;
                    String formatted = formatTime(secondsRemaining);
                    updateMediaMetadata();
                    updateNotification(formatted);
                    timerHandler.postDelayed(this, 1000);
                } else {
                    onFocusCompleted();
                }
            }
        };

        timerHandler.postDelayed(timerRunnable, 1000);
    }

    private void pauseFocusTimer() {
        isPaused = true;
        updatePlaybackState(PlaybackStateCompat.STATE_PAUSED);
        updateNotification(formatTime(secondsRemaining));
    }

    private void resumeFocusTimer() {
        isPaused = false;
        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING);
        updateNotification(formatTime(secondsRemaining));
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
            timerHandler.postDelayed(timerRunnable, 1000);
        }
    }

    private void stopFocusTimer() {
        isRunning = false;
        isPaused = false;
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
        if (mediaSession != null) {
            mediaSession.setActive(false);
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        stopFocusTimer();
        stopForeground(true);
        stopSelf();
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        stopFocusTimer();
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }
        stopForeground(true);
        super.onDestroy();
    }

    private void onFocusCompleted() {
        stopFocusTimer();
        stopForeground(true);

        try {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                v.vibrate(500);
            }
        } catch (Exception ignored) {}

        int mins = Math.max(1, totalSeconds / 60);
        Notification completeNotif = new NotificationCompat.Builder(this, COMPLETE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_bboma)
                .setColor(0xFF10B981) // Green badge
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

    private Notification buildMediaNotification(String timeFormatted) {
        PendingIntent contentPendingIntent = createContentIntent();

        // Stop Action Intent
        Intent stopIntent = new Intent(this, FocusForegroundService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this, 1, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        // Pause / Resume Action Intent
        Intent toggleIntent = new Intent(this, FocusForegroundService.class);
        toggleIntent.setAction(isPaused ? ACTION_RESUME : ACTION_PAUSE);
        PendingIntent togglePendingIntent = PendingIntent.getService(
                this, 2, toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        NotificationCompat.Action toggleAction = new NotificationCompat.Action.Builder(
                isPaused ? android.R.drawable.ic_media_play : android.R.drawable.ic_media_pause,
                isPaused ? "계속하기" : "일시정지",
                togglePendingIntent
        ).build();

        NotificationCompat.Action stopAction = new NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_close_clear_cancel,
                "집중 종료",
                stopPendingIntent
        ).build();

        // Official Android MediaStyle
        MediaStyle mediaStyle = new MediaStyle();
        if (mediaSession != null) {
            mediaStyle.setMediaSession(mediaSession.getSessionToken());
        }
        mediaStyle.setShowActionsInCompactView(0, 1);
        mediaStyle.setShowCancelButton(true);
        mediaStyle.setCancelButtonIntent(stopPendingIntent);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_bboma)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setColor(0xFFFF5252) // Theme Primary Coral Red
                .setColorized(true)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setContentTitle("👶 [뽀마키즈] 육아 집중 중 (" + timeFormatted + ")")
                .setContentText(userName + "(" + userRole + ")님 • 아이와 소중한 눈맞춤 중")
                .setSubText("남은 시간 " + timeFormatted)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .addAction(toggleAction)
                .addAction(stopAction)
                .setStyle(mediaStyle)
                .setContentIntent(contentPendingIntent)
                .build();
    }

    private void updateNotification(String timeFormatted) {
        Notification notification = buildMediaNotification(timeFormatted);
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

            // 1. Media Session Foreground Channel
            NotificationChannel focusChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "뽀마키즈 육아 집중 미디어 세션",
                    NotificationManager.IMPORTANCE_LOW
            );
            focusChannel.setDescription("육아 집중 모드 실행 중 상태바 및 미디어 컨트롤러를 통해 실시간 진행 상황을 표시합니다.");
            focusChannel.setShowBadge(true);
            focusChannel.setSound(null, null);
            focusChannel.enableVibration(false);
            focusChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
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
