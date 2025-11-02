package com.tcwhu.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ActivityLogsAdapter extends RecyclerView.Adapter<ActivityLogsAdapter.ViewHolder> {

    private List<ActivityLog> logList;

    // NOTE: Assuming you want to keep the OnItemClickListener interface if you use it later.
    // If not used, you can simplify the constructor. Keeping it here for full compatibility.
    public interface OnItemClickListener {
        void onItemClick(ActivityLog log);
    }
    private OnItemClickListener listener;


    public ActivityLogsAdapter(List<ActivityLog> logList, OnItemClickListener listener) {
        this.logList = logList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_activity_log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(logList.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return logList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textLogIcon, textAction, textAdminId, textTimestamp, textTargetId;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textLogIcon = itemView.findViewById(R.id.textLogIcon);
            textAction = itemView.findViewById(R.id.textAction);
            textAdminId = itemView.findViewById(R.id.textAdminId);
            textTimestamp = itemView.findViewById(R.id.textTimestamp);
            textTargetId = itemView.findViewById(R.id.textTargetId);
        }

        private String safeTrim(String text) {
            if (text == null) return "";
            // Assuming adminId might be long Firebase UID or a short nickname
            return text.length() > 20 ? text.substring(0, 20) + "..." : text;
        }

        public void bind(final ActivityLog log, final OnItemClickListener listener) {
            // Get the Admin's full name/nickname (stored in adminId field)
            String adminNickname = log.getAdminId();

            // Display main info
            textAction.setText(log.getAction());
            // CRITICAL FIX: Display the actual nickname
            textAdminId.setText("Logged by: " + adminNickname);

            // Format timestamp
            SimpleDateFormat formatter = new SimpleDateFormat("MMM d, yyyy, h:mm a", Locale.US);
            textTimestamp.setText(formatter.format(new Date(log.getTimestamp())));

            // Set icons dynamically (unchanged)
            String action = log.getAction().toLowerCase();
            if (action.contains("ban") || action.contains("suspend")) {
                textLogIcon.setText("🚫");
            } else if (action.contains("approv") || action.contains("verify")) {
                textLogIcon.setText("✅");
            } else if (action.contains("event")) {
                textLogIcon.setText("📅");
            } else {
                textLogIcon.setText("📝");
            }

            // Show Target ID if applicable (unchanged)
            if (log.getTargetId() != null && !log.getTargetId().isEmpty()) {
                textTargetId.setText("Target ID: " + safeTrim(log.getTargetId()));
                textTargetId.setVisibility(View.VISIBLE);
            } else {
                textTargetId.setVisibility(View.GONE);
            }

            // Click listener (for future detail view)
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(log);
            });
        }
    }
}