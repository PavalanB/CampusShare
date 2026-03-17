package com.campusshare.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.campusshare.R;
import com.campusshare.fragments.InboxFragment;
import com.campusshare.fragments.MyItemsFragment;
import com.campusshare.fragments.ProfileFragment;
import com.campusshare.fragments.SearchFragment;
import com.campusshare.models.User;
import com.campusshare.repositories.AuthRepository;
import com.campusshare.utils.NotificationHelper;
import com.campusshare.utils.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    private TextView tvToolbarTitle;
    private FloatingActionButton fabAdd;
    private BottomNavigationView bottomNav;
    private Toolbar toolbar;

    private AuthRepository authRepository;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        authRepository = new AuthRepository();
        currentUser = SessionManager.getUser(this);

        if (currentUser == null) {
            if (authRepository.getCurrentUser() != null) {
                authRepository.fetchUserProfile(authRepository.getCurrentUser().getUid(),
                    new AuthRepository.UserProfileCallback() {
                        @Override
                        public void onSuccess(User user) {
                            currentUser = user;
                            SessionManager.saveUser(MainActivity.this, user);
                            continueInitialization();
                        }
                        @Override
                        public void onFailure(String error) {
                            redirectToLogin();
                        }
                    });
                return;
            } else {
                redirectToLogin();
                return;
            }
        }

        continueInitialization();
    }

    private void continueInitialization() {
        NotificationHelper.refreshAndSaveToken();

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tvToolbarTitle = findViewById(R.id.tv_toolbar_title);
        fabAdd         = findViewById(R.id.fab_add);
        bottomNav      = findViewById(R.id.bottom_nav);

        setupBottomNav();

        fabAdd.setOnClickListener(v ->
            startActivity(new Intent(this, AddResourceActivity.class))
        );

        // Set default fragment
        if (getSupportFragmentManager().findFragmentById(R.id.fragment_container) == null) {
            loadFragment(new SearchFragment(), "BROWSE");
        }
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupBottomNav() {
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_browse) {
                loadFragment(new SearchFragment(), "BROWSE");
                fabAdd.hide();
                return true;
            } else if (id == R.id.nav_search) {
                // Since Browse and Search are essentially the same in the current design,
                // we'll keep SearchFragment for both or you can differentiate if needed.
                loadFragment(new SearchFragment(), "SEARCH");
                fabAdd.hide();
                return true;
            } else if (id == R.id.nav_my_listings) {
                loadFragment(new MyItemsFragment(), "MY ITEMS");
                fabAdd.show();
                return true;
            } else if (id == R.id.nav_inbox) {
                loadFragment(new InboxFragment(), "INBOX");
                fabAdd.hide();
                return true;
            } else if (id == R.id.nav_profile) {
                loadFragment(new ProfileFragment(), "PROFILE");
                fabAdd.hide();
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment, String title) {
        tvToolbarTitle.setText(title);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            authRepository.logout();
            SessionManager.clearSession(this);
            redirectToLogin();
            return true;
        } else if (id == R.id.action_map) {
            startActivity(new Intent(this, MapActivity.class));
            return true;
        } else if (id == R.id.action_game) {
            startActivity(new Intent(this, GameActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
