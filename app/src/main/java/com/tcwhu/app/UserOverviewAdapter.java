package com.tcwhu.app;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class UserOverviewAdapter extends RecyclerView.Adapter<UserOverviewAdapter.ViewHolder> {

    public interface OnActionListener {
        void onSuspend(String userId);
        void onBan(String userId);
        void onUnsuspend(String userId);
        void onUnban(String userId);
        void onDelete(String userId);
        void onReview(Student student);
        void onDenyDeletion(String userId);
    }

    private List<Student> studentList;
    private OnActionListener listener;

    public UserOverviewAdapter(List<Student> studentList, OnActionListener listener) {
        this.studentList = studentList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_overview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(studentList.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return studentList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textAvatar, textNickname, textStudentNumber, textStatus;
        MaterialButton buttonSuspend, buttonBan;
        ImageButton buttonDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textAvatar = itemView.findViewById(R.id.textAvatar);
            textNickname = itemView.findViewById(R.id.textNickname);
            textStudentNumber = itemView.findViewById(R.id.textStudentNumber);
            textStatus = itemView.findViewById(R.id.textStatus);
            buttonSuspend = itemView.findViewById(R.id.buttonSuspendAction);
            buttonBan = itemView.findViewById(R.id.buttonBanReviewAction);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
        }

        public void bind(final Student student, final OnActionListener listener) {
            textAvatar.setText(student.getAvatar() != null ? student.getAvatar() : "👤");
            textNickname.setText(student.getNickname() != null ? student.getNickname() : "Unknown");

            buttonSuspend.setVisibility(View.GONE);
            buttonBan.setVisibility(View.GONE);
            buttonDelete.setImageTintList(ColorStateList.valueOf(Color.parseColor("#B71C1C")));
            textStudentNumber.setText(student.getStudentNumber() != null ? student.getStudentNumber() : "N/A");

            if (student.isDeletionRequested()) {
                setStatus("#D32F2F", "DELETION REQUEST");
                String reason = student.getDeletionReason() != null ? student.getDeletionReason() : "No reason given.";
                textStudentNumber.setText((student.getStudentNumber() != null ? student.getStudentNumber() : "N/A") + "\nReason: " + reason);

                configureButton(buttonSuspend, "DENY", Color.parseColor("#388E3C"), v -> listener.onDenyDeletion(student.getUserId()));
                buttonSuspend.setVisibility(View.VISIBLE);
                buttonDelete.setImageTintList(ColorStateList.valueOf(Color.parseColor("#388E3C")));

            } else if (student.isBanned()) {
                setStatus("#B71C1C", "BANNED");
                configureButton(buttonBan, "UNBAN", Color.parseColor("#388E3C"), v -> listener.onUnban(student.getUserId()));
                buttonBan.setVisibility(View.VISIBLE);

            } else if (student.isSuspended()) {
                setStatus("#FFA000", "SUSPENDED");
                configureButton(buttonSuspend, "UNSUSPEND", Color.parseColor("#388E3C"), v -> listener.onUnsuspend(student.getUserId()));
                buttonSuspend.setVisibility(View.VISIBLE);

            } else if (!student.isVerified()) {
                setStatus("#FFA000", "PENDING");
                configureButton(buttonBan, "REVIEW", Color.parseColor("#6A0DAD"), v -> listener.onReview(student));
                buttonBan.setVisibility(View.VISIBLE);

            } else {
                setStatus("#388E3C", "VERIFIED");
                configureButton(buttonSuspend, "SUSPEND", Color.parseColor("#F57C00"), v -> listener.onSuspend(student.getUserId()));
                configureButton(buttonBan, "BAN", Color.parseColor("#B71C1C"), v -> listener.onBan(student.getUserId()));
                buttonSuspend.setVisibility(View.VISIBLE);
                buttonBan.setVisibility(View.VISIBLE);
            }

            buttonDelete.setOnClickListener(v -> listener.onDelete(student.getUserId()));
        }

        private void setStatus(String color, String text) {
            textStatus.setText(text);
            textStatus.setTextColor(Color.parseColor(color));
        }

        private void configureButton(MaterialButton button, String label, int color, View.OnClickListener click) {
            button.setText(label);
            button.setTextColor(ColorStateList.valueOf(color));
            button.setStrokeColor(ColorStateList.valueOf(color));
            button.setIconTint(ColorStateList.valueOf(color));
            button.setOnClickListener(click);
        }
    }
}