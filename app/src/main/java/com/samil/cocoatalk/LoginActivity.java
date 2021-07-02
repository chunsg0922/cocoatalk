package com.samil.cocoatalk;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.jetbrains.annotations.NotNull;

import static android.content.ContentValues.TAG;


public class LoginActivity extends AppCompatActivity {

    private static FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth mAuth;
    Member member = new Member();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 액션바(타이틀/네비게이션) 숨기기
        ActionBar bar = getSupportActionBar();
        bar.hide();

        mAuth = FirebaseAuth.getInstance();

        EditText editID = (EditText)findViewById(R.id.editID);
        EditText editPasswd = (EditText)findViewById(R.id.editPasswd);
        TextView findMeText = (TextView)findViewById(R.id.findMeText);
        Button registerBtn = (Button)findViewById(R.id.registerBtn);
        Button loginBtn = (Button)findViewById(R.id.loginBtn);

        // '로그인' 클릭 시 실행되는 리스너
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String memberID = editID.getText().toString().trim();
                String memberPassword = editPasswd.getText().toString().trim();

                // '아이디'와 '비밀번호' 입력칸이 비어있는 경우
                if (memberID.equals("") || memberPassword.equals("")) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this, R.style.myTheme).setTitle("알림").setMessage("아이디와 비밀번호를 입력해주세요.")
                            .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            });
                    AlertDialog dialog = builder.create();
                    dialog.show();

                }

                // '아이디'와 '비밀번호' 입력이 되어있는 경우
                else {
                    mAuth.signInWithEmailAndPassword(memberID, memberPassword)
                            .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {

                                        Log.d(TAG, "signInWithCustomToken:success");
                                        FirebaseUser user = mAuth.getCurrentUser();
                                        String id = user.getEmail();
                                        String uid = user.getUid();
                                        Log.e("로그인 이메일 정보 : " , id + ", UID: " + uid);
                                        member.setId(id);
                                        member.setUid(uid);

                                        Intent intent = new Intent(LoginActivity.this, TabActivity.class);
                                        intent.putExtra("id", member.getId());
                                        intent.putExtra("uid", member.getUid());
                                        LoginActivity.this.startActivity(intent);
                                        finish();
                                        //updateUI(user);
                                    } else {
                                        AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this, R.style.myTheme).setTitle("로그인 실패").
                                                setMessage("회원 정보가 일치하지 않습니다. \n 계정과 비밀번호를 다시 입력해주세요.")
                                                .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        editID.setText("");
                                                        editPasswd.setText("");
                                                    }
                                                });
                                        AlertDialog dialog = builder.create();
                                        dialog.show();
                                        Log.w(TAG, "signInWithCustomToken:failure", task.getException());
                                       // updatUI(null);
                                    }
                                }
                            });

                }
            }

        });

        // '코코아계정 또는 비밀번호 찾기' 클릭 시 실행되는 리스너
        findMeText.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(LoginActivity.this, FindMeActivity.class);
                LoginActivity.this.startActivity(intent);
                finish();
            }
        });

        // '코코아계정 만들기' 클릭 시 실행되는 리스너
        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterAgreementActivity.class);
                //Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                LoginActivity.this.startActivity(intent);
                finish();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
       // updateUI(currentUser);
    }

    // 화면 중지
    @Override
    protected void onStop(){
        super.onStop();

        SharedPreferences sharedPreferences = getSharedPreferences("sFile",MODE_PRIVATE);

    }
}