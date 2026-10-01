package com.weparent.bbomakids;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * 부부 상태 위젯 데이터 (ADR 66).
 * 앱(웹 화면)이 켜져 있을 땐 WidgetBridge 플러그인이, 꺼져 있을 땐 FCM 데이터 푸시(WidgetMessagingService)가 이곳을 갱신하고
 * 위젯은 여기 저장된 값만 그린다.
 */
public final class WidgetStore {
    private static final String PREFS = "couple_status_widget";
    public static final long STALE_MS = 4L * 60 * 60 * 1000; // 4시간 넘으면 흐리게

    private WidgetStore() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static void saveFamily(Context c, String code, String role) {
        prefs(c).edit().putString("code", code).putString("role", role).apply();
    }

    public static String code(Context c) { return prefs(c).getString("code", null); }
    public static String role(Context c) { return prefs(c).getString("role", null); }

    public static void saveName(Context c, String role, String name) {
        if (name != null && !name.isEmpty()) prefs(c).edit().putString(role + "_name", name).apply();
    }

    public static String name(Context c, String role) {
        return prefs(c).getString(role + "_name", "mom".equals(role) ? "엄마" : "아빠");
    }

    /** since: 상태를 정한 시각(ms). 더 오래된 값은 무시해서 늦게 도착한 푸시가 새 상태를 덮지 않게 함 */
    public static void saveStatus(Context c, String role, String icon, String text, long since) {
        if (text == null || text.isEmpty()) return;
        long cur = prefs(c).getLong(role + "_since", 0);
        if (since < cur) return;
        prefs(c).edit()
            .putString(role + "_icon", icon == null ? "" : icon)
            .putString(role + "_text", text)
            .putLong(role + "_since", since)
            .apply();
    }

    public static String icon(Context c, String role) { return prefs(c).getString(role + "_icon", ""); }
    public static String text(Context c, String role) { return prefs(c).getString(role + "_text", null); }
    public static long since(Context c, String role) { return prefs(c).getLong(role + "_since", 0); }

    /** 홈 화면에 놓인 모든 부부 상태 위젯을 다시 그림 */
    public static void refreshAll(Context c) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(c);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(c, CoupleStatusWidget.class));
        if (ids == null || ids.length == 0) return;
        Intent i = new Intent(c, CoupleStatusWidget.class);
        i.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        c.sendBroadcast(i);
    }
}
