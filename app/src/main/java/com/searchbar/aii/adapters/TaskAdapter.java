package com.searchbar.aii.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.searchbar.aii.R;
import com.searchbar.aii.models.Task;
import com.searchbar.aii.utils.FirebaseHelper;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private Context context;
    private List<Task> tasks;
    private FirebaseHelper firebaseHelper;

    public TaskAdapter(Context context, List<Task> tasks, FirebaseHelper firebaseHelper) {
        this.context = context;
        this.tasks = tasks;
        this.firebaseHelper = firebaseHelper;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);

        // Bind data to views
        holder.titleTextView.setText(task.getTitle());
        holder.descriptionTextView.setText(task.getDescription());
        holder.dueDateTextView.setText("Due: " + task.getDueDate());
        holder.priorityTextView.setText(task.getPriority());
        holder.statusCheckBox.setChecked(task.getStatus().equals("Completed"));

        // Set priority color
        switch (task.getPriority()) {
            case "High":
                holder.priorityTextView.setBackgroundColor(
                        context.getResources().getColor(R.color.high_priority));
                break;
            case "Medium":
                holder.priorityTextView.setBackgroundColor(
                        context.getResources().getColor(R.color.medium_priority));
                break;
            case "Low":
                holder.priorityTextView.setBackgroundColor(
                        context.getResources().getColor(R.color.low_priority));
                break;
        }

        // ✅ FIXED: Remove previous listeners to prevent duplicates
        holder.statusCheckBox.setOnCheckedChangeListener(null);

        // Set checkbox state
        holder.statusCheckBox.setChecked(task.getStatus().equals("Completed"));

        // ✅ FIXED: Checkbox listener - Use holder.getBindingAdapterPosition()
        holder.statusCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                Task currentTask = tasks.get(adapterPosition);
                String status = isChecked ? "Completed" : "Pending";
                currentTask.setStatus(status);

                // Update in Firebase
                if (currentTask.getFirebaseId() != null) {
                    firebaseHelper.updateTaskStatus(currentTask.getFirebaseId(), status,
                            new FirebaseHelper.OnTaskCompleteListener() {
                                @Override
                                public void onSuccess() {
                                    Toast.makeText(context, "Status updated", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onFailure(String error) {
                                    Toast.makeText(context, "Failed to update: " + error,
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            }
        });

        // ✅ FIXED: Delete button listener - Use holder.getBindingAdapterPosition()
        holder.deleteButton.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                Task currentTask = tasks.get(adapterPosition);
                showDeleteDialog(currentTask, adapterPosition);
            }
        });
    }

    private void showDeleteDialog(Task task, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Delete Task")
                .setMessage("Are you sure you want to delete this task?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (task.getFirebaseId() != null) {
                        firebaseHelper.deleteTask(task.getFirebaseId(),
                                new FirebaseHelper.OnTaskCompleteListener() {
                                    @Override
                                    public void onSuccess() {
                                        // ✅ Check if position is still valid before removing
                                        if (position >= 0 && position < tasks.size()) {
                                            tasks.remove(position);
                                            notifyItemRemoved(position);
                                            notifyItemRangeChanged(position, tasks.size());
                                        }
                                        Toast.makeText(context, "Task deleted",
                                                Toast.LENGTH_SHORT).show();
                                    }

                                    @Override
                                    public void onFailure(String error) {
                                        Toast.makeText(context, "Failed to delete: " + error,
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    public void updateTasks(List<Task> newTasks) {
        this.tasks = newTasks;
        notifyDataSetChanged();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView, descriptionTextView, dueDateTextView, priorityTextView;
        CheckBox statusCheckBox;
        ImageButton deleteButton;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            descriptionTextView = itemView.findViewById(R.id.descriptionTextView);
            dueDateTextView = itemView.findViewById(R.id.dueDateTextView);
            priorityTextView = itemView.findViewById(R.id.priorityTextView);
            statusCheckBox = itemView.findViewById(R.id.statusCheckBox);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}
