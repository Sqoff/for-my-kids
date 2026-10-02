package com.weparent.bbomakids;

import androidx.annotation.NonNull;
import com.capacitorjs.plugins.pushnotifications.MessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.util.Map;

/**
 * Capacitor 푸시 서비스를 감싸서, 앱이 꺼져 있어도
 * - 배우자 상태가 바뀌면(type=status) 위젯을 새로 그리고 (ADR 66)
 * - 위치 요청·공유가 바뀌면(type=loc) 위젯을 갱신 (알림은 서버 알림 메시지 loc_alert, ADR 68·85).
 * 나머지 푸시는 그대로 Capacitor 플러그인에 넘김.
 */
public class WidgetMessagingService extends MessagingService {
    @Override
    public void onMessageReceived(@NonNull RemoteMessage msg) {
        Map<String, String> d = msg.getData();
        String type = d.get("type");
        if ("status".equals(type)) {
            for (String role : new String[]{"mom", "dad"}) {
                WidgetStore.saveName(this, role, d.get(role + "Name"));
                String t = d.get(role + "T");
                if (t != null && !t.isEmpty()) {
                    WidgetStore.saveStatus(this, role, d.get(role + "Ic"), t, num(d.get(role + "Since")));
                }
            }
            WidgetStore.refreshAll(this);
        } else if ("loc".equals(type)) {
            android.util.Log.i("BbomaLoc", "push " + d.get("state") + " " + d.get("lfrom") + "->" + d.get("lto"));
            onLoc(d);
        } else if ("loc_alert".equals(type)) {
            // 앱을 보고 있을 때만 여기로 옴(잠들어 있으면 시스템이 바로 띄움) → 버튼 달린 알림으로 (ADR 85)
            onLoc(d);
            RemoteMessage.Notification n = msg.getNotification();
            LocAlerts.showForeground(this, n != null && n.getTitle() != null ? n.getTitle() : "📍 위치 알림", n != null && n.getBody() != null ? n.getBody() : "", d);
            return;
        }
        super.onMessageReceived(msg);
    }

    /** 위치 요청·공유 상태를 위젯 저장소에 반영. 알림은 서버 알림 메시지(loc_alert)가 따로 띄움 (ADR 85) */
    private void onLoc(Map<String, String> d) {
        String from = d.get("lfrom"), to = d.get("lto"), state = d.get("state");
        if (state == null || from == null) return;
        String addr = d.get("addr");
        double lat = dnum(d.get("lat")), lng = dnum(d.get("lng"));
        WidgetStore.saveName(this, from, d.get("fromName"));
        WidgetStore.saveName(this, to, d.get("toName"));
        WidgetStore.saveLoc(this, from, to, state, num(d.get("until")), num(d.get("at")), addr == null || addr.isEmpty() ? null : addr, lat, lng);
        String backAddr = d.get("backAddr");
        if (d.containsKey("backState")) {
            WidgetStore.saveBack(this, d.get("backState"), num(d.get("backUntil")), backAddr == null || backAddr.isEmpty() ? null : backAddr, dnum(d.get("backLat")), dnum(d.get("backLng")));
        }
        if (!"asked".equals(state)) LocAlerts.cancelRequest(this);
        // 상대 공유가 끝났으면 내가 되돌려 보여주던 것도 같이 끝냄
        if (("ended".equals(state) || "expired".equals(state) || "declined".equals(state)) && WidgetStore.backSharing(this)) {
            try { startService(new android.content.Intent(this, LocationShareService.class).setAction(LocationShareService.ACTION_STOP)); } catch (Exception ignored) {}
        }
        WidgetStore.refreshAll(this);
    }

    private static long num(String s) { try { return Long.parseLong(s); } catch (Exception e) { return 0; } }
    private static double dnum(String s) { try { return Double.parseDouble(s); } catch (Exception e) { return 0; } }
}
