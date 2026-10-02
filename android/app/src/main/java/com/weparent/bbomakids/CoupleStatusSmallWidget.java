package com.weparent.bbomakids;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;
import androidx.core.content.ContextCompat;

/**
 * 부부 상태 위젯 3단계 — 2×2 작은 위젯 (ADR 78).
 * 두 사람 상태만 한눈에 + 상태 바꾸기. 위치는 요청·공유 중일 때 머리말에 작게만 표시(큰 위젯과 같은 원칙).
 */
public class CoupleStatusSmallWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) mgr.updateAppWidget(id, build(context));
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) WidgetStore.refreshAll(context);
    }

    static RemoteViews build(Context c) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_couple_status_small);
        bindRow(c, v, "mom", R.id.mom_name, R.id.mom_icon, R.id.mom_status);
        bindRow(c, v, "dad", R.id.dad_name, R.id.dad_icon, R.id.dad_status);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        Intent pick = new Intent(c, StatusPickerActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        v.setOnClickPendingIntent(R.id.btn_change, PendingIntent.getActivity(c, 31, pick, flags));
        PendingIntent openPi = PendingIntent.getActivity(c, 32, new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), flags);
        v.setOnClickPendingIntent(R.id.row_mom, openPi);
        v.setOnClickPendingIntent(R.id.row_dad, openPi);
        bindLocation(c, v, openPi, flags);
        return v;
    }

    /** 머리말 오른쪽 작은 칩: 공유 중(초록) / 물어봄·궁금해해요(회색). 평소엔 숨김 */
    private static void bindLocation(Context c, RemoteViews v, PendingIntent openPi, int flags) {
        String me = WidgetStore.role(c);
        String st = WidgetStore.activeLocState(c);
        String chip = null;
        boolean green = false;
        PendingIntent pi = openPi;
        if (st != null && me != null) {
            boolean iAsked = me.equals(WidgetStore.locFrom(c));
            if ("sharing".equals(st)) {
                chip = "● 공유 중";
                green = true;
                if (iAsked) {
                    PendingIntent map = CoupleStatusWidget.mapIntent(c, WidgetStore.name(c, FamilyDoc.spouse(me)), flags);
                    if (map != null) pi = map;
                }
            } else if ("asked".equals(st)) {
                chip = iAsked ? "📍 물어봄" : "📍 궁금해해요";
                if (!iAsked) {
                    Intent req = new Intent(c, LocationRequestActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    pi = PendingIntent.getActivity(c, 33, req, flags);
                }
            }
        }
        v.setViewVisibility(R.id.loc_row, chip == null ? View.GONE : View.VISIBLE);
        if (chip != null) {
            v.setTextViewText(R.id.loc_row, chip);
            v.setInt(R.id.loc_row, "setBackgroundResource", green ? R.drawable.widget_chip_green : R.drawable.widget_chip_neutral);
            v.setTextColor(R.id.loc_row, ContextCompat.getColor(c, green ? R.color.widget_green_ink : R.color.widget_ink));
            v.setOnClickPendingIntent(R.id.loc_row, pi);
        }
    }

    private static void bindRow(Context c, RemoteViews v, String role, int nameId, int iconId, int statusId) {
        String text = WidgetStore.text(c, role);
        long since = WidgetStore.since(c, role);
        boolean stale = text != null && System.currentTimeMillis() - since > WidgetStore.STALE_MS;
        String name = WidgetStore.name(c, role);
        // 이름 옆에 시각 (오래됐으면 "N시간 전")
        v.setTextViewText(nameId, text == null ? name : name + " · " + (stale ? CoupleStatusWidget.ago(since) : CoupleStatusWidget.hhmm(since)));
        v.setTextColor(nameId, ContextCompat.getColor(c, stale ? R.color.widget_stale : ("mom".equals(role) ? R.color.widget_mom : R.color.widget_dad)));
        if (text == null) {
            v.setViewVisibility(iconId, View.GONE);
            v.setTextViewText(statusId, "아직 안 알렸어요");
            v.setTextColor(statusId, ContextCompat.getColor(c, R.color.widget_sub));
            return;
        }
        String icon = WidgetStore.icon(c, role);
        v.setViewVisibility(iconId, icon.isEmpty() ? View.GONE : View.VISIBLE);
        v.setTextViewText(iconId, icon);
        v.setTextViewText(statusId, text);
        v.setTextColor(statusId, ContextCompat.getColor(c, stale ? R.color.widget_stale : R.color.widget_ink));
    }
}
