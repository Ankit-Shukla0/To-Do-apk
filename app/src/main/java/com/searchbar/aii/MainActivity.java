package com.searchbar.aii;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.searchbar.aii.adapters.TaskAdapter;
import com.searchbar.aii.models.Task;
import com.searchbar.aii.utils.FirebaseHelper;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private FloatingActionButton fabAddTask;
    private EditText searchEditText;
    private Button filterButton;

    private TaskAdapter taskAdapter;
    private FirebaseHelper firebaseHelper;
    private SharedPreferences sharedPreferences;

    private String currentFilter = "All";
    private List<Task> allTasks = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Helper
        firebaseHelper = new FirebaseHelper(this);
        sharedPreferences = getSharedPreferences("TodoAppPrefs", MODE_PRIVATE);

        // Check if user is logged in
        if (!firebaseHelper.isUserLoggedIn()) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        // Initialize Views
        initViews();

        // Setup Components
        setupToolbar();
        setupDrawer();
        setupTabs();
        setupRecyclerView();
        setupSearch();

        // Load Tasks from Firebase
        loadTasksFromFirebase();

        // FAB Click Listener
        fabAddTask.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, AddTaskActivity.class));
        });

        // Filter Button
        filterButton.setOnClickListener(v -> showFilterMenu(v));

        // Handle back press
        setupBackPressHandler();
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        toolbar = findViewById(R.id.toolbar);
        tabLayout = findViewById(R.id.tabLayout);
        recyclerView = findViewById(R.id.recyclerView);
        fabAddTask = findViewById(R.id.fabAddTask);
        searchEditText = findViewById(R.id.searchEditText);
        filterButton = findViewById(R.id.filterButton);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("My Tasks");
        }
    }

    private void setupDrawer() {
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.app_name, R.string.app_name);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.menu_all_tasks) {
                currentFilter = "All";
                filterTasks();
            } else if (id == R.id.menu_pending) {
                currentFilter = "Pending";
                filterTasks();
            } else if (id == R.id.menu_completed) {
                currentFilter = "Completed";
                filterTasks();
            } else if (id == R.id.menu_logout) {
                showLogoutDialog();
            }

            drawerLayout.closeDrawers();
            return true;
        });
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("All"));
        tabLayout.addTab(tabLayout.newTab().setText("Pending"));
        tabLayout.addTab(tabLayout.newTab().setText("Completed"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentFilter = tab.getText().toString();
                filterTasks();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new TaskAdapter(this, new ArrayList<>(), firebaseHelper);
        recyclerView.setAdapter(taskAdapter);
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    searchTasks(s.toString());
                } else {
                    filterTasks();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadTasksFromFirebase() {
        firebaseHelper.getAllTasks(new FirebaseHelper.OnTasksLoadListener() {
            @Override
            public void onTasksLoaded(List<Task> tasks) {
                allTasks = tasks;
                filterTasks();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(MainActivity.this,
                        "Error loading tasks: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterTasks() {
        List<Task> filteredTasks = new ArrayList<>();

        if (currentFilter.equals("All")) {
            filteredTasks = new ArrayList<>(allTasks);
        } else {
            for (Task task : allTasks) {
                if (task.getStatus().equals(currentFilter)) {
                    filteredTasks.add(task);
                }
            }
        }

        taskAdapter.updateTasks(filteredTasks);
    }

    private void searchTasks(String query) {
        List<Task> searchResults = new ArrayList<>();
        query = query.toLowerCase();

        for (Task task : allTasks) {
            if (task.getTitle().toLowerCase().contains(query) ||
                    task.getDescription().toLowerCase().contains(query)) {
                searchResults.add(task);
            }
        }

        taskAdapter.updateTasks(searchResults);
    }

    private void showFilterMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.filter_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            Toast.makeText(MainActivity.this,
                    "Filter: " + item.getTitle(), Toast.LENGTH_SHORT).show();
            return true;
        });

        popup.show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> logout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logout() {
        firebaseHelper.logout();

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupBackPressHandler() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(navigationView)) {
                    drawerLayout.closeDrawers();
                } else {
                    showExitDialog();
                }
            }
        };

        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void showExitDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Exit App")
                .setMessage("Do you want to exit?")
                .setPositiveButton("Yes", (dialog, which) -> finishAffinity())
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTasksFromFirebase();
    }
}
