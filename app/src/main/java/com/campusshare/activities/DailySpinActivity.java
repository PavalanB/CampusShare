package com.campusshare.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.campusshare.R;
import com.campusshare.repositories.AuthRepository;
import com.campusshare.utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class DailySpinActivity extends AppCompatActivity {

    private ImageView ivWheel;
    private TextView tvResult, tvStatus;
    private Button btnSpin;
    private final Random random = new Random();
    private int lastDegree = 0;
    private boolean isSpinning = false;
    private AuthRepository authRepository;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game); // Reuse wheel layout

        authRepository = new AuthRepository();
        prefs = getSharedPreferences("daily_rewards", MODE_PRIVATE);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar == null) {
            // If toolbar is missing in reused layout, we can find it or skip
            toolbar = new Toolbar(this); 
        }
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Daily Lucky Spin");
        }

        ivWheel = findViewById(R.id.iv_wheel);
        btnSpin = findViewById(R.id.btn_spin);
        tvResult = findViewById(R.id.tv_game_result);
        
        // Add status text for "Used today"
        tvStatus = new TextView(this);
        // In a real app we'd add it to layout, here I'll use Toast or Result area

        checkDailyLimit();

        btnSpin.setOnClickListener(v -> {
            if (!isSpinning) {
                spinWheel();
            }
        });
    }

    private void checkDailyLimit() {
        String lastDate = prefs.getString("last_spin_date", "");
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        if (lastDate.equals(currentDate)) {
            btnSpin.setEnabled(false);
            btnSpin.setText("Come back tomorrow!");
            tvResult.setVisibility(View.VISIBLE);
            tvResult.setText("You have already used your daily spin!");
        }
    }

    private void spinWheel() {
        isSpinning = true;
        btnSpin.setEnabled(false);
        tvResult.setVisibility(View.GONE);

        int degree = random.nextInt(3600) + 720;

        RotateAnimation rotate = new RotateAnimation(lastDegree, degree,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);

        rotate.setDuration(3000);
        rotate.setFillAfter(true);
        rotate.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                isSpinning = false;
                lastDegree = degree % 360;
                
                int creditsWon = (lastDegree / 60) * 5 + 5;
                tvResult.setText("Congratulations! You won " + creditsWon + " credits!");
                tvResult.setVisibility(View.VISIBLE);

                // Save today's date
                String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                prefs.edit().putString("last_spin_date", currentDate).apply();

                // Add credits
                String uid = SessionManager.getUserID(DailySpinActivity.this);
                authRepository.updateCreditScore(uid, creditsWon, new AuthRepository.SimpleCallback() {
                    @Override public void onSuccess() {
                        Toast.makeText(DailySpinActivity.this, "Credits added!", Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onFailure(String error) {}
                });
            }
            @Override public void onAnimationRepeat(Animation animation) {}
        });

        ivWheel.startAnimation(rotate);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
