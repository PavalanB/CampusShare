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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        CardView logoCard = findViewById(R.id.cv_logo);
        TextView title = findViewById(R.id.tv_app_name);
        TextView tagline = findViewById(R.id.tv_tagline);

        // Load animations
        Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        fadeIn.setDuration(1500);
        
        logoCard.startAnimation(fadeIn);
        title.startAnimation(fadeIn);
        tagline.startAnimation(fadeIn);

        // Delay for 4 seconds before moving to LoginActivity
        new Handler().postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            finish();
        }, 4000);
    }
}
