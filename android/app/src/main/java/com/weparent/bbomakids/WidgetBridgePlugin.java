package com.weparent.bbomakids;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * 앱(웹 화면) ↔ 홈 화면 위젯 다리 (ADR 66)
 * - sync: 가족 코드·내 역할·이름·두 사람 상태를 위젯 저장소에 넣고 위젯을 다시 그림
 * - nativeUid: 위젯이 직접 서버에 상태를 쓰도록 네이티브 Firebase 익명 로그인 → 웹이 이 uid를 가족 멤버로 등록
 */
@CapacitorPlugin(name = "WidgetBridge")
public class WidgetBridgePlugin extends Plugin {

    @PluginMethod
    public void sync(PluginCall call) {
        String code = call.getString("code");
        String role = call.getString("role");
        if (code != null && role != null) WidgetStore.saveFamily(getContext(), code, role);
        JSObject names = call.getObject("names");
        if (names != null) {
            WidgetStore.saveName(getContext(), "mom", names.getString("mom"));
            WidgetStore.saveName(getContext(), "dad", names.getString("dad"));
        }
        JSObject status = call.getObject("status");
        if (status != null) {
            for (String r : new String[]{"mom", "dad"}) {
                JSObject s = null;
                try { s = status.getJSObject(r); } catch (Exception ignored) {}
                if (s == null) continue;
                long since = 0;
                try { since = s.getLong("since"); } catch (Exception ignored) {}
                WidgetStore.saveStatus(getContext(), r, s.getString("ic"), s.getString("t"), since);
            }
        }
        WidgetStore.refreshAll(getContext());
        call.resolve();
    }

    @PluginMethod
    public void nativeUid(PluginCall call) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser u = auth.getCurrentUser();
        if (u != null) { resolveUid(call, u.getUid()); return; }
        auth.signInAnonymously()
            .addOnSuccessListener(r -> resolveUid(call, r.getUser() != null ? r.getUser().getUid() : null))
            .addOnFailureListener(e -> call.reject(e.getMessage()));
    }

    private void resolveUid(PluginCall call, String uid) {
        JSObject o = new JSObject();
        o.put("uid", uid);
        call.resolve(o);
    }
}
