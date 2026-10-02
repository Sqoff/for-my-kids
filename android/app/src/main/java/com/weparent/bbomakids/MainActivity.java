package com.weparent.bbomakids;

import android.content.Intent;
import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(FocusServicePlugin.class);
        registerPlugin(WidgetBridgePlugin.class);
        super.onCreate(savedInstanceState);
        LocAlerts.ensureChannel(this); // 서버 위치 알림이 진동으로 오게 채널을 미리 만듦 (ADR 85)
    }

    @Override
    public void onDestroy() {
        try {
            Intent serviceIntent = new Intent(this, FocusForegroundService.class);
            serviceIntent.setAction(FocusForegroundService.ACTION_STOP);
            stopService(serviceIntent);
        } catch (Exception ignored) {}
        super.onDestroy();
    }
}


