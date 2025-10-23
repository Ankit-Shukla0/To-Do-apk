package com.searchbar.aii;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.searchbar.aii.utils.FirebaseHelper;

public class OtpVerificationActivity extends AppCompatActivity {

    private static final String TAG = "OtpVerification";
    private TextView emailTextView, timerTextView, resendTextView, instructionsText;
    private Button verifyButton, resendButton;
    private ProgressBar progressBar;
    private FirebaseHelper firebaseHelper;
    private CountDownTimer countDownTimer;
    private boolean canResend = false;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);

        // Initialize Firebase Helper
        firebaseHelper = new FirebaseHelper(this);

        // Initialize Views
        initViews();

        // Get user email from Intent OR FirebaseHelper
        userEmail = getIntent().getStringExtra("USER_EMAIL");
        if (userEmail == null || userEmail.isEmpty()) {
            userEmail = firebaseHelper.getCurrentUserEmail();
        }

        Log.d(TAG, "User email: " + userEmail);

        // Display email
        if (userEmail != null && !userEmail.isEmpty()) {
            emailTextView.setText("Verification email sent to:\n" + userEmail);
        } else {
            emailTextView.setText("Verification email sent to your registered email");
        }

        // ✅ CRITICAL: Send verification email immediately on screen load
        sendVerificationEmailAutomatically();

        // Setup listeners
        setupListeners();

        // Handle back press
        setupBackPressHandler();
    }

    private void initViews() {
        emailTextView = findViewById(R.id.emailTextView);
        timerTextView = findViewById(R.id.timerTextView);
        verifyButton = findViewById(R.id.verifyButton);
        resendButton = findViewById(R.id.resendButton);
        resendTextView = findViewById(R.id.resendTextView);
        progressBar = findViewById(R.id.progress_bar);

        progressBar.setVisibility(View.GONE);
        resendButton.setEnabled(false);
    }

    private void setupListeners() {
        verifyButton.setOnClickListener(v -> checkEmailVerification());

        resendButton.setOnClickListener(v -> {
            if (canResend) {
                resendVerificationEmail();
            } else {
                Toast.makeText(this, "Please wait before resending", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ✅ NEW METHOD: Automatically send email when screen opens
    private void sendVerificationEmailAutomatically() {
        Log.d(TAG, "Attempting to send verification email automatically");
        showLoading(true);

        // Add 1 second delay to ensure Firebase is ready
        new android.os.Handler().postDelayed(() -> {

            firebaseHelper.sendVerificationEmail(new FirebaseHelper.OnTaskCompleteListener() {
                @Override
                public void onSuccess() {
                    showLoading(false);
                    Log.d(TAG, "✅ Verification email sent successfully!");

                    Toast.makeText(OtpVerificationActivity.this,
                            "Verification email sent! Please check your inbox and spam folder.",
                            Toast.LENGTH_LONG).show();

                    // Start resend timer
                    startResendTimer();
                }

                @Override
                public void onFailure(String error) {
                    showLoading(false);
                    Log.e(TAG, "❌ Failed to send verification email: " + error);

                    Toast.makeText(OtpVerificationActivity.this,
                            "Failed to send email: " + error + "\n\nClick 'Resend' to try again.",
                            Toast.LENGTH_LONG).show();

                    // Allow immediate resend on failure
                    canResend = true;
                    resendButton.setEnabled(true);
                    timerTextView.setVisibility(View.GONE);
                }
            });

        }, 1000); // 1 second delay
    }

    private void checkEmailVerification() {
        showLoading(true);
        Log.d(TAG, "Checking email verification status");

        // Reload user data to get latest verification status
        firebaseHelper.reloadUser(new FirebaseHelper.OnTaskCompleteListener() {
            @Override
            public void onSuccess() {
                // Check if email is verified
                if (firebaseHelper.isEmailVerified()) {
                    showLoading(false);
                    Log.d(TAG, "✅ Email verified successfully!");

                    Toast.makeText(OtpVerificationActivity.this,
                            "Email verified successfully!", Toast.LENGTH_SHORT).show();

                    // Navigate to MainActivity
                    Intent intent = new Intent(OtpVerificationActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    showLoading(false);
                    Log.d(TAG, "❌ Email not verified yet");

                    Toast.makeText(OtpVerificationActivity.this,
                            "Email not verified yet.\n\nPlease check your inbox/spam and click the verification link.\n\nThen return here and click this button again.",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(String error) {
                showLoading(false);
                Log.e(TAG, "Error checking verification: " + error);

                Toast.makeText(OtpVerificationActivity.this,
                        "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resendVerificationEmail() {
        showLoading(true);
        canResend = false;
        resendButton.setEnabled(false);
        Log.d(TAG, "Resending verification email");

        firebaseHelper.sendVerificationEmail(new FirebaseHelper.OnTaskCompleteListener() {
            @Override
            public void onSuccess() {
                showLoading(false);
                Log.d(TAG, "✅ Verification email resent successfully!");

                Toast.makeText(OtpVerificationActivity.this,
                        "Verification email sent again! Please check your inbox.",
                        Toast.LENGTH_SHORT).show();

                startResendTimer(); // Restart timer
            }

            @Override
            public void onFailure(String error) {
                showLoading(false);
                Log.e(TAG, "❌ Failed to resend email: " + error);

                Toast.makeText(OtpVerificationActivity.this,
                        "Failed to send email: " + error, Toast.LENGTH_LONG).show();

                canResend = true;
                resendButton.setEnabled(true);
            }
        });
    }

    private void startResendTimer() {
        timerTextView.setVisibility(View.VISIBLE);
        resendTextView.setVisibility(View.VISIBLE);
        resendButton.setEnabled(false);
        canResend = false;

        // 60 seconds countdown
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerTextView.setText("Resend available in: " + millisUntilFinished / 1000 + "s");
            }

            @Override
            public void onFinish() {
                timerTextView.setVisibility(View.GONE);
                canResend = true;
                resendButton.setEnabled(true);
                Log.d(TAG, "Resend timer finished - user can resend now");
            }
        }.start();
    }

    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            verifyButton.setEnabled(false);
            resendButton.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            verifyButton.setEnabled(true);
            resendButton.setEnabled(canResend);
        }
    }

    private void setupBackPressHandler() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Logout and go back to login
                firebaseHelper.logout();
                Intent intent = new Intent(OtpVerificationActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        };

        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
