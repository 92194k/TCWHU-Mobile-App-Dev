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
        void onBan(String userId);
        void onUnsuspend(String userId);
        void onUnban(String userId);
        void onDelete(String userId);
    }

    private List<Student> studentList;
    private OnActionListener listener;

    public UserOverviewAdapter(List<Student> studentList, OnActionListener listener) {
        this.studentList = studentList;
        this.listener = listener;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_overview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Student student = studentList.get(position);
        holder.bind(student, listener);
    }

    @Override
    public int getItemCount() { return studentList.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textAvatar, textNickname, textStudentNumber, textStatus;
        MaterialButton buttonDynamicAction;
        ImageButton buttonDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textAvatar = itemView.findViewById(R.id.textAvatar);
            textNickname = itemView.findViewById(R.id.textNickname);
            textStudentNumber = itemView.findViewById(R.id.textStudentNumber);
            textStatus = itemView.findViewById(R.id.textStatus);
            buttonDynamicAction = itemView.findViewById(R.id.buttonDynamicAction);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
        }

        public void bind(final Student student, final OnActionListener listener) {
            String nickname = student.getNickname() != null ? student.getNickname() : "N/A";
            String studentNumber = student.getStudentNumber() != null ? student.getStudentNumber() : "N/A";
            String avatar = student.getAvatar() != null ? student.getAvatar() : "?";

            textAvatar.setText(avatar);
            textNickname.setText(nickname);
            textStudentNumber.setText(studentNumber);

            if (student.isBanned()) {
                setStatus(Color.parseColor("#B71C1C"), "BANNED");
                setDynamicAction("UNBAN", R.drawable.ic_shield_off, v -> listener.onUnban(student.getUserId()), Color.parseColor("#388E3C"));
            } else if (student.isSuspended()) {
                setStatus(Color.parseColor("#F57C00"), "SUSPENDED");
                setDynamicAction("UNSUSPEND", R.drawable.ic_clock, v -> listener.onUnsuspend(student.getUserId()), Color.parseColor("#FFD700"));
            } else if (!student.isVerified()) {
                setStatus(Color.parseColor("#FFA000"), "PENDING");
                setDynamicAction("REVIEW", R.drawable.ic_view, v -> {}, Color.parseColor("#6A0DAD"));
            } else {
                setStatus(Color.parseColor("#388E3C"), "VERIFIED");
                setDynamicAction("BAN", R.drawable.ic_ban, v -> listener.onBan(student.getUserId()), Color.parseColor("#B71C1C"));
            }

            buttonDelete.setOnClickListener(v -> listener.onDelete(student.getUserId()));
        }

        private void setStatus(int color, String text) {
            textStatus.setText(text);
            textStatus.setTextColor(color);
        }

        private void setDynamicAction(String text, int iconRes, View.OnClickListener clickListener, int color) {
            buttonDynamicAction.setText(text);
            buttonDynamicAction.setIconResource(iconRes);

            ColorStateList colorStateList = ColorStateList.valueOf(color);
            buttonDynamicAction.setTextColor(colorStateList);
            buttonDynamicAction.setIconTint(colorStateList);
            buttonDynamicAction.setStrokeColor(colorStateList);

            buttonDynamicAction.setOnClickListener(clickListener);
        }
    }
}
