package com.campusshare.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.campusshare.R;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.iv_logo);
        TextView titleMain = findViewById(R.id.tv_app_name_main);
        TextView titleSub = findViewById(R.id.tv_app_name_sub);

        // Entrance Animations
        ScaleAnimation scaleIn = new ScaleAnimation(0.8f, 1.0f, 0.8f, 1.0f, 
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scaleIn.setDuration(1500);
        scaleIn.setInterpolator(new AccelerateDecelerateInterpolator());

        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(1500);

        logo.startAnimation(scaleIn);
        logo.startAnimation(fadeIn);
        titleMain.startAnimation(fadeIn);
        titleSub.startAnimation(fadeIn);

        // Transition to Login after 4 seconds as requested
        new Handler().postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 4000);
    }
}
