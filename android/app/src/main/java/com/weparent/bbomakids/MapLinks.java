package com.weparent.bbomakids;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import java.net.URLEncoder;

/**
 * 🗺️ 위치를 네이버 지도로 열기 (ADR 86).
 * 네이버 지도 앱이 있으면 앱(nmap://place)으로 그 자리에 핀, 없으면 네이버 지도 웹.
 */
public final class MapLinks {
    static final String NMAP = "com.nhn.android.nmap";

    private MapLinks() {}

    static boolean hasNaverMap(Context c) {
        try { c.getPackageManager().getPackageInfo(NMAP, 0); return true; }
        catch (PackageManager.NameNotFoundException e) { return false; }
    }

    public static Intent intent(Context c, String name, String lat, String lng) {
        String label;
        try { label = URLEncoder.encode(name == null ? "위치" : name, "UTF-8"); } catch (Exception e) { label = ""; }
        Uri uri = hasNaverMap(c)
            ? Uri.parse("nmap://place?lat=" + lat + "&lng=" + lng + "&name=" + label + "&appname=" + c.getPackageName())
            : Uri.parse("https://map.naver.com/p/?c=" + lng + "," + lat + ",17,0,0,0,dh");
        Intent i = new Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (hasNaverMap(c)) i.setPackage(NMAP);
        return i;
    }
}
