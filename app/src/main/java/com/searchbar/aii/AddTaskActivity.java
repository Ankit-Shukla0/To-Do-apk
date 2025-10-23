package com.searchbar.aii;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.searchbar.aii.models.Task;
import com.searchbar.aii.utils.FirebaseHelper;

import java.util.Calendar;

public class AddTaskActivity extends AppCompatActivity {

    private EditText titleEditText, descriptionEditText, dueDateEditText, assignedToEditText;
    private Spinner prioritySpinner;
    private Button saveTaskButton;
    private ProgressBar progressBar;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        // Setup toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Add New Task");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize Firebase Helper
        firebaseHelper = new FirebaseHelper(this);

        // Initialize Views
        initViews();

        // Setup components
        setupPrioritySpinner();
        setupDatePicker();

        // Save button click listener
        saveTaskButton.setOnClickListener(v -> saveTask());

        // Handle back press
        setupBackPressHandler();
    }

    private void initViews() {
        titleEditText = findViewById(R.id.titleEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        dueDateEditText = findViewById(R.id.dueDateEditText);
        assignedToEditText = findViewById(R.id.assignedToEditText);
        prioritySpinner = findViewById(R.id.prioritySpinner);
        saveTaskButton = findViewById(R.id.saveTaskButton);
        progressBar = findViewById(R.id.progress_bar);

        // Initially hide progress bar
        progressBar.setVisibility(View.GONE);
    }

    private void setupPrioritySpinner() {
        String[] priorities = {"High", "Medium", "Low"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                priorities
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prioritySpinner.setAdapter(adapter);
    }

    private void setupDatePicker() {
        dueDateEditText.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    AddTaskActivity.this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        String date = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                        dueDateEditText.setText(date);
                    },
                    year, month, day
            );
            datePickerDialog.show();
        });
    }

    private void saveTask() {
        // Get input values
        String title = titleEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String dueDate = dueDateEditText.getText().toString().trim();
        String priority = prioritySpinner.getSelectedItem().toString();
        String assignedTo = assignedToEditText.getText().toString().trim();

        // Validate title
        if (title.isEmpty()) {
            titleEditText.setError("Please enter task title");
            titleEditText.requestFocus();
            return;
        }

        // Show loading
        showLoading(true);

        // Create task object
        Task task = new Task(title, description, dueDate, priority, "Pending", assignedTo, 0);

        // Add to Firebase
        firebaseHelper.addTask(task, new FirebaseHelper.OnTaskCompleteListener() {
            @Override
            public void onSuccess() {
                showLoading(false);
                Toast.makeText(AddTaskActivity.this,
                        "Task added successfully!", Toast.LENGTH_SHORT).show();
                finish(); // Return to MainActivity
            }

            @Override
            public void onFailure(String error) {
                showLoading(false);
                Toast.makeText(AddTaskActivity.this,
                        "Failed to add task: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            saveTaskButton.setEnabled(false);
            saveTaskButton.setText("Saving...");
        } else {
            progressBar.setVisibility(View.GONE);
            saveTaskButton.setEnabled(true);
            saveTaskButton.setText("SAVE TASK");
        }
    }

    private void setupBackPressHandler() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish(); // Go back to MainActivity
            }
        };

        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
