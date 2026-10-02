package com.weparent.bbomakids;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.HashMap;
import java.util.Map;

/**
 * 📍 위치 요청 창 (ADR 68, 85).
 *  - 요청 받은 사람: "○○님이 위치를 궁금해해요" → [지금은 어려워요] / [15분 보여주기]. 이유는 묻지 않음
 *  - 물어본 사람(상대가 보여주는 중): [🗺️ 지도 보기] / [내 위치도 보여주기] — 서로 보기도 본인이 눌러야만
 * 서버 알림(앱이 잠들어 있어도 시스템이 띄움)을 누르면 알림에 실린 요청 정보로 이 창이 열림.
 */
public class LocationRequestActivity extends AppCompatActivity {
    public static final String EXTRA_ACCEPT = "accept";
    public static final String EXTRA_BACK = "back";
    public static final String ACTION_OPEN = "com.weparent.bbomakids.LOC_REQUEST";
    static final int NOTIF_REQ = 4816;
    private static final int PERM_REQ = 31;
    private boolean backMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LocAlerts.cancelRequest(this);
        absorbPushExtras(getIntent());
        String role = WidgetStore.role(this);
        String st = WidgetStore.activeLocState(this);
        if (role == null || st == null) { openApp(); return; }
        boolean askedMe = "asked".equals(st) && role.equals(WidgetStore.locTo(this));
        boolean iAskedAndSharing = "sharing".equals(st) && role.equals(WidgetStore.locFrom(this));
        backMode = iAskedAndSharing && (getIntent().getBooleanExtra(EXTRA_BACK, false) || "sharing".equals(getIntent().getStringExtra("kind")) || !askedMe);
        if (!askedMe && !backMode) { openApp(); return; }
        if (askedMe && getIntent().getBooleanExtra(EXTRA_ACCEPT, false)) { accept(); return; }
        if (backMode && getIntent().getBooleanExtra(EXTRA_ACCEPT, false) && !WidgetStore.backSharing(this)) { accept(); return; }
        buildSheet(role);
    }

    /** 서버 알림을 눌러 열렸으면 알림에 실린 요청 정보를 먼저 저장 (앱이 잠들어 있던 동안 데이터 푸시를 못 받았을 수 있어서) */
    private void absorbPushExtras(Intent in) {
        if (in == null || in.getStringExtra("lfrom") == null) return;
        long until = num(in.getStringExtra("until")), at = num(in.getStringExtra("at"));
        String addr = in.getStringExtra("addr");
        double lat = dnum(in.getStringExtra("lat")), lng = dnum(in.getStringExtra("lng"));
        WidgetStore.saveLoc(this, in.getStringExtra("lfrom"), in.getStringExtra("lto"), in.getStringExtra("state"), until, at,
            addr == null || addr.isEmpty() ? null : addr, lat, lng);
        WidgetStore.refreshAll(this);
    }

    private void buildSheet(String role) {
        boolean dark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        int ink = dark ? 0xFFF1EEEA : 0xFF3A3330, sub = 0xFF9A918A, line = dark ? 0xFF3A3D44 : 0xFFE6E0D6, bg = dark ? 0xFF1F2126 : 0xFFFBF7EF, ok = 0xFF1F9D6B;
        String spouse = WidgetStore.name(this, FamilyDoc.spouse(role));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(22));
        GradientDrawable rb = new GradientDrawable();
        rb.setColor(bg);
        rb.setCornerRadii(new float[]{dp(24), dp(24), dp(24), dp(24), 0, 0, 0, 0});
        root.setBackground(rb);

        TextView t = new TextView(this);
        t.setTextColor(ink);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        TextView s = new TextView(this);
        s.setTextColor(sub);
        s.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        s.setPadding(0, dp(4), 0, dp(16));
        Button left, right;
        if (backMode) {
            String addr = WidgetStore.locAddr(this);
            t.setText("📍 " + spouse + "님이 위치를 보여주고 있어요");
            boolean mine = WidgetStore.backSharing(this);
            s.setText((addr == null || addr.isEmpty() ? "위치를 받는 중이에요" : addr) + (mine ? "\n내 위치도 보여주는 중이에요" : "\n원하면 내 위치도 같은 시간 동안 보여줄 수 있어요"));
            left = button("🗺️ 지도 보기", ink, 0, line);
            right = button(mine ? "닫기" : "내 위치도 보여주기", 0xFFFFFFFF, ok, ok);
            left.setOnClickListener(v -> {
                PendingIntent map = CoupleStatusWidget.mapIntent(this, spouse, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                try { if (map != null) map.send(); else Toast.makeText(this, "아직 위치를 받는 중이에요", Toast.LENGTH_SHORT).show(); } catch (Exception ignored) {}
                finish();
            });
            right.setOnClickListener(v -> { if (mine) finish(); else accept(); });
        } else {
            t.setText("📍 " + spouse + "님이 지금 위치를 궁금해해요");
            s.setText("보여주면 15분 동안 정확한 위치가 보이고, 그 뒤엔 지워져요.");
            left = button("지금은 어려워요", ink, 0, line);
            right = button("15분 보여주기", 0xFFFFFFFF, ok, ok);
            left.setOnClickListener(v -> { decline(this); finish(); });
            right.setOnClickListener(v -> accept());
        }
        root.addView(t);
        root.addView(s);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0, dp(50), 1f);
        lp1.setMarginEnd(dp(8));
        row.addView(left, lp1);
        row.addView(right, new LinearLayout.LayoutParams(0, dp(50), 1f));
        root.addView(row);

        setContentView(root);
        Window w = getWindow();
        w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        w.setGravity(Gravity.BOTTOM);
        setFinishOnTouchOutside(true);
    }

    private void openApp() {
        startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        finish();
    }

    static void decline(android.content.Context c) {
        LocAlerts.cancelRequest(c);
        Map<String, Object> f = new HashMap<>();
        f.put("loc.req.state", "declined");
        f.put("updatedBy", WidgetStore.role(c));
        FamilyDoc.update(c, f);
        WidgetStore.saveLocState(c, "declined", 0);
        WidgetStore.refreshAll(c);
        Toast.makeText(c, "‘지금은 어려워요’를 보냈어요", Toast.LENGTH_SHORT).show();
    }

    private void accept() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERM_REQ);
            return;
        }
        startShare();
        finish();
    }

    private void startShare() {
        LocationShareService.start(this, backMode);
        Toast.makeText(this, backMode ? "내 위치도 같이 보여줄게요" : "15분 동안 위치를 보여줄게요", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] perms, @NonNull int[] res) {
        super.onRequestPermissionsResult(code, perms, res);
        boolean granted = false;
        for (int r : res) if (r == PackageManager.PERMISSION_GRANTED) granted = true;
        if (granted) startShare();
        else Toast.makeText(this, "위치 권한이 없어서 보여줄 수 없어요", Toast.LENGTH_LONG).show();
        finish();
    }

    private Button button(String label, int text, int fill, int stroke) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(text);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        b.setStateListAnimator(null);
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setStroke(dp(1.5f), stroke);
        d.setCornerRadius(dp(100));
        b.setBackground(d);
        return b;
    }

    private int dp(float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics()));
    }

    private static long num(String s) { try { return Long.parseLong(s); } catch (Exception e) { return 0; } }
    private static double dnum(String s) { try { return Double.parseDouble(s); } catch (Exception e) { return 0; } }
}
