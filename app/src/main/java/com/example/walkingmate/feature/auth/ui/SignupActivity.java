package com.example.walkingmate.feature.auth.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.walkingmate.R;
import com.example.walkingmate.core.security.PasswordSecurity;
import com.example.walkingmate.feature.user.data.UserData;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private EditText usernameEditText, passwordEditText, confirmPasswordEditText;
    private RadioGroup genderGroup;
    private Spinner ageSpinner;
    private Button signupButton;
    private TextView loginLink;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        usernameEditText = findViewById(R.id.signup_username);
        passwordEditText = findViewById(R.id.signup_password);
        confirmPasswordEditText = findViewById(R.id.signup_confirm_password);
        genderGroup = findViewById(R.id.gender_group);
        ageSpinner = findViewById(R.id.age_spinner);
        signupButton = findViewById(R.id.signup_button);
        loginLink = findViewById(R.id.login_link);


        // 나이 연령대 스피너 설정
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.age_ranges, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ageSpinner.setAdapter(adapter);

        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        loginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignupActivity.this, StartActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void registerUser() {
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(SignupActivity.this, "모든 필드를 입력해 주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(SignupActivity.this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 성별 선택
        int selectedGenderId = genderGroup.getCheckedRadioButtonId();
        RadioButton selectedGenderButton = findViewById(selectedGenderId);
        String gender = selectedGenderButton == null ? "" : selectedGenderButton.getText().toString();

        // 성별이 선택되었는지 확인
        if (gender.isEmpty()) {
            Toast.makeText(SignupActivity.this, "성별을 선택해 주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 성별 값을 "M" 또는 "F"로 변환
        String genderCode;
        if (gender.equals("남성")) {
            genderCode = "M";
        } else if (gender.equals("여성")) {
            genderCode = "F";
        } else {
            Toast.makeText(SignupActivity.this, "잘못된 성별 선택입니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 나이 연령대 선택
        String ageRange = ageSpinner.getSelectedItem().toString();  // 선택된 나이 연령대 가져오기

        // 사용자 아이디 중복 체크
        db.collection("users").document(username).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                Toast.makeText(SignupActivity.this, "아이디가 이미 존재합니다.", Toast.LENGTH_SHORT).show();
                            } else {
                                // [보안 리팩토링] 회원가입 시 비밀번호를 평문 저장하지 않고 해시로 저장.
                                String hashedPassword = PasswordSecurity.hashPassword(password);
                                Map<String, Object> user = new HashMap<>();
                                user.put("username", username);
                                user.put("password", hashedPassword);
                                user.put("gender", genderCode);  // 성별을 "M" 또는 "F"로 저장
                                user.put("age", ageRange);       // 나이 연령대 추가
                                // 기타 기본값 설정
                                user.put("nickname", "");
                                user.put("name", "");
                                user.put("birthyear", "");
                                user.put("profileImagebig", "");
                                user.put("profileImagesmall", "");
                                user.put("appname", "");
                                user.put("title", "");
                                user.put("reliability", 0.0);

                                db.collection("users").document(username)
                                        .set(user)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    // Challenge 컬렉션에 데이터 추가
                                                    Map<String, Object> challengeData = new HashMap<>();
                                                    challengeData.put("feedseq", 0);
                                                    challengeData.put("meet", 0);
                                                    challengeData.put("step", 0);

                                                    db.collection("challenge").document(username)
                                                            .set(challengeData)
                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                    if (task.isSuccessful()) {
                                                                        UserData userData = new UserData(
                                                                                username,
                                                                                "", // profileImagebig
                                                                                "", // profileImagesmall
                                                                                "", // appname
                                                                                "", // nickname
                                                                                "", // name
                                                                                ageRange, // 나이 연령대
                                                                                genderCode, // 성별 (M 또는 F)
                                                                                "", // birthyear
                                                                                "", // title
                                                                                0.0 // reliability
                                                                        );

                                                                        // UserData 로컬 저장
                                                                        UserData.saveData(userData, SignupActivity.this);

                                                                        Toast.makeText(SignupActivity.this, "회원가입 성공", Toast.LENGTH_SHORT).show();
                                                                        Intent intent = new Intent(SignupActivity.this, StartActivity.class);
                                                                        startActivity(intent);
                                                                        finish();
                                                                    } else {
                                                                        Toast.makeText(SignupActivity.this, "회원가입 실패: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                                    }
                                                                }
                                                            })
                                                            .addOnFailureListener(e -> {
                                                                Toast.makeText(SignupActivity.this, "회원가입 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                            });
                                                } else {
                                                    Toast.makeText(SignupActivity.this, "회원가입 실패: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(SignupActivity.this, "회원가입 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            }
                        } else {
                            Toast.makeText(SignupActivity.this, "회원가입 실패: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
