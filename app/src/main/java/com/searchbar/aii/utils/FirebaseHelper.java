package com.searchbar.aii.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.searchbar.aii.models.Task;
import java.util.ArrayList;
import java.util.List;

public class FirebaseHelper {
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private Context context;
    private static final String TAG = "FirebaseHelper";

    public FirebaseHelper(Context context) {
        this.context = context;
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    // ==================== USER AUTHENTICATION ====================

    // Register new user
    public void registerUser(String email, String password, String username, OnAuthCompleteListener listener) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Log.d(TAG, "User registered successfully: " + user.getUid());
                            // Save username to database
                            mDatabase.child("users").child(user.getUid())
                                    .child("username").setValue(username)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Username saved to database");
                                        listener.onSuccess(user.getUid());
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Failed to save username: " + e.getMessage());
                                        listener.onFailure(e.getMessage());
                                    });
                        }
                    } else {
                        String error = task.getException() != null ?
                                task.getException().getMessage() : "Registration failed";
                        Log.e(TAG, "Registration failed: " + error);
                        listener.onFailure(error);
                    }
                });
    }

    // Login user
    public void loginUser(String email, String password, OnAuthCompleteListener listener) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Log.d(TAG, "User logged in successfully: " + user.getUid());
                            listener.onSuccess(user.getUid());
                        }
                    } else {
                        String error = task.getException() != null ?
                                task.getException().getMessage() : "Login failed";
                        Log.e(TAG, "Login failed: " + error);
                        listener.onFailure(error);
                    }
                });
    }

    // Logout user
    public void logout() {
        mAuth.signOut();
        Log.d(TAG, "User logged out");
    }

    // Check if user is logged in
    public boolean isUserLoggedIn() {
        boolean loggedIn = mAuth.getCurrentUser() != null;
        Log.d(TAG, "User logged in: " + loggedIn);
        return loggedIn;
    }

    // Get current user ID
    public String getCurrentUserId() {
        FirebaseUser user = mAuth.getCurrentUser();
        String userId = user != null ? user.getUid() : null;
        Log.d(TAG, "Current user ID: " + userId);
        return userId;
    }

    // Get current user email
    public String getCurrentUserEmail() {
        FirebaseUser user = mAuth.getCurrentUser();
        String email = user != null ? user.getEmail() : null;
        Log.d(TAG, "Current user email: " + email);
        return email;
    }

    // ==================== EMAIL VERIFICATION ====================

    // Send verification email
    public void sendVerificationEmail(OnTaskCompleteListener listener) {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            Log.e(TAG, "Cannot send verification email: No user signed in");
            listener.onFailure("No user is currently signed in");
            return;
        }

        // Check if already verified
        if (user.isEmailVerified()) {
            Log.d(TAG, "Email already verified");
            listener.onSuccess();
            return;
        }

        Log.d(TAG, "Sending verification email to: " + user.getEmail());

        // Send verification email
        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Verification email sent successfully");
                        listener.onSuccess();
                    } else {
                        String error = task.getException() != null ?
                                task.getException().getMessage() : "Failed to send verification email";
                        Log.e(TAG, "Failed to send verification email: " + error);
                        listener.onFailure(error);
                    }
                });
    }

    // Check if email is verified
    public boolean isEmailVerified() {
        FirebaseUser user = mAuth.getCurrentUser();
        boolean verified = user != null && user.isEmailVerified();
        Log.d(TAG, "Email verified: " + verified);
        return verified;
    }

    // Reload user data to refresh verification status
    public void reloadUser(OnTaskCompleteListener listener) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            Log.d(TAG, "Reloading user data...");
            user.reload().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "User data reloaded successfully");
                    Log.d(TAG, "Email verified status: " + user.isEmailVerified());
                    listener.onSuccess();
                } else {
                    Log.e(TAG, "Failed to reload user data");
                    listener.onFailure("Failed to reload user");
                }
            });
        } else {
            Log.e(TAG, "Cannot reload: User not logged in");
            listener.onFailure("User not logged in");
        }
    }

    // ==================== TASK OPERATIONS ====================

    // Add new task
    public void addTask(Task task, OnTaskCompleteListener listener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.e(TAG, "Cannot add task: User not logged in");
            listener.onFailure("User not logged in");
            return;
        }

        String taskId = mDatabase.child("tasks").child(userId).push().getKey();

        if (taskId != null) {
            task.setFirebaseId(taskId);
            Log.d(TAG, "Adding task with ID: " + taskId);
            mDatabase.child("tasks").child(userId).child(taskId).setValue(task)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Task added successfully");
                        listener.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to add task: " + e.getMessage());
                        listener.onFailure(e.getMessage());
                    });
        } else {
            Log.e(TAG, "Failed to generate task ID");
            listener.onFailure("Failed to generate task ID");
        }
    }

    // Get all tasks
    public void getAllTasks(OnTasksLoadListener listener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.e(TAG, "Cannot load tasks: User not logged in");
            listener.onError("User not logged in");
            return;
        }

        Log.d(TAG, "Loading tasks for user: " + userId);
        mDatabase.child("tasks").child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Task> taskList = new ArrayList<>();
                for (DataSnapshot taskSnapshot : snapshot.getChildren()) {
                    Task task = taskSnapshot.getValue(Task.class);
                    if (task != null) {
                        task.setFirebaseId(taskSnapshot.getKey());
                        taskList.add(task);
                    }
                }
                Log.d(TAG, "Loaded " + taskList.size() + " tasks");
                listener.onTasksLoaded(taskList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load tasks: " + error.getMessage());
                listener.onError(error.getMessage());
            }
        });
    }

    // Update task status
    public void updateTaskStatus(String taskId, String status, OnTaskCompleteListener listener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.e(TAG, "Cannot update task: User not logged in");
            listener.onFailure("User not logged in");
            return;
        }

        Log.d(TAG, "Updating task " + taskId + " status to: " + status);
        mDatabase.child("tasks").child(userId).child(taskId).child("status").setValue(status)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Task status updated successfully");
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update task status: " + e.getMessage());
                    listener.onFailure(e.getMessage());
                });
    }

    // Delete task
    public void deleteTask(String taskId, OnTaskCompleteListener listener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.e(TAG, "Cannot delete task: User not logged in");
            listener.onFailure("User not logged in");
            return;
        }

        Log.d(TAG, "Deleting task: " + taskId);
        mDatabase.child("tasks").child(userId).child(taskId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Task deleted successfully");
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete task: " + e.getMessage());
                    listener.onFailure(e.getMessage());
                });
    }

    // ==================== CALLBACK INTERFACES ====================

    public interface OnAuthCompleteListener {
        void onSuccess(String userId);
        void onFailure(String error);
    }

    public interface OnTaskCompleteListener {
        void onSuccess();
        void onFailure(String error);
    }

    public interface OnTasksLoadListener {
        void onTasksLoaded(List<Task> tasks);
        void onError(String error);
    }
}
