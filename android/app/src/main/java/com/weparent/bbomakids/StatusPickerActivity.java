package com.weparent.bbomakids;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

/** 위젯의 "상태 바꾸기" → 아래에서 올라오는 작은 창: 8가지 + 직접 쓰기 (ADR 66) */
public class StatusPickerActivity extends AppCompatActivity {

    static final String[][] STATUSES = {
        {"🏠", "집"}, {"🚸", "등원 중"}, {"🧒", "하원 중"}, {"💼", "일하는 중"},
        {"🚗", "이동 중"}, {"🛒", "장보는 중"}, {"🍼", "아이 보는 중"}, {"😴", "쉬는 중"}
    };

    private boolean dark;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String role = WidgetStore.role(this);
        if (role == null) {
            Toast.makeText(this, "뽀마키즈 앱을 열어 가족을 먼저 연결해 주세요", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        dark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        // 위젯과 같은 색 (ADR 71)
        int ink = dark ? 0xFFF2F4F6 : 0xFF191F28;
        int sub = 0xFF8B95A1;
        int line = dark ? 0xFF333A43 : 0xFFE5E8EB;
        int bg = dark ? 0xFF1E2329 : 0xFFFFFFFF;
        int primary = 0xFF3182F6;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        root.setPadding(pad, dp(18), pad, dp(22));
        GradientDrawable rb = new GradientDrawable();
        rb.setColor(bg);
        rb.setCornerRadii(new float[]{dp(24), dp(24), dp(24), dp(24), 0, 0, 0, 0});
        root.setBackground(rb);

        TextView title = new TextView(this);
        title.setText("지금 뭐 하고 있어요?");
        title.setTextColor(ink);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        root.addView(title);

        TextView hint = new TextView(this);
        hint.setText(WidgetStore.name(this, "mom".equals(role) ? "dad" : "mom") + "님 위젯에도 바로 보여요");
        hint.setTextColor(sub);
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        hint.setPadding(0, dp(2), 0, dp(12));
        root.addView(hint);

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(3);
        String curText = WidgetStore.text(this, role);
        for (String[] s : STATUSES) {
            boolean cur = s[1].equals(curText); // 지금 상태는 배경 없이 테두리·글자만 강조 (ADR 27)
            Button b = chip(s[0] + " " + s[1], cur ? primary : ink, cur ? primary : line, cur);
            b.setOnClickListener(v -> pick(role, s[0], s[1]));
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f));
            lp.width = 0;
            lp.setMargins(dp(3), dp(3), dp(3), dp(3));
            grid.addView(b, lp);
        }
        root.addView(grid);

        LinearLayout custom = new LinearLayout(this);
        custom.setOrientation(LinearLayout.HORIZONTAL);
        custom.setPadding(dp(3), dp(10), dp(3), 0);
        EditText input = new EditText(this);
        input.setHint("✏️ 직접 쓰기 (예: 병원 가는 중)");
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        input.setTextColor(ink);
        input.setHintTextColor(sub);
        input.setSingleLine(true);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10)});
        input.setBackground(box(Color.TRANSPARENT, line, 14, 1f));
        input.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        custom.addView(input, ilp);
        Button send = new Button(this);
        send.setText("알리기");
        send.setAllCaps(false);
        send.setTextColor(0xFFFFFFFF);
        send.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        send.setStateListAnimator(null);
        send.setBackground(box(primary, primary, 100, 0));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(dp(84), dp(44));
        slp.setMarginStart(dp(8));
        custom.addView(send, slp);
        Runnable doCustom = () -> {
            String v = input.getText().toString().trim();
            if (v.isEmpty()) { input.requestFocus(); return; }
            pick(role, "✏️", v);
        };
        send.setOnClickListener(v -> doCustom.run());
        input.setOnEditorActionListener((tv, actionId, ev) -> { doCustom.run(); return true; });
        root.addView(custom);

        setContentView(root);
        Window w = getWindow();
        w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        w.setGravity(Gravity.BOTTOM);
        w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        setFinishOnTouchOutside(true);
    }

    private void pick(String role, String icon, String text) {
        long now = System.currentTimeMillis();
        WidgetStore.saveStatus(this, role, icon, text, now);
        WidgetStore.refreshAll(this);
        pushToServer(getApplicationContext(), role, icon, text, now);
        Toast.makeText(this, "‘" + text + "’ 알렸어요", Toast.LENGTH_SHORT).show();
        finish();
    }

    /** 가족 문서의 status.<role> 에 저장. 서버 함수가 배우자 위젯에 데이터 푸시를 보냄 */
    static void pushToServer(Context app, String role, String icon, String text, long since) {
        String code = WidgetStore.code(app);
        if (code == null) return;
        Map<String, Object> st = new HashMap<>();
        st.put("ic", icon);
        st.put("t", text);
        st.put("since", since);
        Runnable write = () -> FirebaseFirestore.getInstance().collection("couples").document(code)
            .update("status." + role, st, "updatedBy", role)
            .addOnFailureListener(e -> Toast.makeText(app, "서버에 못 보냈어요. 뽀마키즈 앱을 한 번 열어 주세요", Toast.LENGTH_LONG).show());
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) write.run();
        else auth.signInAnonymously().addOnSuccessListener(r -> write.run());
    }

    private Button chip(String label, int textColor, int stroke, boolean selected) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(textColor);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        b.setMinHeight(dp(44));
        b.setMinimumHeight(dp(44));
        b.setPadding(dp(4), 0, dp(4), 0);
        b.setStateListAnimator(null);
        if (selected) b.setTypeface(b.getTypeface(), android.graphics.Typeface.BOLD);
        b.setBackground(box(Color.TRANSPARENT, stroke, 100, selected ? 1.5f : 1f));
        return b;
    }

    private GradientDrawable box(int fill, int stroke, int radiusDp, float strokeDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        if (strokeDp > 0) d.setStroke(dp(strokeDp), stroke);
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    private int dp(float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics()));
    }
}
