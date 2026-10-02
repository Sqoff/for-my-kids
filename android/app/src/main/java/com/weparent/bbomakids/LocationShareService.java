package com.weparent.bbomakids;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import com.google.firebase.firestore.FieldValue;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 📍 15분 위치 공유 (ADR 68). '보여주기'를 누른 사람의 폰에서만 돌고, 위쪽 알림이 떠 있는 동안 1분마다 정확한 위치를 가족 문서 loc.pos 에 씀.
 * 15분이 지나거나 '그만'을 누르면 loc.pos 를 지우고 끝냄 (기록을 남기지 않음).
 */
public class LocationShareService extends Service implements LocationListener {
    public static final String ACTION_START = "com.weparent.bbomakids.LOC_START";
    public static final String ACTION_STOP = "com.weparent.bbomakids.LOC_STOP";
    public static final long SHARE_MS = 15L * 60 * 1000;
    /** true = 물어본 사람이 '내 위치도 보여주기'를 누름 → loc.back / loc.backPos 에 씀 (ADR 85) */
    public static final String EXTRA_BACK = "back";
    private static final String CHANNEL = "loc_share";
    private static final int NOTIF_ID = 4815;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private LocationManager lm;
    private long until;
    private long lastSent;
    private boolean running;
    private boolean back;

    public static void start(Context c) { start(c, false); }

    public static void start(Context c, boolean back) {
        Intent i = new Intent(c, LocationShareService.class).setAction(ACTION_START).putExtra(EXTRA_BACK, back);
        ContextCompat.startForegroundService(c, i);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();
        if (ACTION_STOP.equals(action)) { finish(true); return START_NOT_STICKY; }
        if (running) return START_NOT_STICKY;
        running = true;
        back = intent != null && intent.getBooleanExtra(EXTRA_BACK, false);
        until = System.currentTimeMillis() + SHARE_MS;
        // 되돌려 보여주기는 상대 공유가 끝나는 시각에 같이 끝냄
        if (back && WidgetStore.locUntil(this) > System.currentTimeMillis()) until = Math.min(until, WidgetStore.locUntil(this));
        startInForeground();
        Map<String, Object> f = new HashMap<>();
        if (back) {
            f.put("loc.back.state", "sharing");
            f.put("loc.back.until", until);
            WidgetStore.saveBack(this, "sharing", until, null, 0, 0);
        } else {
            f.put("loc.req.state", "sharing");
            f.put("loc.req.until", until);
            WidgetStore.saveLocState(this, "sharing", until);
        }
        f.put("updatedBy", WidgetStore.role(this));
        FamilyDoc.update(this, f);
        WidgetStore.refreshAll(this);
        startUpdates();
        handler.postDelayed(() -> finish(true), Math.max(1000L, until - System.currentTimeMillis()));
        return START_NOT_STICKY;
    }

    private void startInForeground() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(new NotificationChannel(CHANNEL, "위치 공유 중", NotificationManager.IMPORTANCE_LOW));
        }
        int f = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent stop = PendingIntent.getService(this, 7, new Intent(this, LocationShareService.class).setAction(ACTION_STOP), f);
        String spouse = WidgetStore.name(this, FamilyDoc.spouse(WidgetStore.role(this)));
        Notification n = new NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_bboma)
            .setContentTitle("📍 " + spouse + "님에게 " + (back ? "내 위치도 " : "위치를 ") + "보여주는 중")
            .setContentText(hhmm(until) + "까지 · 끝나면 위치는 지워져요")
            .setOngoing(true)
            .addAction(0, "그만", stop)
            .build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        else startForeground(NOTIF_ID, n);
    }

    private void startUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            finish(true);
            return;
        }
        lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        try {
            Location best = null;
            for (String p : new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER}) {
                if (!lm.isProviderEnabled(p)) continue;
                Location last = lm.getLastKnownLocation(p);
                if (last != null && (best == null || last.getTime() > best.getTime())) best = last;
                lm.requestLocationUpdates(p, 60_000L, 0f, this, Looper.getMainLooper());
            }
            if (best != null) onLocationChanged(best);
        } catch (SecurityException ignored) {
            finish(true);
        }
    }

    @Override
    public void onLocationChanged(Location loc) {
        long now = System.currentTimeMillis();
        if (now - lastSent < 30_000L && lastSent != 0) return; // 30초에 한 번까지만
        lastSent = now;
        final double lat = loc.getLatitude(), lng = loc.getLongitude();
        final float acc = loc.getAccuracy();
        new Thread(() -> {
            String addr = "";
            try {
                if (Geocoder.isPresent()) {
                    List<Address> list = new Geocoder(this, Locale.KOREA).getFromLocation(lat, lng, 1);
                    if (list != null && !list.isEmpty()) addr = shortAddress(list.get(0));
                }
            } catch (Exception ignored) {}
            Map<String, Object> pos = new HashMap<>();
            pos.put("lat", lat);
            pos.put("lng", lng);
            pos.put("acc", Math.round(acc));
            pos.put("addr", addr);
            pos.put("at", System.currentTimeMillis());
            Map<String, Object> f = new HashMap<>();
            f.put(back ? "loc.backPos" : "loc.pos", pos);
            f.put("updatedBy", WidgetStore.role(this));
            FamilyDoc.update(this, f);
        }).start();
    }

    /** "경기도 성남시 분당구 판교역로 166" → "분당구 판교역로 166" 처럼 짧게 */
    static String shortAddress(Address a) {
        String line = a.getAddressLine(0);
        if (line == null) return "";
        line = line.replace("대한민국 ", "").trim();
        String[] parts = line.split(" ");
        return parts.length > 3 ? String.join(" ", java.util.Arrays.copyOfRange(parts, parts.length - 3, parts.length)) : line;
    }

    private void finish(boolean writeEnd) {
        handler.removeCallbacksAndMessages(null);
        if (lm != null) { try { lm.removeUpdates(this); } catch (Exception ignored) {} }
        if (writeEnd && running) {
            Map<String, Object> f = new HashMap<>();
            if (back) {
                f.put("loc.back.state", "ended");
                f.put("loc.backPos", FieldValue.delete());
                WidgetStore.saveBack(this, null, 0, null, 0, 0);
            } else {
                f.put("loc.req.state", "ended");
                f.put("loc.pos", FieldValue.delete());
                WidgetStore.saveLocState(this, "ended", 0);
            }
            f.put("updatedBy", WidgetStore.role(this));
            FamilyDoc.update(this, f);
            WidgetStore.refreshAll(this);
        }
        running = false;
        stopForeground(true);
        stopSelf();
    }

    private static String hhmm(long ms) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(ms);
        return String.format(Locale.KOREA, "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
    }

    @Override public void onProviderEnabled(String p) {}
    @Override public void onProviderDisabled(String p) {}
    @Override public void onStatusChanged(String p, int s, android.os.Bundle b) {}
    @Override public IBinder onBind(Intent intent) { return null; }
}
