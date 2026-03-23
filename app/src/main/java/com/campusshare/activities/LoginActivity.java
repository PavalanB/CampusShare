package com.campusshare.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.campusshare.R;
import com.campusshare.models.User;
import com.campusshare.repositories.AuthRepository;
import com.campusshare.utils.SessionManager;
import com.google.firebase.auth.FirebaseUser;

import java.util.Random;

public class LoginActivity extends AppCompatActivity {

    // UI elements
    private EditText etEmail, etPassword, etCaptchaAnswer;
    private Button btnLogin;
    private TextView tvRegister, tvForgotPassword, tvCaptchaQuestion;
    private ImageButton btnRefreshCaptcha;
    private ProgressBar progressBar;

    // Captcha Logic
    private int captchaResult;
    private final Random random = new Random();

    // Repository handles all Firebase calls
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        setClickListeners();
        generateCaptcha();

        authRepository = new AuthRepository();

        // If already logged in, skip straight to MainActivity
        FirebaseUser fbUser = authRepository.getCurrentUser();
        if (fbUser != null) {
            // Check if we have the user profile in SessionManager
            if (SessionManager.getUser(this) != null) {
                goToMain();
                return;
            } else {
                // Firebase is logged in but SessionManager is empty (e.g. data cleared)
                // Re-fetch profile to restore session
                showLoading(true);
                authRepository.fetchUserProfile(fbUser.getUid(), new AuthRepository.UserProfileCallback() {
                    @Override
                    public void onSuccess(User user) {
                        SessionManager.saveUser(LoginActivity.this, user);
                        goToMain();
                    }
                    @Override
                    public void onFailure(String errorMessage) {
                        authRepository.logout();
                        showLoading(false);
                    }
                });
                return;
            }
        }
        if (authRepository.getCurrentUser() != null) {
            goToMain();
            return;
        }

        initViews();
        setClickListeners();
    }

    private void initViews() {
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etCaptchaAnswer = findViewById(R.id.et_captcha_answer);
        btnLogin = findViewById(R.id.btn_login);
        tvRegister = findViewById(R.id.tv_register);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        tvCaptchaQuestion = findViewById(R.id.tv_captcha_question);
        btnRefreshCaptcha = findViewById(R.id.btn_refresh_captcha);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setClickListeners() {

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
                String captchaInput = etCaptchaAnswer.getText().toString().trim();

            if (!validateInputs(email, password, captchaInput)) return;

            showLoading(true);

            authRepository.login(email, password, new AuthRepository.UserProfileCallback() {
                @Override
                public void onSuccess(User user) {
                    showLoading(false);
                    // Save user session locally
                    SessionManager.saveUser(LoginActivity.this, user);
                    goToMain();
                }

                @Override
                public void onFailure(String errorMessage) {
                    showLoading(false);
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        });
                    @Override
                    public void onFailure(String errorMessage) {
                        showLoading(false);
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        generateCaptcha(); // Refresh captcha on failure
                        etCaptchaAnswer.setText("");
                    }
                });
            });
        }

        if (btnRefreshCaptcha != null) {
            btnRefreshCaptcha.setOnClickListener(v -> generateCaptcha());
        }

        tvRegister.setOnClickListener(v ->
            startActivity(new Intent(this, RegisterActivity.class))
        );

        tvForgotPassword.setOnClickListener(v ->
            startActivity(new Intent(this, ForgotPasswordActivity.class))
        );
    }

    private void generateCaptcha() {
        int num1 = random.nextInt(20) + 1;
        int num2 = random.nextInt(20) + 1;
        captchaResult = num1 + num2;
        if (tvCaptchaQuestion != null) {
            tvCaptchaQuestion.setText(getString(R.string.captcha_question, num1, num2));
        }
    }

    // ─── Validation ───────────────────────────────────────────────────────────

    private boolean validateInputs(String email, String password, String captchaInput) {
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return false;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email");
            etEmail.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return false;
        }
        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(captchaInput)) {
            etCaptchaAnswer.setError(getString(R.string.error_captcha_required));
            etCaptchaAnswer.requestFocus();
            return false;
        }

        try {
            int userVal = Integer.parseInt(captchaInput);
            if (userVal != captchaResult) {
                etCaptchaAnswer.setError(getString(R.string.error_captcha_wrong));
                etCaptchaAnswer.requestFocus();
                generateCaptcha();
                return false;
            }
        } catch (NumberFormatException e) {
            etCaptchaAnswer.setError(getString(R.string.error_invalid_number));
            etCaptchaAnswer.requestFocus();
            return false;
        }

        return true;
    }

    // ─── UI Helpers ───────────────────────────────────────────────────────────

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (btnLogin != null) btnLogin.setEnabled(!show);
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!show);
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        // Clear back stack so pressing back doesn't return to login
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
