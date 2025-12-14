package com.tcwhu.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportsAdapter extends RecyclerView.Adapter<ReportsAdapter.ViewHolder> {

    public interface OnActionListener {
        void onActionClick(Report report, String action);
    }

    private List<Report> reportList;
    private Map<String, String> userNicknameMap;
    private OnActionListener listener;

    public ReportsAdapter(List<Report> reportList, Map<String, String> userNicknameMap, OnActionListener listener) {
        this.reportList = reportList;
        this.userNicknameMap = userNicknameMap;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(reportList.get(position), userNicknameMap, listener);
    }

    @Override
    public int getItemCount() {
        return reportList != null ? reportList.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView reportTitle, reportDetails;
        Button buttonIssueWarning, buttonSuspend, buttonBan;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            reportTitle = itemView.findViewById(R.id.reportTitle);
            reportDetails = itemView.findViewById(R.id.reportDetails);
            buttonIssueWarning = itemView.findViewById(R.id.buttonIssueWarning);
            buttonSuspend = itemView.findViewById(R.id.buttonSuspend);
            buttonBan = itemView.findViewById(R.id.buttonBan);
        }

        public void bind(final Report report, final Map<String, String> userNicknameMap, final OnActionListener listener) {
            String reporterName = "Unknown User";
            if (userNicknameMap != null && report.getReporterId() != null) {
                String name = userNicknameMap.get(report.getReporterId());
                if (name != null) reporterName = name;
            }

            String reportedName = "Unknown User";
            if (userNicknameMap != null && report.getReportedUserId() != null) {
                String name = userNicknameMap.get(report.getReportedUserId());
                if (name != null) reportedName = name;
            }

            reportTitle.setText("Report from " + reporterName);

            SimpleDateFormat formatter = new SimpleDateFormat("MMM d, yyyy, h:mm a", Locale.getDefault());
            String formattedDate = formatter.format(new Date(report.getTimestamp()));

            String details = "Reported User: " + reportedName +
                    "\nReason: " + report.getReason() +
                    "\nTime: " + formattedDate;
            reportDetails.setText(details);

            buttonIssueWarning.setOnClickListener(v -> listener.onActionClick(report, "warning"));
            buttonSuspend.setOnClickListener(v -> listener.onActionClick(report, "suspend"));
            buttonBan.setOnClickListener(v -> listener.onActionClick(report, "ban"));
        }
    }
}