package com.campusshare.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.campusshare.R;
import com.campusshare.repositories.AuthRepository;
import com.campusshare.utils.CampusDeliveryDashView;
import com.campusshare.utils.SessionManager;

public class BikeGameActivity extends AppCompatActivity {

    private AuthRepository authRepository;
    private CampusDeliveryDashView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bike_game);

        authRepository = new AuthRepository();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        gameView = findViewById(R.id.game_view);
        gameView.setCallback(new CampusDeliveryDashView.GameCallback() {
            @Override
            public void onScoreUpdate(int score) {
                // Potential to play a sound or update UI
            }

            @Override
            public void onGameOver(int finalScore) {
                rewardCredits(finalScore);
            }
        });
    }

    private void rewardCredits(int amount) {
        if (amount <= 0) return;
        
        String uid = SessionManager.getUserID(this);
        if (uid == null) return;

        authRepository.updateCreditScore(uid, amount, new AuthRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(BikeGameActivity.this, "Syncing rewards: +" + amount + " Credits!", Toast.LENGTH_SHORT).show();
            }
            @Override public void onFailure(String error) {}
        });
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
