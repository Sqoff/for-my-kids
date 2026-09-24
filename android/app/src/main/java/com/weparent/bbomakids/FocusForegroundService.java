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
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.app.NotificationCompat;

public class FocusForegroundService extends Service {

    public static final String ACTION_START = "com.weparent.bbomakids.ACTION_START_FOCUS";
    public static final String ACTION_STOP = "com.weparent.bbomakids.ACTION_STOP_FOCUS";

    public static final String CHANNEL_ID = "bboma_focus_foreground_v4";
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

    // Floating Red Status Bar Overlay View (Matches eee.jpg reference)
    private WindowManager windowManager;
    private View overlayCapsuleView;
    private TextView overlayTextView;

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

        // Show prominent red capsule overlay directly over top status bar
        showStatusBarOverlay();

        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isRunning) return;

                if (secondsRemaining > 0) {
                    secondsRemaining--;
                    String formatted = formatTime(secondsRemaining);
                    // Update ongoing notification
                    updateNotification(formatted);
                    // Update floating red status bar capsule
                    updateStatusBarOverlay(formatted);
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
        removeStatusBarOverlay();
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
        stopForeground(true);
        super.onDestroy();
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

    private Notification buildOngoingNotification(String timeFormatted) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_bboma)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setColor(0xFFFF1744) // Vivid Crimson Red Badge
                .setColorized(true)
                .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
                .setContentTitle("🔴 [뽀마키즈] " + userName + "(" + userRole + ") 육아 집중 가동 중")
                .setContentText("아이와 눈맞춤 집중 중 • 남은 시간: " + timeFormatted)
                .setSubText("🔴 동작 중")
                .setTicker("🔴 뽀마키즈 육아 집중 모드 동작 중")
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setBadgeIconType(NotificationCompat.BADGE_ICON_LARGE)
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

            // 1. Ongoing Foreground Channel - HIGH Importance with Red Light
            NotificationChannel focusChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "뽀마키즈 육아 집중 상주 알림",
                    NotificationManager.IMPORTANCE_HIGH
            );
            focusChannel.setDescription("육아 집중 모드 실행 중 상단 상태바에 상주하며 남은 시간을 표시합니다.");
            focusChannel.setShowBadge(true);
            focusChannel.setSound(null, null);
            focusChannel.enableVibration(false);
            focusChannel.enableLights(true);
            focusChannel.setLightColor(0xFFFF1744);
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

    /**
     * Shows a prominent vivid red pill/capsule directly on top of the status bar next to the clock,
     * matching the exact reference style from eee.jpg.
     */
    private void showStatusBarOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                return;
            }
        }

        if (overlayCapsuleView != null) {
            return;
        }

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
                    if (windowManager == null) return;

                    LinearLayout capsule = new LinearLayout(FocusForegroundService.this);
                    capsule.setOrientation(LinearLayout.HORIZONTAL);
                    capsule.setGravity(Gravity.CENTER_VERTICAL);

                    int padH = dpToPx(8);
                    int padV = dpToPx(3);
                    capsule.setPadding(padH, padV, padH, padV);

                    // Solid Red Capsule Background (Exact match to eee.jpg red badge)
                    GradientDrawable bg = new GradientDrawable();
                    bg.setShape(GradientDrawable.RECTANGLE);
                    bg.setColor(Color.parseColor("#E50914")); // Vivid Red
                    bg.setCornerRadius(dpToPx(12));
                    bg.setStroke(dpToPx(1), Color.parseColor("#FF5252"));
                    capsule.setBackground(bg);

                    // Text: 🔴 뽀마 25:00
                    overlayTextView = new TextView(FocusForegroundService.this);
                    overlayTextView.setText("🔴 뽀마 " + formatTime(secondsRemaining));
                    overlayTextView.setTextColor(Color.WHITE);
                    overlayTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
                    overlayTextView.setTypeface(null, android.graphics.Typeface.BOLD);
                    capsule.addView(overlayTextView);

                    int layoutType = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                            : WindowManager.LayoutParams.TYPE_PHONE;

                    int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;

                    final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                            WindowManager.LayoutParams.WRAP_CONTENT,
                            WindowManager.LayoutParams.WRAP_CONTENT,
                            layoutType,
                            flags,
                            PixelFormat.TRANSLUCENT
                    );

                    params.gravity = Gravity.TOP | Gravity.START;
                    // Positioned at top-left status bar area next to the clock
                    params.x = dpToPx(65);
                    params.y = dpToPx(3);

                    capsule.setOnTouchListener(new View.OnTouchListener() {
                        private int initialX, initialY;
                        private float initialTouchX, initialTouchY;
                        private boolean isMoved = false;

                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            switch (event.getAction()) {
                                case MotionEvent.ACTION_DOWN:
                                    initialX = params.x;
                                    initialY = params.y;
                                    initialTouchX = event.getRawX();
                                    initialTouchY = event.getRawY();
                                    isMoved = false;
                                    return true;
                                case MotionEvent.ACTION_MOVE:
                                    int dx = (int) (event.getRawX() - initialTouchX);
                                    int dy = (int) (event.getRawY() - initialTouchY);
                                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                                        isMoved = true;
                                        params.x = initialX + dx;
                                        params.y = initialY + dy;
                                        windowManager.updateViewLayout(capsule, params);
                                    }
                                    return true;
                                case MotionEvent.ACTION_UP:
                                    if (!isMoved) {
                                        Intent appIntent = new Intent(FocusForegroundService.this, MainActivity.class);
                                        appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                        startActivity(appIntent);
                                    }
                                    return true;
                            }
                            return false;
                        }
                    });

                    overlayCapsuleView = capsule;
                    windowManager.addView(overlayCapsuleView, params);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void updateStatusBarOverlay(final String timeText) {
        if (overlayTextView != null) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    if (overlayTextView != null) {
                        overlayTextView.setText("🔴 뽀마 " + timeText);
                    }
                }
            });
        }
    }

    private void removeStatusBarOverlay() {
        if (windowManager != null && overlayCapsuleView != null) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (windowManager != null && overlayCapsuleView != null) {
                            windowManager.removeView(overlayCapsuleView);
                            overlayCapsuleView = null;
                            overlayTextView = null;
                        }
                    } catch (Exception ignored) {}
                }
            });
        }
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
