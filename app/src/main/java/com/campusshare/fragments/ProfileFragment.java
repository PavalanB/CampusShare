package com.campusshare.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.campusshare.R;
import com.campusshare.activities.HistoryActivity;
import com.campusshare.activities.MainActivity;
import com.campusshare.models.User;
import com.campusshare.repositories.AuthRepository;
import com.campusshare.repositories.ResourceRepository;
import com.campusshare.utils.SessionManager;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private AuthRepository authRepository;
    private User currentUser;
    private TextView tvName, tvInitials;
    private ImageView ivProfile;
    private ProgressBar pbProfile;
    private ProfileUpdateListener profileUpdateListener;
    private Uri cameraImageUri;

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    uploadProfilePhoto(result.getData().getData());
                }
            }
    );

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    uploadProfilePhoto(cameraImageUri);
                }
            }
    );

    private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                boolean allGranted = true;
                for (Boolean granted : result.values()) {
                    if (!granted) {
                        allGranted = false;
                        break;
                    }
                }
                if (allGranted) {
                    showPhotoPickerDialog();
                } else {
                    Toast.makeText(getContext(), "Permissions required to update photo", Toast.LENGTH_SHORT).show();
                }
            }
    );

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
        ivProfile = view.findViewById(R.id.iv_profile_photo);
        pbProfile = view.findViewById(R.id.pb_profile_photo);

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
        
        View avatarContainer = view.findViewById(R.id.avatar_container);
        if (avatarContainer != null) {
            avatarContainer.setOnClickListener(v -> checkPermissionsAndShowDialog());
        }

        View btnHistory = view.findViewById(R.id.btn_view_history);
        if (btnHistory != null) {
            btnHistory.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), HistoryActivity.class);
                startActivity(intent);
            });
        }

        view.findViewById(R.id.btn_logout).setOnClickListener(v -> {
            authRepository.logout();
            SessionManager.clearSession(getContext());
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).recreate();
            }
        });

        return view;
    }

    private void updateUI() {
        if (currentUser == null) return;

        tvName.setText(currentUser.getName());

        String photoUrl = currentUser.getProfilePhoto();
        if (photoUrl != null && !photoUrl.isEmpty()) {
            ivProfile.setVisibility(View.VISIBLE);
            tvInitials.setVisibility(View.GONE);
            Glide.with(this)
                .load(photoUrl)
                .circleCrop()
                .into(ivProfile);
        } else {
            ivProfile.setVisibility(View.GONE);
            tvInitials.setVisibility(View.VISIBLE);
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
    }

    private void checkPermissionsAndShowDialog() {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES};
        } else {
            permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE};
        }

        boolean allGranted = true;
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(requireContext(), p) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            showPhotoPickerDialog();
        } else {
            permissionLauncher.launch(permissions);
        }
    }

    private void showPhotoPickerDialog() {
        String[] options = {"Take Photo", "Choose from Gallery", "Cancel"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Update Profile Photo")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openCamera();
                    } else if (which == 1) {
                        openGallery();
                    }
                })
                .show();
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            Toast.makeText(getContext(), "Error creating file", Toast.LENGTH_SHORT).show();
        }
        
        if (photoFile != null) {
            cameraImageUri = FileProvider.getUriForFile(requireContext(),
                    "com.campusshare.fileprovider",
                    photoFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
            cameraLauncher.launch(intent);
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void uploadProfilePhoto(Uri imageUri) {
        if (imageUri == null) return;
        pbProfile.setVisibility(View.VISIBLE);
        ResourceRepository.uploadPhoto(requireContext(), imageUri, new ResourceRepository.PhotoUploadCallback() {
            @Override
            public void onSuccess(String downloadUrl) {
                authRepository.updateProfilePhoto(currentUser.getUserID(), downloadUrl, new AuthRepository.UserProfileCallback() {
                    @Override
                    public void onSuccess(User updatedUser) {
                        if (!isAdded()) return;
                        pbProfile.setVisibility(View.GONE);
                        currentUser = updatedUser;
                        SessionManager.saveUser(getContext(), updatedUser);
                        updateUI();
                        if (profileUpdateListener != null) {
                            profileUpdateListener.onProfileUpdated(updatedUser);
                        }
                        Toast.makeText(getContext(), "Profile photo updated", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        if (!isAdded()) return;
                        pbProfile.setVisibility(View.GONE);
                        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                if (!isAdded()) return;
                pbProfile.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Upload failed: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEditNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Edit Name");

        final EditText input = new EditText(requireContext());
        input.setText(currentUser.getName());
        input.setSelection(input.getText().length());
        
        FrameLayout container = new FrameLayout(requireContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = 48;
        params.rightMargin = 48;
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
