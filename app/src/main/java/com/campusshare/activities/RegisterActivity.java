package com.campusshare.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.campusshare.R;
import com.campusshare.models.User;
import com.campusshare.repositories.AuthRepository;
import com.campusshare.utils.SessionManager;

public class RegisterActivity extends AppCompatActivity {

    // UI elements
    private EditText etName, etCollegeID, etEmail, etPhone, etPassword, etConfirmPassword;
    private Spinner spinnerDepartment, spinnerYear;
    private Button btnRegister;
    private TextView tvLogin;
    private ProgressBar progressBar;

    private AuthRepository authRepository;

    // Department and year options — update these to match your college
    private static final String[] DEPARTMENTS = {
        "Select Department",
        "Computer Science",
        "Electronics & Communication",
        "Mechanical Engineering",
        "Civil Engineering",
        "Electrical Engineering",
        "Information Technology",
        "Biotechnology",
        "Chemical Engineering"
    };

    private static final String[] YEARS = {
        "Select Year", "1st Year", "2nd Year", "3rd Year", "4th Year"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SessionManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authRepository = new AuthRepository();

        initViews();
        setupSpinners();
        setClickListeners();
    }

    private void initViews() {
        etName = findViewById(R.id.et_name);
        etCollegeID = findViewById(R.id.et_college_id);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        spinnerDepartment = findViewById(R.id.spinner_department);
        spinnerYear = findViewById(R.id.spinner_year);
        btnRegister = findViewById(R.id.btn_register);
        tvLogin = findViewById(R.id.tv_login);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupSpinners() {
        ArrayAdapter<String> deptAdapter = new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, DEPARTMENTS
        );
        deptAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDepartment.setAdapter(deptAdapter);

        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, YEARS
        );
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYear.setAdapter(yearAdapter);
    }

    private void setClickListeners() {

        btnRegister.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String collegeID = etCollegeID.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();
            String department = spinnerDepartment.getSelectedItem().toString();
            String year = spinnerYear.getSelectedItem().toString();

            if (!validateInputs(name, collegeID, email, phone, password, confirmPassword, department, year))
                return;

            showLoading(true);

            authRepository.register(email, password, name, phone, department, year, collegeID,
                new AuthRepository.UserProfileCallback() {
                    @Override
                    public void onSuccess(User user) {
                        showLoading(false);
                        SessionManager.saveUser(RegisterActivity.this, user);
                        Toast.makeText(RegisterActivity.this,
                            "Welcome to CampusShare, " + user.getName() + "!",
                            Toast.LENGTH_SHORT).show();
                        goToMain();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        showLoading(false);
                        Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                }
            );
        });

        tvLogin.setOnClickListener(v -> finish()); // go back to LoginActivity
    }

    // ─── Validation ───────────────────────────────────────────────────────────

    private boolean validateInputs(String name, String collegeID, String email, String phone,
                                   String password, String confirmPassword,
                                   String department, String year) {
        if (TextUtils.isEmpty(name)) {
            etName.setError("Full name is required"); etName.requestFocus(); return false;
        }
        if (TextUtils.isEmpty(collegeID)) {
            etCollegeID.setError("College ID is required"); etCollegeID.requestFocus(); return false;
        }
        if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Valid email is required"); etEmail.requestFocus(); return false;
        }
        if (TextUtils.isEmpty(phone) || phone.length() < 10) {
            etPhone.setError("Valid phone number is required"); etPhone.requestFocus(); return false;
        }
        if (department.equals("Select Department")) {
            Toast.makeText(this, "Please select your department", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (year.equals("Select Year")) {
            Toast.makeText(this, "Please select your year", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters"); etPassword.requestFocus(); return false;
        }
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match"); etConfirmPassword.requestFocus(); return false;
        }
        return true;
    }

    // ─── UI Helpers ───────────────────────────────────────────────────────────

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!show);
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
