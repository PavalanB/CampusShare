package com.campusshare.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.campusshare.R;
import com.campusshare.repositories.AuthRepository;
import com.google.firebase.auth.FirebaseUser;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmail;
    private Button btnSendReset;
    private Button btnBackToLogin;
    private ProgressBar progressBar;

    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        authRepository = new AuthRepository();

        etEmail = findViewById(R.id.et_email);
        btnSendReset = findViewById(R.id.btn_send_reset);
        btnBackToLogin = findViewById(R.id.btn_back_to_login);
        progressBar = findViewById(R.id.progress_bar);

        btnSendReset.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                etEmail.setError("Please enter your email");
                etEmail.requestFocus();
                return;
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Enter a valid email address");
                etEmail.requestFocus();
                return;
            }

            showLoading(true);

            authRepository.sendPasswordReset(email, new AuthRepository.AuthCallback() {
                @Override
                public void onSuccess(FirebaseUser user) {
                    showLoading(false);
                    Toast.makeText(ForgotPasswordActivity.this,
                        "Reset link sent! Check your email.", Toast.LENGTH_LONG).show();
                    finish(); // Go back to Login
                }

                @Override
                public void onFailure(String errorMessage) {
                    showLoading(false);
                    Toast.makeText(ForgotPasswordActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        });

        btnBackToLogin.setOnClickListener(v -> finish());
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSendReset.setEnabled(!show);
    }
}
