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

    public ActivityLogsAdapter(List<ActivityLog> logList) {
        this.logList = logList;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_activity_log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(logList.get(position));
    }

    @Override
    public int getItemCount() { return logList.size(); }

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

        public void bind(final ActivityLog log) {
            // Get Nickname from Admin ID (since we don't have a map here, we use a placeholder)
            String adminNickname = log.getAdminId().substring(0, 8) + "...";

            textAction.setText(log.getAction());
            textAdminId.setText("Admin: " + adminNickname);

            SimpleDateFormat formatter = new SimpleDateFormat("MMM d, yyyy, h:mm a", Locale.US);
            textTimestamp.setText(formatter.format(new Date(log.getTimestamp())));

            // Set icon based on action (matching the TSX logic)
            String action = log.getAction().toLowerCase();
            if (action.contains("ban") || action.contains("suspend")) {
                textLogIcon.setText("🚫");
            } else if (action.contains("approv")) {
                textLogIcon.setText("✅");
            } else if (action.contains("event")) {
                textLogIcon.setText("📅");
            } else {
                textLogIcon.setText("📝");
            }

            // Show target ID if it exists
            if (log.getTargetId() != null && !log.getTargetId().isEmpty()) {
                textTargetId.setText("Target ID: " + log.getTargetId().substring(0, 8) + "...");
                textTargetId.setVisibility(View.VISIBLE);
            } else {
                textTargetId.setVisibility(View.GONE);
            }
        }
    }
}