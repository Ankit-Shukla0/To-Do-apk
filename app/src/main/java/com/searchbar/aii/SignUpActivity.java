package com.searchbar.aii;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.searchbar.aii.utils.FirebaseHelper;

public class SignUpActivity extends AppCompatActivity {

    private EditText usernameInput, emailInput, passwordInput, confirmPasswordInput;
    private Button signupButton;
    private TextView loginLink;
    private ProgressBar progressBar;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize Firebase Helper
        firebaseHelper = new FirebaseHelper(this);

        // Initialize Views
        initializeViews();

        // Set Click Listeners
        setupClickListeners();

        // Handle back press
        setupBackPressHandler();
    }

    private void initializeViews() {
        usernameInput = findViewById(R.id.username_input);
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        confirmPasswordInput = findViewById(R.id.confirm_password_input);
        signupButton = findViewById(R.id.signup_button);
        loginLink = findViewById(R.id.login_link);
        progressBar = findViewById(R.id.progress_bar);

        // Initially hide progress bar
        progressBar.setVisibility(View.GONE);
    }

    private void setupClickListeners() {
        signupButton.setOnClickListener(v -> attemptSignup());

        loginLink.setOnClickListener(v -> {
            finish(); // Go back to login screen
        });
    }

    private void setupBackPressHandler() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish(); // Go back to login
            }
        };

        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void attemptSignup() {
        // Get input values
        String username = usernameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        // Validate inputs
        if (!validateInputs(username, email, password, confirmPassword)) {
            return;
        }

        // Show progress bar
        showLoading(true);

        // Attempt Firebase registration
        firebaseHelper.registerUser(email, password, username, new FirebaseHelper.OnAuthCompleteListener() {
            @Override
            public void onSuccess(String userId) {
                // Registration successful, now send verification email
                firebaseHelper.sendVerificationEmail(new FirebaseHelper.OnTaskCompleteListener() {
                    @Override
                    public void onSuccess() {
                        showLoading(false);
                        Toast.makeText(SignUpActivity.this,
                                "Registration successful! Please verify your email.",
                                Toast.LENGTH_LONG).show();

                        // Navigate to OTP Verification screen
                        Intent intent = new Intent(SignUpActivity.this, OtpVerificationActivity.class);
                        intent.putExtra("USER_EMAIL", email);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onFailure(String error) {
                        showLoading(false);
                        Toast.makeText(SignUpActivity.this,
                                "Registered but failed to send verification email: " + error,
                                Toast.LENGTH_LONG).show();

                        // Still navigate to verification screen
                        Intent intent = new Intent(SignUpActivity.this, OtpVerificationActivity.class);
                        intent.putExtra("USER_EMAIL", email);
                        startActivity(intent);
                        finish();
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                showLoading(false);
                Toast.makeText(SignUpActivity.this,
                        "Signup Failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean validateInputs(String username, String email, String password, String confirmPassword) {
        // Check if username is empty
        if (TextUtils.isEmpty(username)) {
            usernameInput.setError("Username is required");
            usernameInput.requestFocus();
            return false;
        }

        // Check username length
        if (username.length() < 3) {
            usernameInput.setError("Username must be at least 3 characters");
            usernameInput.requestFocus();
            return false;
        }

        // Check if email is empty
        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email is required");
            emailInput.requestFocus();
            return false;
        }

        // Check if email is valid
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Enter a valid email");
            emailInput.requestFocus();
            return false;
        }

        // Check if password is empty
        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Password is required");
            passwordInput.requestFocus();
            return false;
        }

        // Check password length
        if (password.length() < 6) {
            passwordInput.setError("Password must be at least 6 characters");
            passwordInput.requestFocus();
            return false;
        }

        // Check if confirm password is empty
        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordInput.setError("Please confirm your password");
            confirmPasswordInput.requestFocus();
            return false;
        }

        // Check if passwords match
        if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError("Passwords do not match");
            confirmPasswordInput.requestFocus();
            return false;
        }

        return true;
    }

    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            signupButton.setEnabled(false);
            usernameInput.setEnabled(false);
            emailInput.setEnabled(false);
            passwordInput.setEnabled(false);
            confirmPasswordInput.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            signupButton.setEnabled(true);
            usernameInput.setEnabled(true);
            emailInput.setEnabled(true);
            passwordInput.setEnabled(true);
            confirmPasswordInput.setEnabled(true);
        }
    }
}
