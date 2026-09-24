package com.weparent.bbomakids;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.content.ContextCompat;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

@CapacitorPlugin(
    name = "FocusService",
    permissions = {
        @Permission(
            alias = "notifications",
            strings = { Manifest.permission.POST_NOTIFICATIONS }
        )
    }
)
public class FocusServicePlugin extends Plugin {

    @PluginMethod
    public void startFocusService(PluginCall call) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionForAlias("notifications", call, "notificationPermCallback");
                return;
            }
        }
        executeStartService(call);
    }

    @PermissionCallback
    private void notificationPermCallback(PluginCall call) {
        executeStartService(call);
    }

    private void executeStartService(PluginCall call) {
        try {
            int secondsLeft = call.getInt("secondsLeft", 1500);
            int totalSeconds = call.getInt("totalSeconds", 1500);
            String userName = call.getString("userName", "지은");
            String userRole = call.getString("userRole", "엄마");

            Intent serviceIntent = new Intent(getContext(), FocusForegroundService.class);
            serviceIntent.setAction(FocusForegroundService.ACTION_START);
            serviceIntent.putExtra("secondsLeft", secondsLeft);
            serviceIntent.putExtra("totalSeconds", totalSeconds);
            serviceIntent.putExtra("userName", userName);
            serviceIntent.putExtra("userRole", userRole);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getContext().startForegroundService(serviceIntent);
            } else {
                getContext().startService(serviceIntent);
            }

            JSObject ret = new JSObject();
            ret.put("success", true);
            ret.put("message", "FocusForegroundService started");
            call.resolve(ret);
        } catch (Exception e) {
            call.reject("Failed to start FocusForegroundService: " + e.getMessage(), e);
        }
    }

    @PluginMethod
    public void stopFocusService(PluginCall call) {
        try {
            Intent serviceIntent = new Intent(getContext(), FocusForegroundService.class);
            serviceIntent.setAction(FocusForegroundService.ACTION_STOP);
            getContext().startService(serviceIntent);

            JSObject ret = new JSObject();
            ret.put("success", true);
            ret.put("message", "FocusForegroundService stopped");
            call.resolve(ret);
        } catch (Exception e) {
            call.reject("Failed to stop FocusForegroundService: " + e.getMessage(), e);
        }
    }
}
