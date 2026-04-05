package com.campusshare.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import com.campusshare.R;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsActivity extends AppCompatActivity {

    private TextView tvCurrentTheme;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences("settings", MODE_PRIVATE);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        tvCurrentTheme = findViewById(R.id.tv_current_theme);
        LinearLayout btnChangeTheme = findViewById(R.id.btn_change_theme);
        SwitchMaterial switchNotifications = findViewById(R.id.switch_push_notifications);

        updateThemeText();

        btnChangeTheme.setOnClickListener(v -> showThemeSelectionDialog());

        switchNotifications.setChecked(prefs.getBoolean("push_notifications", true));
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("push_notifications", isChecked).apply();
        });
    }

    private void updateThemeText() {
        int mode = AppCompatDelegate.getDefaultNightMode();
        if (mode == AppCompatDelegate.MODE_NIGHT_YES) {
            tvCurrentTheme.setText("Dark");
        } else {
            tvCurrentTheme.setText("Light");
        }
    }

    private void showThemeSelectionDialog() {
        String[] themes = {"Light", "Dark"};
        new AlertDialog.Builder(this)
            .setTitle("Choose Theme")
            .setItems(themes, (dialog, which) -> {
                if (which == 0) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                }
                updateThemeText();
            })
            .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
