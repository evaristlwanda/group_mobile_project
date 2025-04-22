package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class SignUp extends AppCompatActivity {

    private FirebaseAuth auth;
    private EditText signUpFullName, signUpEmail, signUpPassword, signUpConfirmPassword;
    private Button signUpButton;
    private TextView tvAlreadyAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);

        // Initialize FirebaseAuth and Views
        auth = FirebaseAuth.getInstance();
        signUpFullName = findViewById(R.id.etFullName);
        signUpEmail = findViewById(R.id.etEmail);
        signUpPassword = findViewById(R.id.etPassword);
        signUpConfirmPassword = findViewById(R.id.etConfirmPassword);
        signUpButton = findViewById(R.id.btnSignUp);
        tvAlreadyAccount = findViewById(R.id.tvAlreadyAccount);

        // Sign up button click
        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String fullName = signUpFullName.getText().toString().trim();
                String email = signUpEmail.getText().toString().trim();
                String password = signUpPassword.getText().toString().trim();
                String confirmPassword = signUpConfirmPassword.getText().toString().trim();

                // Validation
                if (fullName.isEmpty()) {
                    signUpFullName.setError("Full name cannot be empty");
                    return;
                }

                if (email.isEmpty()) {
                    signUpEmail.setError("Email cannot be empty");
                    return;
                }

                if (password.isEmpty()) {
                    signUpPassword.setError("Password cannot be empty");
                    return;
                }

                if (confirmPassword.isEmpty()) {
                    signUpConfirmPassword.setError("Confirm Password cannot be empty");
                    return;
                }

                if (!password.equals(confirmPassword)) {
                    signUpConfirmPassword.setError("Passwords do not match");
                    return;
                }

                // Register user
                auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(SignUp.this, "Sign up successful", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(SignUp.this, MainActivity.class));
                                    finish();
                                } else {
                                    Toast.makeText(SignUp.this, "Sign up failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                }
                            }
                        });
            }
        });

        // "Already have an account?" text click
        tvAlreadyAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(SignUp.this, MainActivity.class));
            }
        });

        // Apply edge-to-edge to root layout (make sure the XML layout has id="main")
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
