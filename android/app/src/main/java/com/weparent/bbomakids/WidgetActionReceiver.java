package com.weparent.bbomakids;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import java.util.HashMap;
import java.util.Map;

/** 위젯·알림 버튼: 위치 물어보기 / 요청 취소 / 지금은 어려워요 / 공유 그만 (ADR 68) */
public class WidgetActionReceiver extends BroadcastReceiver {
    public static final String ASK = "com.weparent.bbomakids.LOC_ASK";
    public static final String CANCEL = "com.weparent.bbomakids.LOC_CANCEL";
    public static final String DECLINE = "com.weparent.bbomakids.LOC_DECLINE";
    public static final String STOP = "com.weparent.bbomakids.LOC_STOP_SHARE";
    public static final long ASK_TTL_MS = 10L * 60 * 1000; // 답이 없으면 10분 뒤 저절로 끝

    @Override
    public void onReceive(Context c, Intent intent) {
        String a = intent.getAction();
        String role = WidgetStore.role(c);
        if (role == null) {
            Toast.makeText(c, "뽀마키즈 앱을 열어 가족을 먼저 연결해 주세요", Toast.LENGTH_LONG).show();
            return;
        }
        if (ASK.equals(a)) ask(c, role);
        else if (CANCEL.equals(a)) {
            Map<String, Object> f = new HashMap<>();
            f.put("loc.req.state", "ended");
            f.put("updatedBy", role);
            FamilyDoc.update(c, f);
            WidgetStore.saveLocState(c, "ended", 0);
            WidgetStore.refreshAll(c);
        } else if (DECLINE.equals(a)) LocationRequestActivity.decline(c);
        else if (STOP.equals(a)) c.startService(new Intent(c, LocationShareService.class).setAction(LocationShareService.ACTION_STOP));
    }

    static void ask(Context c, String role) {
        long now = System.currentTimeMillis();
        String to = FamilyDoc.spouse(role);
        Map<String, Object> req = new HashMap<>();
        req.put("from", role);
        req.put("to", to);
        req.put("at", now);
        req.put("state", "asked");
        req.put("until", now + ASK_TTL_MS);
        Map<String, Object> f = new HashMap<>();
        f.put("loc.req", req);
        f.put("updatedBy", role);
        FamilyDoc.update(c, f);
        WidgetStore.saveLoc(c, role, to, "asked", now + ASK_TTL_MS, now, null, 0, 0);
        WidgetStore.refreshAll(c);
        Toast.makeText(c, WidgetStore.name(c, to) + "님에게 위치를 물어봤어요", Toast.LENGTH_SHORT).show();
    }
}
