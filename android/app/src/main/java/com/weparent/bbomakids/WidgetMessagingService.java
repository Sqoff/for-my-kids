package com.weparent.bbomakids;

import androidx.annotation.NonNull;
import com.capacitorjs.plugins.pushnotifications.MessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.util.Map;

/**
 * Capacitor 푸시 서비스를 감싸서, 앱이 꺼져 있어도 배우자 상태가 바뀌면(데이터 푸시 type=status) 위젯을 새로 그림 (ADR 66).
 * 나머지 푸시는 그대로 Capacitor 플러그인에 넘김.
 */
public class WidgetMessagingService extends MessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage msg) {
        Map<String, String> d = msg.getData();
        if ("status".equals(d.get("type"))) {
            for (String role : new String[]{"mom", "dad"}) {
                WidgetStore.saveName(this, role, d.get(role + "Name"));
                String t = d.get(role + "T");
                if (t != null && !t.isEmpty()) {
                    long since = 0;
                    try { since = Long.parseLong(d.get(role + "Since")); } catch (Exception ignored) {}
                    WidgetStore.saveStatus(this, role, d.get(role + "Ic"), t, since);
                }
            }
            WidgetStore.refreshAll(this);
        }
        super.onMessageReceived(msg);
    }
}
