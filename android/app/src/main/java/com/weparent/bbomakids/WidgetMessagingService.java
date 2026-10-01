package com.weparent.bbomakids;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import com.capacitorjs.plugins.pushnotifications.MessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.util.Map;

/**
 * Capacitor 푸시 서비스를 감싸서, 앱이 꺼져 있어도
 * - 배우자 상태가 바뀌면(type=status) 위젯을 새로 그리고 (ADR 66)
 * - 위치 요청·공유가 바뀌면(type=loc) 위젯을 갱신하고 알림을 띄움 (ADR 68).
 * 나머지 푸시는 그대로 Capacitor 플러그인에 넘김.
 */
public class WidgetMessagingService extends MessagingService {
    private static final String CH_LOC = "loc_request";

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
        }
        super.onMessageReceived(msg);
    }

    private void onLoc(Map<String, String> d) {
        String me = WidgetStore.role(this);
        String from = d.get("lfrom"), to = d.get("lto"), state = d.get("state");
        long at = num(d.get("at"));
        String addr = d.get("addr");
        double lat = dnum(d.get("lat")), lng = dnum(d.get("lng"));
        WidgetStore.saveName(this, from, d.get("fromName"));
        WidgetStore.saveName(this, to, d.get("toName"));
        WidgetStore.saveLoc(this, from, to, state, num(d.get("until")), at, addr == null || addr.isEmpty() && lat == 0 ? null : addr, lat, lng);
        WidgetStore.refreshAll(this);
        if (me == null || state == null) return;
        // 앱이 켜져 있으면 웹 동기화가 같은 값을 먼저 저장하므로, '이미 알렸는지'는 따로 기억함
        boolean changed = WidgetStore.markLocNotified(this, at + ":" + state);
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(new NotificationChannel(CH_LOC, "위치 요청", NotificationManager.IMPORTANCE_HIGH));
        }
        int f = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        String fromName = WidgetStore.name(this, from), toName = WidgetStore.name(this, to);
        if ("asked".equals(state) && me.equals(to) && changed) {
            Intent open = new Intent(this, LocationRequestActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            Intent yes = new Intent(this, LocationRequestActivity.class).putExtra(LocationRequestActivity.EXTRA_ACCEPT, true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent no = PendingIntent.getBroadcast(this, 21, new Intent(this, WidgetActionReceiver.class).setAction(WidgetActionReceiver.DECLINE), f);
            nm.notify(LocationRequestActivity.NOTIF_REQ, new NotificationCompat.Builder(this, CH_LOC)
                .setSmallIcon(R.drawable.ic_stat_bboma)
                .setContentTitle("📍 " + fromName + "님이 위치를 궁금해해요")
                .setContentText("보여주면 15분 동안 정확한 위치가 보여요")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(PendingIntent.getActivity(this, 22, open, f))
                .addAction(0, "지금은 어려워요", no)
                .addAction(0, "15분 보여주기", PendingIntent.getActivity(this, 23, yes, f))
                .build());
        } else if (me.equals(from) && changed && "declined".equals(state)) {
            nm.notify(LocationRequestActivity.NOTIF_REQ + 1, new NotificationCompat.Builder(this, CH_LOC)
                .setSmallIcon(R.drawable.ic_stat_bboma)
                .setContentTitle(toName + "님이 지금은 어렵대요")
                .setContentText("이유는 묻지 않아요")
                .setAutoCancel(true)
                .build());
        } else if (me.equals(from) && changed && "sharing".equals(state)) {
            PendingIntent map = CoupleStatusWidget.mapIntent(this, toName, f);
            NotificationCompat.Builder b = new NotificationCompat.Builder(this, CH_LOC)
                .setSmallIcon(R.drawable.ic_stat_bboma)
                .setContentTitle("📍 " + toName + "님이 위치를 보여주고 있어요")
                .setContentText("15분 동안 위젯과 앱에서 볼 수 있어요")
                .setAutoCancel(true);
            if (map != null) b.setContentIntent(map);
            nm.notify(LocationRequestActivity.NOTIF_REQ + 1, b.build());
        } else if (!"asked".equals(state) && me.equals(to)) {
            nm.cancel(LocationRequestActivity.NOTIF_REQ); // 요청이 끝났으면 받은 알림 정리
        }
    }

    private static long num(String s) { try { return Long.parseLong(s); } catch (Exception e) { return 0; } }
    private static double dnum(String s) { try { return Double.parseDouble(s); } catch (Exception e) { return 0; } }
}
