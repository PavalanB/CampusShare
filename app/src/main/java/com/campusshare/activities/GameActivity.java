package com.campusshare.activities;

import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.campusshare.R;
import java.util.Random;

public class GameActivity extends AppCompatActivity {

    private ImageView ivWheel;
    private Button btnSpin;
    private TextView tvResult;
    private Random random = new Random();
    private int lastDegree = 0;
    private boolean isSpinning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        ivWheel = findViewById(R.id.iv_wheel);
        btnSpin = findViewById(R.id.btn_spin);
        tvResult = findViewById(R.id.tv_game_result);

        btnSpin.setOnClickListener(v -> {
            if (!isSpinning) {
                spinWheel();
            }
        });
    }

    private void spinWheel() {
        isSpinning = true;
        tvResult.setVisibility(View.GONE);

        int degree = random.nextInt(3600) + 720; // At least 2 full rotations

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
                
                // Mock result based on degree
                int creditsWon = (lastDegree / 60) * 5 + 5;
                tvResult.setText("You won " + creditsWon + " Credits!");
                tvResult.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        ivWheel.startAnimation(rotate);
    }
}
