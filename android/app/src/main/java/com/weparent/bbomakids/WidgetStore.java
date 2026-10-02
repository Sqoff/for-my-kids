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

    // ---------- 📍 위치 요청·공유 (ADR 68) ----------
    /** 요청 정보 저장. reqAt 이 지금 저장된 것보다 오래됐으면 무시(늦게 온 푸시) */
    public static void saveLoc(Context c, String from, String to, String state, long until, long reqAt, String addr, double lat, double lng) {
        SharedPreferences p = prefs(c);
        if (reqAt < p.getLong("loc_at", 0)) return;
        if (reqAt > p.getLong("loc_at", 0)) p.edit().remove("back_state").remove("back_until").remove("back_addr").remove("back_lat").remove("back_lng").apply();
        SharedPreferences.Editor e = p.edit()
            .putString("loc_from", from).putString("loc_to", to).putString("loc_state", state)
            .putLong("loc_until", until).putLong("loc_at", reqAt);
        if (addr != null) e.putString("loc_addr", addr).putString("loc_lat", String.valueOf(lat)).putString("loc_lng", String.valueOf(lng));
        else e.remove("loc_addr").remove("loc_lat").remove("loc_lng");
        e.apply();
    }
    public static void saveLocState(Context c, String state, long until) {
        SharedPreferences.Editor e = prefs(c).edit().putString("loc_state", state);
        if (until > 0) e.putLong("loc_until", until);
        if (!"sharing".equals(state)) e.remove("loc_addr").remove("loc_lat").remove("loc_lng");
        e.apply();
    }
    public static String locState(Context c) { return prefs(c).getString("loc_state", null); }
    public static String locFrom(Context c) { return prefs(c).getString("loc_from", null); }
    public static String locTo(Context c) { return prefs(c).getString("loc_to", null); }
    public static long locUntil(Context c) { return prefs(c).getLong("loc_until", 0); }
    public static String locAddr(Context c) { return prefs(c).getString("loc_addr", null); }
    public static String locLat(Context c) { return prefs(c).getString("loc_lat", null); }
    public static String locLng(Context c) { return prefs(c).getString("loc_lng", null); }
    /** 같은 요청·상태로 알림을 이미 띄웠으면 false, 처음이면 기록하고 true */
    public static boolean markLocNotified(Context c, String key) {
        SharedPreferences p = prefs(c);
        if (key.equals(p.getString("loc_notified", null))) return false;
        p.edit().putString("loc_notified", key).apply();
        return true;
    }
    /** ↔ 물어본 사람이 '내 위치도 보여주기'를 눌렀을 때 그 위치 (ADR 85). addr == null 이면 지움 */
    public static void saveBack(Context c, String state, long until, String addr, double lat, double lng) {
        SharedPreferences.Editor e = prefs(c).edit();
        if (state == null || state.isEmpty()) e.remove("back_state").remove("back_until");
        else e.putString("back_state", state).putLong("back_until", until);
        if (addr != null) e.putString("back_addr", addr).putString("back_lat", String.valueOf(lat)).putString("back_lng", String.valueOf(lng));
        else e.remove("back_addr").remove("back_lat").remove("back_lng");
        e.apply();
    }
    /** 물어본 사람도 위치를 보여주는 중인지 */
    public static boolean backSharing(Context c) {
        SharedPreferences p = prefs(c);
        return "sharing".equals(p.getString("back_state", null)) && System.currentTimeMillis() < p.getLong("back_until", 0);
    }
    public static String backAddr(Context c) { return prefs(c).getString("back_addr", null); }
    public static String backLat(Context c) { return prefs(c).getString("back_lat", null); }
    public static String backLng(Context c) { return prefs(c).getString("back_lng", null); }
    /** 지금 의미 있는 요청인지 (끝났거나 시간이 지났으면 null) */
    public static String activeLocState(Context c) {
        String st = locState(c);
        if (st == null || "ended".equals(st)) return null;
        long until = locUntil(c);
        if (("asked".equals(st) || "sharing".equals(st)) && until > 0 && System.currentTimeMillis() > until) return null;
        if ("declined".equals(st) && System.currentTimeMillis() - prefs(c).getLong("loc_at", 0) > 30L * 60 * 1000) return null;
        return st;
    }

    /** 홈 화면에 놓인 모든 부부 상태 위젯을 다시 그림 */
    public static void refreshAll(Context c) {
        refresh(c, CoupleStatusWidget.class);
        refresh(c, CoupleStatusSmallWidget.class); // 2×2 작은 위젯 (ADR 78)
        StatusNotification.update(c);               // 🔒 잠금화면 상태 카드 (ADR 87)
    }

    private static void refresh(Context c, Class<?> provider) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(c);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(c, provider));
        if (ids == null || ids.length == 0) return;
        Intent i = new Intent(c, provider);
        i.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        c.sendBroadcast(i);
    }
}
