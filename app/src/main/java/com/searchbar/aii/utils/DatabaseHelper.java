package com.searchbar.aii.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.searchbar.aii.models.Task;
import com.searchbar.aii.models.User;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "TodoListDB";
    private static final int DATABASE_VERSION = 1;

    // User Table
    private static final String TABLE_USERS = "users";
    private static final String COL_USER_ID = "id";
    private static final String COL_USERNAME = "username";
    private static final String COL_EMAIL = "email";
    private static final String COL_PASSWORD = "password";

    // Task Table
    private static final String TABLE_TASKS = "tasks";
    private static final String COL_TASK_ID = "id";
    private static final String COL_TITLE = "title";
    private static final String COL_DESCRIPTION = "description";
    private static final String COL_DUE_DATE = "due_date";
    private static final String COL_PRIORITY = "priority";
    private static final String COL_STATUS = "status";
    private static final String COL_ASSIGNED_TO = "assigned_to";
    private static final String COL_USER_ID_FK = "user_id";
    private static final String COL_CREATED_AT = "created_at";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createUsersTable = "CREATE TABLE " + TABLE_USERS + "("
                + COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COL_USERNAME + " TEXT,"
                + COL_EMAIL + " TEXT UNIQUE,"
                + COL_PASSWORD + " TEXT)";

        String createTasksTable = "CREATE TABLE " + TABLE_TASKS + "("
                + COL_TASK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COL_TITLE + " TEXT,"
                + COL_DESCRIPTION + " TEXT,"
                + COL_DUE_DATE + " TEXT,"
                + COL_PRIORITY + " TEXT,"
                + COL_STATUS + " TEXT,"
                + COL_ASSIGNED_TO + " TEXT,"
                + COL_USER_ID_FK + " INTEGER,"
                + COL_CREATED_AT + " INTEGER)";

        db.execSQL(createUsersTable);
        db.execSQL(createTasksTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TASKS);
        onCreate(db);
    }

    // ==================== USER METHODS ====================

    public boolean addUser(User user) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_USERNAME, user.getUsername());
        values.put(COL_EMAIL, user.getEmail());
        values.put(COL_PASSWORD, user.getPassword());

        long result = db.insert(TABLE_USERS, null, values);
        db.close();
        return result != -1;
    }

    public User loginUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null,
                COL_EMAIL + "=? AND " + COL_PASSWORD + "=?",
                new String[]{email, password}, null, null, null);

        User user = null;
        if (cursor.moveToFirst()) {
            user = new User();
            user.setId(cursor.getInt(0));
            user.setUsername(cursor.getString(1));
            user.setEmail(cursor.getString(2));
            user.setPassword(cursor.getString(3));
        }
        cursor.close();
        db.close();
        return user;
    }

    // ==================== TASK METHODS ====================

    public boolean addTask(Task task) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TITLE, task.getTitle());
        values.put(COL_DESCRIPTION, task.getDescription());
        values.put(COL_DUE_DATE, task.getDueDate());
        values.put(COL_PRIORITY, task.getPriority());
        values.put(COL_STATUS, task.getStatus());
        values.put(COL_ASSIGNED_TO, task.getAssignedTo());
        values.put(COL_USER_ID_FK, task.getUserId());
        values.put(COL_CREATED_AT, task.getCreatedAt());

        long result = db.insert(TABLE_TASKS, null, values);
        db.close();
        return result != -1;
    }

    public List<Task> getAllTasks(int userId) {
        List<Task> taskList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_TASKS, null, COL_USER_ID_FK + "=?",
                new String[]{String.valueOf(userId)}, null, null,
                COL_CREATED_AT + " DESC");

        if (cursor.moveToFirst()) {
            do {
                Task task = cursorToTask(cursor);
                taskList.add(task);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return taskList;
    }

    public List<Task> getTasksByStatus(int userId, String status) {
        List<Task> taskList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_TASKS, null,
                COL_USER_ID_FK + "=? AND " + COL_STATUS + "=?",
                new String[]{String.valueOf(userId), status}, null, null,
                COL_CREATED_AT + " DESC");

        if (cursor.moveToFirst()) {
            do {
                Task task = cursorToTask(cursor);
                taskList.add(task);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return taskList;
    }

    public List<Task> searchTasks(int userId, String query) {
        List<Task> taskList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_TASKS, null,
                COL_USER_ID_FK + "=? AND (" + COL_TITLE + " LIKE ? OR " +
                        COL_DESCRIPTION + " LIKE ?)",
                new String[]{String.valueOf(userId), "%" + query + "%", "%" + query + "%"},
                null, null, COL_CREATED_AT + " DESC");

        if (cursor.moveToFirst()) {
            do {
                Task task = cursorToTask(cursor);
                taskList.add(task);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return taskList;
    }

    public boolean updateTask(Task task) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TITLE, task.getTitle());
        values.put(COL_DESCRIPTION, task.getDescription());
        values.put(COL_DUE_DATE, task.getDueDate());
        values.put(COL_PRIORITY, task.getPriority());
        values.put(COL_STATUS, task.getStatus());
        values.put(COL_ASSIGNED_TO, task.getAssignedTo());

        int result = db.update(TABLE_TASKS, values, COL_TASK_ID + "=?",
                new String[]{String.valueOf(task.getId())});
        db.close();
        return result > 0;
    }

    // YE NAYA METHOD HAI - Task status ko update karne ke liye
    public boolean updateTaskStatus(int taskId, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_STATUS, status);

        int result = db.update(TABLE_TASKS, values, COL_TASK_ID + "=?",
                new String[]{String.valueOf(taskId)});
        db.close();
        return result > 0;
    }

    public boolean deleteTask(int taskId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_TASKS, COL_TASK_ID + "=?",
                new String[]{String.valueOf(taskId)});
        db.close();
        return result > 0;
    }

    // Helper method to convert Cursor to Task object
    private Task cursorToTask(Cursor cursor) {
        Task task = new Task();
        task.setId(cursor.getInt(0));
        task.setTitle(cursor.getString(1));
        task.setDescription(cursor.getString(2));
        task.setDueDate(cursor.getString(3));
        task.setPriority(cursor.getString(4));
        task.setStatus(cursor.getString(5));
        task.setAssignedTo(cursor.getString(6));
        task.setUserId(cursor.getInt(7));
        task.setCreatedAt(cursor.getLong(8));
        return task;
    }
}