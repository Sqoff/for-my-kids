package com.weparent.bbomakids;

import android.content.Context;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Map;

/** 가족 문서(couples/{code})를 위젯·서비스에서 직접 고칠 때 쓰는 도우미 (ADR 66·68). 네이티브 익명 계정은 앱이 members에 등록해 둠 */
public final class FamilyDoc {
    private FamilyDoc() {}

    public static void update(Context ctx, Map<String, Object> fields) {
        Context app = ctx.getApplicationContext();
        String code = WidgetStore.code(app);
        if (code == null) return;
        Runnable write = () -> FirebaseFirestore.getInstance().collection("couples").document(code)
            .update(fields)
            .addOnFailureListener(e -> Toast.makeText(app, "서버에 못 보냈어요. 뽀마키즈 앱을 한 번 열어 주세요", Toast.LENGTH_LONG).show());
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) write.run();
        else auth.signInAnonymously().addOnSuccessListener(r -> write.run());
    }

    public static String spouse(String role) { return "mom".equals(role) ? "dad" : "mom"; }
}
