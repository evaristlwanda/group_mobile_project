package com.example.myapplication;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    private ListAdapter adapter;
    private List<ListItem> items;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    private TextInputLayout fromTextInputLayout;
    private TextInputLayout toTextInputLayout;
    private TextInputEditText fromEditText;
    private TextInputEditText toEditText;
    private RecyclerView myRecyclerView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Initialize UI elements
        fromTextInputLayout = view.findViewById(R.id.from);
        toTextInputLayout = view.findViewById(R.id.to);
        fromEditText = view.findViewById(R.id.text_from);
        toEditText = view.findViewById(R.id.text_to);
        MaterialButton findBuddiesButton = view.findViewById(R.id.findBuddiesButton);
        myRecyclerView = view.findViewById(R.id.myRecyclerView);

        // Set up RecyclerView
        myRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        items = new ArrayList<>();
        adapter = new ListAdapter(items);
        myRecyclerView.setAdapter(adapter);

        // Set initial data (optional)
        items.add(new ListItem("Buddy 1", R.drawable.buddy1));
        items.add(new ListItem("Buddy 2", R.drawable.buddy3));
        items.add(new ListItem("Buddy 3", R.drawable.buddy));
        adapter.notifyDataSetChanged();

        findBuddiesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUserDataAndFindBuddies();
            }
        });

        return view;
    }

    private void saveUserDataAndFindBuddies() {
        String from = Objects.requireNonNull(fromEditText.getText()).toString().trim();
        String to = Objects.requireNonNull(toEditText.getText()).toString().trim();

        if (TextUtils.isEmpty(from)) {
            fromTextInputLayout.setError("Please enter your starting location");
            return;
        } else {
            fromTextInputLayout.setError(null);
        }

        if (TextUtils.isEmpty(to)) {
            toTextInputLayout.setError("Please enter your destination");
            return;
        } else {
            toTextInputLayout.setError(null);
        }

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        String userEmail = currentUser.getEmail();

        // Replace with actual user data you want to save
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", userEmail);
        userData.put("from", from);
        userData.put("to", to);
        userData.put("searchRoute", from + "-" + to); // Combine from and to for search route

        // Save user data to Firestore
        firestore.collection("route")
                .document(userId)
                .set(userData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "User data saved successfully");
                        findBuddies(from, to); // Start searching after saving
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error saving user data", e);
                        Toast.makeText(getContext(), "Error saving user data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void findBuddies(String from, String to) {
        String searchRoute = from + "-" + to;
        // Query Firestore for users with the same search route
        firestore.collection("users")
                .whereEqualTo("searchRoute", searchRoute)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            QuerySnapshot querySnapshot = task.getResult();
                            if (querySnapshot != null && !querySnapshot.isEmpty()) {
                                // Users found with the same search route
                                List<String> buddies = new ArrayList<>();
                                for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                    String buddyEmail = document.getString("email");
                                    String buddyFrom = document.getString("from");
                                    String buddyTo = document.getString("to");
                                    if (buddyEmail != null && !buddyEmail.equals(Objects.requireNonNull(auth.getCurrentUser()).getEmail())) {
                                        buddies.add(buddyEmail + " (From: " + buddyFrom + ", To: " + buddyTo + ")");
                                    }
                                }
                                if (!buddies.isEmpty()) {
                                    // Display the list of buddies
                                    displayBuddies(buddies);
                                } else {
                                    // No buddies found (only the current user)
                                    Toast.makeText(getContext(), "There is no friend in search route", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                // No users found with the same search route
                                Toast.makeText(getContext(), "There is no friend in search route", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            // Error getting documents
                            Log.w(TAG, "Error getting documents.", task.getException());
                            Toast.makeText(getContext(), "Error searching for buddies", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void displayBuddies(List<String> buddies) {
        // Clear existing items
        items.clear();
        // Add new buddies
        for (String buddy : buddies) {
            items.add(new ListItem(buddy, R.drawable.buddy)); // You can use a default image here
        }
        // Notify adapter of changes
        adapter.notifyDataSetChanged();
    }
}