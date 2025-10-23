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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.searchbar.aii.utils.FirebaseHelper;

public class LoginActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput;
    private Button loginButton;
    private TextView signUpLink, forgotPasswordLink;
    private ProgressBar progressBar;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Helper
        firebaseHelper = new FirebaseHelper(this);

        // Check if user is already logged in AND email verified
        if (firebaseHelper.isUserLoggedIn()) {
            if (firebaseHelper.isEmailVerified()) {
                // User logged in and verified, go to MainActivity
                goToMainActivity();
                return;
            } else {
                // User logged in but not verified, go to verification screen
                goToVerificationActivity(firebaseHelper.getCurrentUserEmail());
                return;
            }
        }

        // Initialize Views
        initializeViews();

        // Set Click Listeners
        setupClickListeners();

        // Handle back press
        setupBackPressHandler();
    }

    private void initializeViews() {
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        loginButton = findViewById(R.id.login_button);
        signUpLink = findViewById(R.id.signup_link);
        forgotPasswordLink = findViewById(R.id.forgot_password_link);
        progressBar = findViewById(R.id.progress_bar);

        // Initially hide progress bar
        progressBar.setVisibility(View.GONE);
    }

    private void setupClickListeners() {
        loginButton.setOnClickListener(v -> attemptLogin());

        signUpLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
        });

        forgotPasswordLink.setOnClickListener(v -> {
            Toast.makeText(this, "Password reset feature coming soon!",
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void setupBackPressHandler() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Close all activities and exit app
                finishAffinity();
            }
        };

        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void attemptLogin() {
        // Get input values
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        // Validate inputs
        if (!validateInputs(email, password)) {
            return;
        }

        // Show progress bar
        showLoading(true);

        // Attempt Firebase login
        firebaseHelper.loginUser(email, password, new FirebaseHelper.OnAuthCompleteListener() {
            @Override
            public void onSuccess(String userId) {
                // Login successful, now check email verification
                checkEmailVerificationStatus(email);
            }

            @Override
            public void onFailure(String error) {
                showLoading(false);
                Toast.makeText(LoginActivity.this,
                        "Login Failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void checkEmailVerificationStatus(String email) {
        // Reload user data to get latest verification status
        firebaseHelper.reloadUser(new FirebaseHelper.OnTaskCompleteListener() {
            @Override
            public void onSuccess() {
                showLoading(false);

                // Check if email is verified
                if (firebaseHelper.isEmailVerified()) {
                    // Email verified, proceed to MainActivity
                    Toast.makeText(LoginActivity.this,
                            "Login Successful!", Toast.LENGTH_SHORT).show();
                    goToMainActivity();
                } else {
                    // Email not verified - Show dialog with options
                    showVerificationRequiredDialog(email);
                }
            }

            @Override
            public void onFailure(String error) {
                showLoading(false);
                Toast.makeText(LoginActivity.this,
                        "Error checking verification status: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showVerificationRequiredDialog(String email) {
        new AlertDialog.Builder(this)
                .setTitle("Email Not Verified")
                .setMessage("Your email " + email + " is not verified yet.\n\n" +
                        "Would you like to receive a new verification email?")
                .setPositiveButton("Send Email", (dialog, which) -> {
                    // Send verification email
                    sendVerificationEmailNow(email);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // Logout user
                    firebaseHelper.logout();
                    Toast.makeText(LoginActivity.this,
                            "Please verify your email to continue", Toast.LENGTH_SHORT).show();
                })
                .setCancelable(false)
                .show();
    }

    private void sendVerificationEmailNow(String email) {
        showLoading(true);

        firebaseHelper.sendVerificationEmail(new FirebaseHelper.OnTaskCompleteListener() {
            @Override
            public void onSuccess() {
                showLoading(false);
                Toast.makeText(LoginActivity.this,
                        "Verification email sent! Check your inbox.", Toast.LENGTH_LONG).show();

                // Navigate to verification screen
                Intent intent = new Intent(LoginActivity.this, OtpVerificationActivity.class);
                intent.putExtra("USER_EMAIL", email);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(String error) {
                showLoading(false);
                Toast.makeText(LoginActivity.this,
                        "Failed to send email: " + error +
                                "\n\nPlease try again or contact support.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean validateInputs(String email, String password) {
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

        return true;
    }

    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            loginButton.setEnabled(false);
            emailInput.setEnabled(false);
            passwordInput.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            loginButton.setEnabled(true);
            emailInput.setEnabled(true);
            passwordInput.setEnabled(true);
        }
    }

    private void goToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void goToVerificationActivity(String email) {
        Intent intent = new Intent(LoginActivity.this, OtpVerificationActivity.class);
        intent.putExtra("USER_EMAIL", email);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
