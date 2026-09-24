package com.weparent.bbomakids;

import android.content.Intent;
import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(FocusServicePlugin.class);
        super.onCreate(savedInstanceState);
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


