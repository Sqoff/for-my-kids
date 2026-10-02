package com.weparent.bbomakids;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import java.net.URLEncoder;
import android.os.Build;
import android.os.Bundle;
import android.util.SizeF;
import android.widget.RemoteViews;
import androidx.core.content.ContextCompat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/** 홈 화면 4x2 부부 상태 위젯 (ADR 66) */
public class CoupleStatusWidget extends AppWidgetProvider {

    /** 이 높이(dp) 이상이면 핀 위·카드 아래로 크게, 그보다 낮으면 핀과 카드를 옆으로 (ADR 67, 72) */
    static final int LARGE_MIN_HEIGHT_DP = 240;

    @Override
    public void onUpdate(Context context, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) mgr.updateAppWidget(id, build(context, mgr.getAppWidgetOptions(id)));
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        // 앱을 업데이트하면 위젯이 옛 모양 그대로 남아 있어서, 새 모양으로 바로 다시 그림
        if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) WidgetStore.refreshAll(context);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager mgr, int id, Bundle options) {
        mgr.updateAppWidget(id, build(context, options)); // 크기를 바꾸면 모양도 다시 고름
    }

    static RemoteViews build(Context c, Bundle options) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+: 두 모양을 다 넘기면 런처가 실제 위젯 크기에 맞는 쪽을 고름 (런처마다 보고하는 크기 숫자가 달라서)
            Map<SizeF, RemoteViews> sizes = new HashMap<>();
            sizes.put(new SizeF(180f, 140f), build(c, false));
            sizes.put(new SizeF(180f, LARGE_MIN_HEIGHT_DP), build(c, true));
            return new RemoteViews(sizes);
        }
        // 세로 화면에서는 위젯 높이 = MAX_HEIGHT
        int h = options == null ? 0 : options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0);
        return build(c, h >= LARGE_MIN_HEIGHT_DP);
    }

    private static RemoteViews build(Context c, boolean large) {
        RemoteViews v = new RemoteViews(c.getPackageName(), large ? R.layout.widget_couple_status_large : R.layout.widget_couple_status);
        bindRow(c, v, "mom", R.id.mom_name, R.id.mom_icon, R.id.mom_status, R.id.mom_time, large);
        bindRow(c, v, "dad", R.id.dad_name, R.id.dad_icon, R.id.dad_status, R.id.dad_time, large);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        Intent pick = new Intent(c, StatusPickerActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        v.setOnClickPendingIntent(R.id.btn_change, PendingIntent.getActivity(c, 1, pick, flags));
        Intent open = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent openPi = PendingIntent.getActivity(c, 2, open, flags);
        v.setOnClickPendingIntent(R.id.row_mom, openPi);
        v.setOnClickPendingIntent(R.id.row_dad, openPi);
        bindLocation(c, v, openPi, flags);
        return v;
    }

    /** 📍 머리말 오른쪽 상태 칩 + 왼쪽 버튼 (ADR 68, 72) */
    private static void bindLocation(Context c, RemoteViews v, PendingIntent openPi, int flags) {
        String me = WidgetStore.role(c);
        String sp = me == null ? "dad" : FamilyDoc.spouse(me);
        String spName = WidgetStore.name(c, sp);
        String st = WidgetStore.activeLocState(c);
        boolean iAsked = me != null && me.equals(WidgetStore.locFrom(c));
        boolean askedMe = me != null && me.equals(WidgetStore.locTo(c));
        String chip = null;
        boolean green = false;
        PendingIntent chipPi = openPi;
        int btnIcon = R.drawable.widget_ic_pin;
        String btn = "위치 물어보기";
        PendingIntent btnPi = broadcast(c, WidgetActionReceiver.ASK, 11, flags);
        String until = hhmm(WidgetStore.locUntil(c));
        if (st != null && iAsked) {
            if ("asked".equals(st)) {
                chip = "물어보는 중";
                btnIcon = R.drawable.widget_ic_close;
                btn = "요청 취소";
                btnPi = broadcast(c, WidgetActionReceiver.CANCEL, 12, flags);
            } else if ("sharing".equals(st)) {
                chip = "위치공유 중";
                green = true;
                String addr = WidgetStore.locAddr(c);
                PendingIntent map = mapIntent(c, spName, flags);
                // 상대 카드 맨 아래 줄에 주소 (지도 보기 버튼으로 카카오맵)
                int spTime = "mom".equals(sp) ? R.id.mom_time : R.id.dad_time;
                v.setViewVisibility(spTime, View.VISIBLE);
                v.setTextViewText(spTime, addr == null || addr.isEmpty() ? "위치 받는 중…" : "📍 " + addr);
                if (map != null) {
                    chipPi = map;
                    btnIcon = R.drawable.widget_ic_map;
                    btn = "지도 보기";
                    btnPi = map;
                    v.setOnClickPendingIntent("mom".equals(sp) ? R.id.row_mom : R.id.row_dad, map);
                }
            } else if ("declined".equals(st)) {
                chip = spName + "님이 지금은 어렵대요";
            }
        } else if (st != null && askedMe) {
            if ("asked".equals(st)) {
                chip = spName + "님이 궁금해해요";
                Intent req = new Intent(c, LocationRequestActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                chipPi = PendingIntent.getActivity(c, 13, req, flags);
                btnIcon = R.drawable.widget_ic_reply;
                btn = "답하기";
                btnPi = chipPi;
            } else if ("sharing".equals(st)) {
                chip = "위치공유 중";
                green = true;
                btnIcon = R.drawable.widget_ic_stop;
                btn = "그만하기";
                // 끝나는 시각은 내 카드 아래 줄에 (칩은 시안처럼 짧게)
                int myTime = "mom".equals(me) ? R.id.mom_time : R.id.dad_time;
                v.setViewVisibility(myTime, View.VISIBLE);
                v.setTextViewText(myTime, until + "까지 보여줘요");
                btnPi = broadcast(c, WidgetActionReceiver.STOP, 14, flags);
            }
        }
        // 칩은 늘 보이되(시안), 초록 "위치공유 중"은 실제로 공유 중일 때만. 평소엔 회색 "위치 비공개" (ADR 72, 74)
        if (chip == null) chip = "위치 비공개";
        int chipInk = ContextCompat.getColor(c, green ? R.color.widget_green_ink : R.color.widget_neutral_ink);
        v.setTextViewText(R.id.loc_text, chip);
        v.setTextColor(R.id.loc_text, chipInk);
        v.setInt(R.id.loc_icon, "setColorFilter", chipInk);
        v.setInt(R.id.loc_row, "setBackgroundResource", green ? R.drawable.widget_chip_green : R.drawable.widget_chip_neutral);
        v.setOnClickPendingIntent(R.id.loc_row, chipPi);
        v.setImageViewResource(R.id.btn_open_icon, btnIcon);
        v.setTextViewText(R.id.btn_open_text, btn);
        v.setOnClickPendingIntent(R.id.btn_open, btnPi);
    }

    private static PendingIntent broadcast(Context c, String action, int code, int flags) {
        return PendingIntent.getBroadcast(c, code, new Intent(c, WidgetActionReceiver.class).setAction(action), flags);
    }

    /** 카카오맵 링크로 그 자리 열기 (지도 앱이 없으면 브라우저) */
    static PendingIntent mapIntent(Context c, String name, int flags) {
        String lat = WidgetStore.locLat(c), lng = WidgetStore.locLng(c);
        if (lat == null || lng == null) return null;
        String label;
        try { label = URLEncoder.encode(name, "UTF-8"); } catch (Exception e) { label = "here"; }
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://map.kakao.com/link/map/" + label + "," + lat + "," + lng)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(c, 15, i, flags);
    }

    private static void bindRow(Context c, RemoteViews v, String role, int nameId, int iconId, int statusId, int timeId, boolean large) {
        v.setTextViewText(nameId, WidgetStore.name(c, role));
        String text = WidgetStore.text(c, role);
        long since = WidgetStore.since(c, role);
        if (text == null) {
            v.setViewVisibility(iconId, View.GONE);
            v.setTextViewText(statusId, "아직 안 알렸어요");
            v.setTextViewTextSize(statusId, android.util.TypedValue.COMPLEX_UNIT_SP, large ? 14 : 12); // 카드 폭에 한 줄로
            v.setViewVisibility(timeId, View.GONE);
            v.setTextColor(statusId, ContextCompat.getColor(c, R.color.widget_sub));
            return;
        }
        String icon = WidgetStore.icon(c, role);
        // 이모지는 이름 옆에 작게 (ADR 72)
        v.setViewVisibility(iconId, icon.isEmpty() ? View.GONE : View.VISIBLE);
        v.setTextViewText(iconId, icon);
        v.setTextViewText(statusId, text);
        boolean stale = System.currentTimeMillis() - since > WidgetStore.STALE_MS;
        int ink = ContextCompat.getColor(c, stale ? R.color.widget_stale : R.color.widget_ink);
        v.setTextColor(statusId, ink);
        // 이름은 엄마·아빠 색으로, 오래된 상태면 흐리게 (두 모양 모두 같은 카드 디자인, ADR 70)
        int nameColor = stale ? ink : ContextCompat.getColor(c, "mom".equals(role) ? R.color.widget_mom : R.color.widget_dad);
        v.setTextColor(nameId, nameColor);
        // 시안처럼 평소엔 이름·상태만, 오래된 상태일 때만 "N시간 전" (ADR 74)
        v.setViewVisibility(timeId, stale ? View.VISIBLE : View.GONE);
        if (stale) v.setTextViewText(timeId, ago(since));
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
