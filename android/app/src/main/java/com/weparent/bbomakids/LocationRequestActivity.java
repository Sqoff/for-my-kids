package com.weparent.bbomakids;

import android.Manifest;
import android.app.NotificationManager;
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

/** 📍 "○○님이 위치를 궁금해해요" → [지금은 어려워요] / [15분 보여주기] (ADR 68). 이유는 묻지 않음 */
public class LocationRequestActivity extends AppCompatActivity {
    public static final String EXTRA_ACCEPT = "accept";
    static final int NOTIF_REQ = 4816;
    private static final int PERM_REQ = 31;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(NOTIF_REQ);
        String role = WidgetStore.role(this);
        if (role == null || !"asked".equals(WidgetStore.locState(this)) || !role.equals(WidgetStore.locTo(this))) {
            Toast.makeText(this, "지금 들어온 위치 요청이 없어요", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (getIntent().getBooleanExtra(EXTRA_ACCEPT, false)) { accept(); return; }

        boolean dark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        int ink = dark ? 0xFFEEF2EF : 0xFF1D2321, sub = dark ? 0xFF8A958F : 0xFF6B7684, line = dark ? 0xFF36403B : 0xFFE2E5E0, bg = dark ? 0xFF222926 : 0xFFFFFFFF, ok = 0xFF1F9D6B;
        String from = WidgetStore.name(this, FamilyDoc.spouse(role));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(22));
        GradientDrawable rb = new GradientDrawable();
        rb.setColor(bg);
        rb.setCornerRadii(new float[]{dp(24), dp(24), dp(24), dp(24), 0, 0, 0, 0});
        root.setBackground(rb);

        TextView t = new TextView(this);
        t.setText("📍 " + from + "님이 지금 위치를 궁금해해요");
        t.setTextColor(ink);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        root.addView(t);
        TextView s = new TextView(this);
        s.setText("보여주면 15분 동안 정확한 위치가 보이고, 그 뒤엔 지워져요.");
        s.setTextColor(sub);
        s.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        s.setPadding(0, dp(4), 0, dp(16));
        root.addView(s);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button no = button("지금은 어려워요", ink, 0, line);
        Button yes = button("15분 보여주기", 0xFFFFFFFF, ok, ok);
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0, dp(50), 1f);
        lp1.setMarginEnd(dp(8));
        row.addView(no, lp1);
        row.addView(yes, new LinearLayout.LayoutParams(0, dp(50), 1f));
        root.addView(row);
        no.setOnClickListener(v -> { decline(this); finish(); });
        yes.setOnClickListener(v -> accept());

        setContentView(root);
        Window w = getWindow();
        w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        w.setGravity(Gravity.BOTTOM);
        setFinishOnTouchOutside(true);
    }

    static void decline(android.content.Context c) {
        ((NotificationManager) c.getSystemService(NOTIFICATION_SERVICE)).cancel(NOTIF_REQ);
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
        LocationShareService.start(this);
        Toast.makeText(this, "15분 동안 위치를 보여줄게요", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] perms, @NonNull int[] res) {
        super.onRequestPermissionsResult(code, perms, res);
        boolean granted = false;
        for (int r : res) if (r == PackageManager.PERMISSION_GRANTED) granted = true;
        if (granted) {
            LocationShareService.start(this);
            Toast.makeText(this, "15분 동안 위치를 보여줄게요", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "위치 권한이 없어서 보여줄 수 없어요", Toast.LENGTH_LONG).show();
        }
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
        d.setCornerRadius(dp(14));
        b.setBackground(d);
        return b;
    }

    private int dp(float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics()));
    }
}
