package com.campusshare.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import android.widget.TextView;
import com.campusshare.R;

public class SplashActivity extends AppCompatActivity {

    private String[] loadingTexts = {
        "CONNECTING",
        "ESTABLISHING SECURE CONNECTION",
        "SYNCING RESOURCES",
        "ACCESS GRANTED"
    };
    private int textIndex = 0;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        CardView logoCard = findViewById(R.id.cv_logo);
        TextView title1 = findViewById(R.id.tv_app_name);
        TextView title2 = findViewById(R.id.tv_app_name_2);
        TextView tagline = findViewById(R.id.tv_tagline);
        TextView statusText = findViewById(R.id.tv_loading_status);

        // Load animations
        Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        fadeIn.setDuration(1200);
        
        Animation slideUp = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left);
        slideUp.setDuration(1000);

        logoCard.startAnimation(fadeIn);
        title1.startAnimation(slideUp);
        title2.startAnimation(fadeIn);
        tagline.startAnimation(fadeIn);

        // Dynamic Loading Text Effect
        Runnable statusUpdater = new Runnable() {
            @Override
            public void run() {
                if (textIndex < loadingTexts.length) {
                    statusText.setText(loadingTexts[textIndex++]);
                    handler.postDelayed(this, 1000);
                }
            }
        };
        handler.post(statusUpdater);

        // Transition to LoginActivity
        new Handler().postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 4500);
    }
}
