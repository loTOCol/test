package com.example.walkingmate.feature.user.ui;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.walkingmate.R;
import com.example.walkingmate.feature.auth.ui.StartActivity;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;

public class AppInfoActivity extends AppCompatActivity {

    ImageButton back;
    Button logoutButton, unregisterButton;
    SharedPreferences sharedPreferences;
    FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_info);

        firestore = FirebaseFirestore.getInstance();
        sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);

        back = findViewById(R.id.back_appinfo);
        logoutButton = findViewById(R.id.logout_button);
        unregisterButton = findViewById(R.id.unregister_button);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLogoutConfirmationDialog();
            }
        });

        unregisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showUnregisterConfirmationDialog();
            }
        });
    }

    private void showLogoutConfirmationDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_logout_confirm);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView cancelView = dialog.findViewById(R.id.btn_logout_cancel);
        TextView confirmView = dialog.findViewById(R.id.btn_logout_confirm);

        cancelView.setOnClickListener(v -> dialog.dismiss());
        confirmView.setOnClickListener(v -> {
            dialog.dismiss();
            performLogout();
        });

        dialog.show();
    }

    private void performLogout() {
        SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String userId = prefs.getString("UserId", null);

        deleteLocalUserData(userId);

        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();

        Intent intent = new Intent(AppInfoActivity.this, StartActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showUnregisterConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("회원 탈퇴")
                .setMessage("정말로 회원 탈퇴하시겠습니까?");

        LayoutInflater inflater = getLayoutInflater();
        View customView = inflater.inflate(R.layout.dialog_common, null);
        builder.setView(customView);

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#FFEBBE")));

        Button yesButton = customView.findViewById(R.id.yes_button);
        Button noButton = customView.findViewById(R.id.no_button);

        yesButton.setOnClickListener(v -> {
            unregisterUser();
            dialog.dismiss();
        });

        noButton.setOnClickListener(v -> dialog.dismiss());
    }

    private void unregisterUser() {
        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String userId = sharedPreferences.getString("UserId", null);

        if (userId == null) {
            Toast.makeText(AppInfoActivity.this, "사용자 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        firestore.collection("users").document(userId).delete()
                .addOnSuccessListener(aVoid -> {
                    firestore.collection("challenge").document(userId).delete()
                            .addOnSuccessListener(aVoid1 -> {
                                deleteLocalUserData(userId);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.clear();
                                editor.apply();

                                Intent intent = new Intent(AppInfoActivity.this, StartActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();

                                Toast.makeText(AppInfoActivity.this, "회원 탈퇴가 완료되었습니다.", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(AppInfoActivity.this, "회원 탈퇴 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                                e.printStackTrace();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AppInfoActivity.this, "회원 탈퇴 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                });
    }

    private void deleteLocalUserData(String userId) {
        if (userId == null) {
            return;
        }
        String folder = getFilesDir().getAbsolutePath() + "/userData/";
        String filename = userId + ".txt";
        File file = new File(folder, filename);

        if (file.exists()) {
            if (file.delete()) {
                Log.d("Local Data", "파일 삭제 성공: " + filename);
            } else {
                Log.d("Local Data", "파일 삭제 실패: " + filename);
            }
        }
    }
}