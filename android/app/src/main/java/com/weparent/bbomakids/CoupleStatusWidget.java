package com.weparent.bbomakids;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;
import androidx.core.content.ContextCompat;
import java.util.Calendar;

/** 홈 화면 4x2 부부 상태 위젯 (ADR 66) */
public class CoupleStatusWidget extends AppWidgetProvider {

    /** 이 높이(dp) 이상이면 두 사람 카드를 크게 보여주는 넉넉한 모양 (ADR 67) */
    static final int LARGE_MIN_HEIGHT_DP = 150;

    @Override
    public void onUpdate(Context context, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) mgr.updateAppWidget(id, build(context, mgr.getAppWidgetOptions(id)));
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager mgr, int id, Bundle options) {
        mgr.updateAppWidget(id, build(context, options)); // 크기를 바꾸면 모양도 다시 고름
    }

    static RemoteViews build(Context c, Bundle options) {
        // 세로 화면에서는 위젯 높이 = MAX_HEIGHT
        int h = options == null ? 0 : options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0);
        boolean large = h >= LARGE_MIN_HEIGHT_DP;
        RemoteViews v = new RemoteViews(c.getPackageName(), large ? R.layout.widget_couple_status_large : R.layout.widget_couple_status);
        bindRow(c, v, "mom", R.id.mom_name, R.id.mom_status, R.id.mom_time, large);
        bindRow(c, v, "dad", R.id.dad_name, R.id.dad_status, R.id.dad_time, large);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        Intent pick = new Intent(c, StatusPickerActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        v.setOnClickPendingIntent(R.id.btn_change, PendingIntent.getActivity(c, 1, pick, flags));
        Intent open = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent openPi = PendingIntent.getActivity(c, 2, open, flags);
        v.setOnClickPendingIntent(R.id.btn_open, openPi);
        v.setOnClickPendingIntent(R.id.row_mom, openPi);
        v.setOnClickPendingIntent(R.id.row_dad, openPi);
        return v;
    }

    private static void bindRow(Context c, RemoteViews v, String role, int nameId, int statusId, int timeId, boolean large) {
        v.setTextViewText(nameId, WidgetStore.name(c, role));
        String text = WidgetStore.text(c, role);
        long since = WidgetStore.since(c, role);
        if (text == null) {
            v.setTextViewText(statusId, "아직 알리지 않았어요");
            v.setTextViewText(timeId, "");
            v.setTextColor(statusId, ContextCompat.getColor(c, R.color.widget_sub));
            return;
        }
        String icon = WidgetStore.icon(c, role);
        v.setTextViewText(statusId, (icon.isEmpty() ? "" : icon + " ") + text);
        boolean stale = System.currentTimeMillis() - since > WidgetStore.STALE_MS;
        int ink = ContextCompat.getColor(c, stale ? R.color.widget_stale : R.color.widget_ink);
        v.setTextColor(statusId, ink);
        // 큰 모양은 이름을 엄마·아빠 색으로, 오래된 상태면 흐리게
        int nameColor = stale ? ink : (large ? ContextCompat.getColor(c, "mom".equals(role) ? R.color.widget_mom : R.color.widget_dad) : ink);
        v.setTextColor(nameId, nameColor);
        v.setTextViewText(timeId, stale ? ago(since) : hhmm(since) + "부터");
    }

    private static String hhmm(long ms) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(ms);
        return String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
    }

    private static String ago(long ms) {
        long h = (System.currentTimeMillis() - ms) / 3600000L;
        return h >= 24 ? (h / 24) + "일 전" : h + "시간 전";
    }
}
