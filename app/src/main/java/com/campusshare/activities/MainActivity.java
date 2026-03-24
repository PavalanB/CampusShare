package com.campusshare.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
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
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class MainActivity extends AppCompatActivity implements ProfileFragment.ProfileUpdateListener {

    private TextView tvToolbarTitle, tvWelcomeMsg;
    private FloatingActionButton fabAdd;
    private BottomNavigationView bottomNav;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageView ivMenu;

    private AuthRepository authRepository;
    private User currentUser;
    private ListenerRegistration notificationListener;

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
        startNotificationListener();

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view_right);
        ivMenu = findViewById(R.id.iv_menu);
        tvToolbarTitle = findViewById(R.id.tv_toolbar_title);
        tvWelcomeMsg = findViewById(R.id.tv_welcome_msg);
        fabAdd         = findViewById(R.id.fab_add);
        bottomNav      = findViewById(R.id.bottom_nav);

        setupSidebar();
        setupBottomNav();

        ivMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.END));

        fabAdd.setOnClickListener(v ->
            startActivity(new Intent(this, AddResourceActivity.class))
        );

        // Set default fragment
        if (getSupportFragmentManager().findFragmentById(R.id.fragment_container) == null) {
            loadFragment(new SearchFragment(), "BROWSE");
            fabAdd.hide();
        }
    }

    private void startNotificationListener() {
        if (currentUser == null) return;

        // Listen for new notifications assigned to THIS user in real-time
        notificationListener = FirebaseFirestore.getInstance()
                .collection("notifications")
                .whereEqualTo("recipientUID", currentUser.getUserID())
                .whereEqualTo("delivered", false)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            String title = dc.getDocument().getString("title");
                            String body = dc.getDocument().getString("body");
                            
                            // Show a Toast or an Alert inside the app
                            if (title != null && body != null) {
                                showInAppNotification(title, body);
                                // Mark as delivered so it doesn't pop up again
                                dc.getDocument().getReference().update("delivered", true);
                            }
                        }
                    }
                });
    }

    private void showInAppNotification(String title, String body) {
        // High-visibility toast for real-time updates
        Toast.makeText(this, "🔔 " + title + "\n" + body, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationListener != null) {
            notificationListener.remove();
        }
    }

    @Override
    public void onProfileUpdated(User user) {
        this.currentUser = user;
        updateSidebarUserInfo();
    }

    private void setupSidebar() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_game) {
                startActivity(new Intent(this, GameActivity.class));
            } else if (id == R.id.nav_settings) {
                // Open Settings
            } else if (id == R.id.nav_theme) {
                showThemeSelectionDialog();
            } else if (id == R.id.nav_about) {
                startActivity(new Intent(this, AboutActivity.class));
            } else if (id == R.id.nav_logout) {
                authRepository.logout();
                SessionManager.clearSession(this);
                redirectToLogin();
            }
            drawerLayout.closeDrawer(GravityCompat.END);
            return true;
        });

        updateSidebarUserInfo();
    }

    private void updateSidebarUserInfo() {
        if (navigationView == null) return;
        
        View headerView = navigationView.getHeaderView(0);
        if (headerView != null) {
            TextView navName = headerView.findViewById(R.id.nav_name);
            TextView navEmail = headerView.findViewById(R.id.nav_email);
            TextView navInitials = headerView.findViewById(R.id.nav_initials);

            if (currentUser != null) {
                if (navName != null) navName.setText(currentUser.getName());
                if (navEmail != null) navEmail.setText(currentUser.getEmail());
                if (navInitials != null && currentUser.getName() != null && !currentUser.getName().isEmpty()) {
                    String name = currentUser.getName();
                    String[] parts = name.split(" ");
                    String initials;
                    if (parts.length >= 2 && !parts[0].isEmpty() && !parts[1].isEmpty()) {
                        initials = String.valueOf(parts[0].charAt(0)) + parts[1].charAt(0);
                    } else {
                        initials = String.valueOf(name.charAt(0));
                    }
                    navInitials.setText(initials.toUpperCase());
                }
            }
        }
    }

    private void showThemeSelectionDialog() {
        String[] themes = {"Light", "Dark"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Theme");
        builder.setItems(themes, (dialog, which) -> {
            if (which == 0) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
        });
        builder.show();
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
                tvWelcomeMsg.setText("Find what you need today");
                fabAdd.hide();
                return true;
            } else if (id == R.id.nav_search) {
                loadFragment(new SearchFragment(), "SEARCH");
                tvWelcomeMsg.setText("Look for specific resources");
                fabAdd.hide();
                return true;
            } else if (id == R.id.nav_my_listings) {
                loadFragment(new MyItemsFragment(), "MY ITEMS");
                tvWelcomeMsg.setText("Manage your resources");
                fabAdd.show();
                return true;
            } else if (id == R.id.nav_inbox) {
                loadFragment(new InboxFragment(), "INBOX");
                tvWelcomeMsg.setText("Your messages and requests");
                fabAdd.hide();
                return true;
            } else if (id == R.id.nav_profile) {
                loadFragment(new ProfileFragment(), "PROFILE");
                tvWelcomeMsg.setText("Your personal dashboard");
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
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
        } else {
            super.onBackPressed();
        }
    }
}
