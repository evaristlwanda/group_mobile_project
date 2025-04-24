package com.example.myapplication;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 101;
    private static final String USERS_COLLECTION = "users";
    private static final String PROFILE_IMAGES_FOLDER = "profile_images";

    private ImageView profileImageView;
    private Button addPhotoButton;
    private Button removePhotoButton;
    private EditText bioEditText;
    private Button saveButton;
    private TextView usernameTextView;
    private TextView emailTextView;

    private Uri imageUri;
    private FirebaseFirestore firestore;
    private StorageReference storageReference;
    private String currentUserId;

    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        firestore = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();

        profileImageView = view.findViewById(R.id.profile_image);
        addPhotoButton = view.findViewById(R.id.add_photo_button);
        removePhotoButton = view.findViewById(R.id.remove_photo_button);
        bioEditText = view.findViewById(R.id.bio_input);
        saveButton = view.findViewById(R.id.save_button);
        usernameTextView = view.findViewById(R.id.username_text);
        emailTextView = view.findViewById(R.id.email_text);

        // Get current user ID
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        } else {
            // Handle the case where the user is not logged in
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return view;
        }

        // Initialize ActivityResultLaunchers
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        imageUri = result.getData().getData();
                        profileImageView.setImageURI(imageUri);
                        uploadImageToFirestore(imageUri);
                    }
                }
        );

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Bitmap imageBitmap = (Bitmap) result.getData().getExtras().get("data");
                        profileImageView.setImageBitmap(imageBitmap);
                        imageUri = getImageUri(imageBitmap);
                        uploadImageToFirestore(imageUri);
                    }
                }
        );

        // Load user data
        loadUserData();

        addPhotoButton.setOnClickListener(v -> showImagePickerDialog());
        removePhotoButton.setOnClickListener(v -> removeProfileImage());
        saveButton.setOnClickListener(v -> saveBioToFirestore());

        return view;
    }

    private void showImagePickerDialog() {
        String[] options = {"Take Photo", "Choose from Gallery"};
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getContext());
        builder.setTitle("Add Photo");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                checkCameraPermissionAndOpenCamera();
            } else {
                checkStoragePermissionAndOpenGallery();
            }
        });
        builder.show();
    }

    private void checkCameraPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            openCamera();
        }
    }

    private void checkStoragePermissionAndOpenGallery() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUEST_CODE);
            } else {
                openGallery();
            }
        } else {
            openGallery();
        }
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(cameraIntent);
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(galleryIntent);
    }

    private void uploadImageToFirestore(Uri imageUri) {
        if (imageUri != null) {
            StorageReference imageRef = storageReference.child(PROFILE_IMAGES_FOLDER + "/" + currentUserId);
            imageRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            saveImageUrlToFirestore(uri.toString());
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error uploading image", e);
                        Toast.makeText(getContext(), "Error uploading image", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void saveImageUrlToFirestore(String imageUrl) {
        DocumentReference userRef = firestore.collection(USERS_COLLECTION).document(currentUserId);
        userRef.update("profileImageUrl", imageUrl)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Image URL saved to Firestore");
                    Toast.makeText(getContext(), "Image uploaded successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving image URL", e);
                    Toast.makeText(getContext(), "Error saving image URL", Toast.LENGTH_SHORT).show();
                });
    }

    private void removeProfileImage() {
        DocumentReference userRef = firestore.collection(USERS_COLLECTION).document(currentUserId);
        userRef.update("profileImageUrl", null)
                .addOnSuccessListener(aVoid -> {
                    StorageReference imageRef = storageReference.child(PROFILE_IMAGES_FOLDER + "/" + currentUserId);
                    imageRef.delete().addOnSuccessListener(aVoid1 -> {
                        profileImageView.setImageResource(R.drawable.baseline_account_circle_24);

                                Toast.makeText(getContext(), "Profile image removed", Toast.LENGTH_SHORT).show();
                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "Error deleting image from storage", e);
                        Toast.makeText(getContext(), "Error removing image", Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error removing image URL from Firestore", e);
                    Toast.makeText(getContext(), "Error removing image", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveBioToFirestore() {
        String bio = bioEditText.getText().toString().trim();
        if (!TextUtils.isEmpty(bio)) {
            DocumentReference userRef = firestore.collection(USERS_COLLECTION).document(currentUserId);
            userRef.update("bio", bio)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Bio saved to Firestore");
                        Toast.makeText(getContext(), "Bio saved successfully", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error saving bio", e);
                        Toast.makeText(getContext(), "Error saving bio", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(getContext(), "Bio cannot be empty", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadUserData() {
        DocumentReference userRef = firestore.collection(USERS_COLLECTION).document(currentUserId);
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String username = documentSnapshot.getString("username");
                String email = documentSnapshot.getString("email");
                String bio = documentSnapshot.getString("bio");
                String profileImageUrl = documentSnapshot.getString("profileImageUrl");

                if (username != null) {
                    usernameTextView.setText(username);
                }
                if (email != null) {
                    emailTextView.setText(email);
                }
                if (bio != null) {
                    bioEditText.setText(bio);
                }
                if (profileImageUrl != null) {
                    // Load image using a library like Glide or Picasso
                    // For simplicity, I'm using a basic approach here
                    com.google.firebase.storage.StorageReference imageRef = storageReference.child(PROFILE_IMAGES_FOLDER + "/" + currentUserId);
                    final long ONE_MEGABYTE = 1024 * 1024;
                    imageRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(bytes -> {
                        Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        profileImageView.setImageBitmap(bmp);
                    }).addOnFailureListener(exception -> {
                        // Handle any errors
                        Log.e(TAG, "Error loading image", exception);
                    });
                }
            } else {
                // If the document doesn't exist, create it with basic user info
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null) {
                    Map<String, Object> user = new HashMap<>();
                    user.put("username", currentUser.getDisplayName());
                    user.put("email", currentUser.getEmail());
                    userRef.set(user);
                }
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error loading user data", e);
            Toast.makeText(getContext(), "Error loading user data", Toast.LENGTH_SHORT).show();
        });
    }

    private Uri getImageUri(Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(getContext().getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(getContext(), "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(getContext(), "Storage permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}