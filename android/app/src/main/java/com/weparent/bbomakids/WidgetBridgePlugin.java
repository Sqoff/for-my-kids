package com.weparent.bbomakids;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
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
        JSObject loc = call.getObject("loc");
        if (loc != null) {
            JSObject req = null, pos = null;
            try { req = loc.getJSObject("req"); } catch (Exception ignored) {}
            try { pos = loc.getJSObject("pos"); } catch (Exception ignored) {}
            if (req != null && req.getString("state") != null) {
                long until = 0, at = 0;
                try { until = req.getLong("until"); } catch (Exception ignored) {}
                try { at = req.getLong("at"); } catch (Exception ignored) {}
                double lat = 0, lng = 0;
                String addr = null;
                if (pos != null) {
                    try { lat = pos.getDouble("lat"); lng = pos.getDouble("lng"); addr = pos.getString("addr", ""); } catch (Exception ignored) {}
                }
                WidgetStore.saveLoc(getContext(), req.getString("from"), req.getString("to"), req.getString("state"), until, at, addr, lat, lng);
                // ↔ 물어본 사람이 되돌려 보여주는 위치 (ADR 85)
                JSObject back = null, bpos = null;
                try { back = loc.getJSObject("back"); } catch (Exception ignored) {}
                try { bpos = loc.getJSObject("backPos"); } catch (Exception ignored) {}
                long buntil = 0; double blat = 0, blng = 0; String baddr = null;
                try { if (back != null) buntil = back.getLong("until"); } catch (Exception ignored) {}
                if (bpos != null) { try { blat = bpos.getDouble("lat"); blng = bpos.getDouble("lng"); baddr = bpos.getString("addr", ""); } catch (Exception ignored) {} }
                WidgetStore.saveBack(getContext(), back == null ? null : back.getString("state"), buntil, baddr, blat, blng);
            }
        }
        WidgetStore.refreshAll(getContext());
        call.resolve();
    }

    /** 📍 '15분 보여주기' — 권한 확인과 공유 시작은 LocationRequestActivity 가 처리 (ADR 68) */
    @PluginMethod
    public void shareLocation(PluginCall call) {
        Intent i = new Intent(getContext(), LocationRequestActivity.class)
            .putExtra(LocationRequestActivity.EXTRA_ACCEPT, true)
            .putExtra(LocationRequestActivity.EXTRA_BACK, Boolean.TRUE.equals(call.getBoolean("back", false))) // ↔ 내 위치도 (ADR 85)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivity(i);
        call.resolve();
    }

    @PluginMethod
    public void stopShareLocation(PluginCall call) {
        getContext().startService(new Intent(getContext(), LocationShareService.class).setAction(LocationShareService.ACTION_STOP));
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

    /** 홈 화면에 위젯 추가 요청 (런처가 지원하면 '홈 화면에 추가' 창이 뜸) */
    @PluginMethod
    public void pinWidget(PluginCall call) {
        JSObject o = new JSObject();
        AppWidgetManager mgr = AppWidgetManager.getInstance(getContext());
        boolean ok = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && mgr.isRequestPinAppWidgetSupported();
        if (ok) ok = mgr.requestPinAppWidget(new ComponentName(getContext(), "small".equals(call.getString("size")) ? CoupleStatusSmallWidget.class : CoupleStatusWidget.class), null, null);
        o.put("requested", ok);
        call.resolve(o);
    }

    private void resolveUid(PluginCall call, String uid) {
        JSObject o = new JSObject();
        o.put("uid", uid);
        call.resolve(o);
    }
}
