package com.campusshare.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.campusshare.R;
import com.campusshare.activities.MainActivity;
import com.campusshare.models.User;
import com.campusshare.repositories.AuthRepository;
import com.campusshare.utils.SessionManager;

public class ProfileFragment extends Fragment {

    private AuthRepository authRepository;
    private User currentUser;
    private TextView tvName, tvInitials;
    private ProfileUpdateListener profileUpdateListener;

    public interface ProfileUpdateListener {
        void onProfileUpdated(User user);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof ProfileUpdateListener) {
            profileUpdateListener = (ProfileUpdateListener) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        authRepository = new AuthRepository();
        currentUser = SessionManager.getUser(getContext());

        if (currentUser == null) return view;

        tvInitials = view.findViewById(R.id.tv_initials);
        tvName = view.findViewById(R.id.tv_profile_name);

        updateUI();

        ((TextView) view.findViewById(R.id.tv_profile_college_id)).setText("ID: " + currentUser.getCollegeID());
        ((TextView) view.findViewById(R.id.tv_profile_dept)).setText(currentUser.getDepartment() + " · " + currentUser.getYear());
        ((TextView) view.findViewById(R.id.tv_profile_email)).setText(currentUser.getEmail());
        ((TextView) view.findViewById(R.id.tv_profile_phone)).setText(currentUser.getPhone());
        ((TextView) view.findViewById(R.id.tv_credit_score)).setText(String.valueOf((int) currentUser.getCreditScore()));
        ((TextView) view.findViewById(R.id.tv_avg_rating)).setText(
            currentUser.getAvgRating() == 0 ? "—" : String.format("%.1f / 5.0", currentUser.getAvgRating())
        );

        view.findViewById(R.id.fab_edit_profile).setOnClickListener(v -> showEditNameDialog());

        view.findViewById(R.id.btn_logout).setOnClickListener(v -> {
            authRepository.logout();
            SessionManager.clearSession(getContext());
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).recreate(); // Simple way to trigger redirect logic in MainActivity
            }
        });

        return view;
    }

    private void updateUI() {
        if (currentUser == null) return;

        tvName.setText(currentUser.getName());

        String name = currentUser.getName();
        if (name != null && !name.isEmpty()) {
            String[] parts = name.split(" ");
            String initials;
            if (parts.length >= 2 && !parts[0].isEmpty() && !parts[1].isEmpty()) {
                initials = String.valueOf(parts[0].charAt(0)) + parts[1].charAt(0);
            } else {
                initials = String.valueOf(name.charAt(0));
            }
            tvInitials.setText(initials.toUpperCase());
        }
    }

    private void showEditNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Edit Name");

        final EditText input = new EditText(requireContext());
        input.setText(currentUser.getName());
        input.setSelection(input.getText().length());
        
        // Add padding to the edit text
        FrameLayout container = new FrameLayout(requireContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        params.rightMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        input.setLayoutParams(params);
        container.addView(input);
        
        builder.setView(container);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty() && !newName.equals(currentUser.getName())) {
                authRepository.updateUserName(currentUser.getUserID(), newName, new AuthRepository.UserProfileCallback() {
                    @Override
                    public void onSuccess(User updatedUser) {
                        currentUser = updatedUser;
                        SessionManager.saveUser(getContext(), updatedUser);
                        updateUI();
                        if (profileUpdateListener != null) {
                            profileUpdateListener.onProfileUpdated(updatedUser);
                        }
                        Toast.makeText(getContext(), "Name updated successfully", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }
}
