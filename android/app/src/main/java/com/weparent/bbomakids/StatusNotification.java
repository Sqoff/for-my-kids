package com.weparent.bbomakids;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.core.app.NotificationCompat;

/**
 * 🔒 잠금화면 상태 카드 (ADR 87).
 * 삼성 잠금화면은 다른 앱 위젯을 받지 않아서, 잠금화면 알림 자리에 두 사람 상태를 조용히 띄워 둠.
 * 위젯과 같은 저장소(WidgetStore)를 보고, 위젯을 다시 그릴 때마다(refreshAll) 같이 갱신됨.
 * 사용자가 밀어서 지우면 내용이 바뀔 때까지 다시 띄우지 않음.
 */
public final class StatusNotification {
    static final String CHANNEL = "lock_card";
    static final int ID = 4820;
    private static final String PREFS = "couple_status_widget";

    private StatusNotification() {}

    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    public static boolean enabled(Context c) { return prefs(c).getBoolean("lock_card", true); }

    public static void setEnabled(Context c, boolean on) {
        prefs(c).edit().putBoolean("lock_card", on).remove("lock_card_dismissed").apply();
        update(c);
    }

    /** 밀어서 지운 내용 기억 → 같은 내용이면 다시 안 띄움 */
    static void dismissed(Context c) { prefs(c).edit().putString("lock_card_dismissed", prefs(c).getString("lock_card_sig", "")).apply(); }

    private static void ensureChannel(Context c) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm.getNotificationChannel(CHANNEL) != null) return;
        // 잠금화면에서 아이콘 줄로 밀려나지 않게 높은 중요도 — 소리·진동은 없고, 처음 한 번만 위에 잠깐 뜸(setOnlyAlertOnce)
        NotificationChannel ch = new NotificationChannel(CHANNEL, "잠금화면 상태 카드", NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("잠금화면에 우리 둘의 지금 상태를 조용히 보여줘요");
        ch.setSound(null, null);
        ch.enableVibration(false);
        ch.setShowBadge(false);
        ch.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(ch);
        try { nm.deleteNotificationChannel("status_card"); } catch (Exception ignored) {} // 이전 테스트 채널
    }

    public static void update(Context c) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        String me = WidgetStore.role(c);
        if (me == null || !enabled(c)) { nm.cancel(ID); return; }
        String sp = FamilyDoc.spouse(me);
        String spLine = line(c, sp), meLine = line(c, me);
        String locLine = locationLine(c, me, sp);
        String sig = spLine + "|" + meLine + "|" + locLine;
        SharedPreferences p = prefs(c);
        p.edit().putString("lock_card_sig", sig).apply();
        if (sig.equals(p.getString("lock_card_dismissed", null))) return; // 밀어서 지운 그대로면 다시 안 띄움

        ensureChannel(c);
        int f = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pick = PendingIntent.getActivity(c, 40, new Intent(c, StatusPickerActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK), f);
        PendingIntent dismiss = PendingIntent.getBroadcast(c, 41, new Intent(c, WidgetActionReceiver.class).setAction(WidgetActionReceiver.CARD_DISMISSED), f);
        PendingIntent openApp = PendingIntent.getActivity(c, 42, new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), f);
        String big = meLine + (locLine == null ? "" : "\n" + locLine);
        NotificationCompat.Builder b = new NotificationCompat.Builder(c, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_bboma)
            .setContentTitle(spLine)
            .setContentText(locLine != null && locLine.contains(WidgetStore.name(c, sp)) ? locLine : meLine)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(big))
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setShowWhen(false)
            .setContentIntent(pick)
            .setDeleteIntent(dismiss)
            .addAction(0, "상태 바꾸기", pick)
            .addAction(0, "앱 열기", openApp);
        try { nm.notify(ID, b.build()); } catch (SecurityException ignored) {} // 알림 권한이 꺼져 있으면 조용히 넘어감
    }

    /** "🐻 용권 · 💼 일하는 중 · 14:36부터" */
    private static String line(Context c, String role) {
        String face = "dad".equals(role) ? "🐻" : "🐰";
        String name = WidgetStore.name(c, role);
        String text = WidgetStore.text(c, role);
        if (text == null) return face + " " + name + " · 아직 안 알렸어요";
        long since = WidgetStore.since(c, role);
        boolean stale = System.currentTimeMillis() - since > WidgetStore.STALE_MS;
        String icon = WidgetStore.icon(c, role);
        return face + " " + name + " · " + (icon.isEmpty() ? "" : icon + " ") + text + " · " + (stale ? CoupleStatusWidget.ago(since) : CoupleStatusWidget.hhmm(since) + "부터");
    }

    /** 위치를 주고받는 중이면 "📍 용권: 삼평동 판교역로 166" 같은 한 줄 */
    private static String locationLine(Context c, String me, String sp) {
        String st = WidgetStore.activeLocState(c);
        if (!"sharing".equals(st)) return null;
        String spName = WidgetStore.name(c, sp);
        String until = CoupleStatusWidget.hhmm(WidgetStore.locUntil(c)) + "까지";
        if (me.equals(WidgetStore.locFrom(c))) {
            String addr = WidgetStore.locAddr(c);
            return "📍 " + spName + ": " + (addr == null || addr.isEmpty() ? "위치 받는 중…" : addr) + " · " + until;
        }
        if (WidgetStore.backSharing(c)) {
            String addr = WidgetStore.backAddr(c);
            return "📍 " + spName + ": " + (addr == null || addr.isEmpty() ? "위치 받는 중…" : addr) + " · " + until;
        }
        return "📍 " + spName + "님에게 내 위치를 보여주는 중 · " + until;
    }
}
