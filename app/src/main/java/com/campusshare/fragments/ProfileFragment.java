package com.campusshare.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.campusshare.R;
import com.campusshare.activities.LoginActivity;
import com.campusshare.models.User;
import com.campusshare.repositories.AuthRepository;
import com.campusshare.utils.SessionManager;

public class ProfileFragment extends Fragment {

    private AuthRepository authRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        authRepository = new AuthRepository();
        User user = SessionManager.getUser(getContext());

        if (user == null) return view;

        // Initials avatar
        TextView tvInitials = view.findViewById(R.id.tv_initials);
        String[] parts = user.getName().split(" ");
        String initials = parts.length >= 2
            ? String.valueOf(parts[0].charAt(0)) + parts[1].charAt(0)
            : String.valueOf(parts[0].charAt(0));
        tvInitials.setText(initials.toUpperCase());

        ((TextView) view.findViewById(R.id.tv_profile_name)).setText(user.getName());
        ((TextView) view.findViewById(R.id.tv_profile_college_id)).setText("ID: " + user.getCollegeID());
        ((TextView) view.findViewById(R.id.tv_profile_dept)).setText(user.getDepartment() + " · " + user.getYear());
        ((TextView) view.findViewById(R.id.tv_profile_email)).setText(user.getEmail());
        ((TextView) view.findViewById(R.id.tv_profile_phone)).setText(user.getPhone());
        ((TextView) view.findViewById(R.id.tv_credit_score)).setText(String.valueOf((int) user.getCreditScore()));
        ((TextView) view.findViewById(R.id.tv_avg_rating)).setText(
            user.getAvgRating() == 0 ? "No ratings yet" : user.getAvgRating() + " / 5.0"
        );

        view.findViewById(R.id.btn_logout).setOnClickListener(v -> {
            authRepository.logout();
            SessionManager.clearSession(getContext());
            redirectToLogin();
        });

        return view;
    }

    private void redirectToLogin() {
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) getActivity().finish();
    }
}
