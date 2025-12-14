package com.tcwhu.app;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.List;
import java.util.stream.Collectors;

public class StudentFinderAdapter extends RecyclerView.Adapter<StudentFinderAdapter.ViewHolder> {

    public interface OnChatStartListener {
        void onChatStart(Student student);
        void onSkip(int position);
    }

    private List<Student> studentList;
    private OnChatStartListener listener;

    public StudentFinderAdapter(List<Student> studentList, OnChatStartListener listener) {
        this.studentList = studentList;
        this.listener = listener;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student_finder, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(studentList.get(position), listener, position);
    }

    @Override
    public int getItemCount() { return studentList.size(); }

    public List<String> getAvatarList() {
        return studentList.stream().map(Student::getAvatar).collect(Collectors.toList());
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textAvatar, textNickname, textYearLevel;
        MaterialButton buttonConnect, buttonSkip;
        FrameLayout avatarContainer;
        LinearLayout interestsContainer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textAvatar = itemView.findViewById(R.id.textAvatar);
            textNickname = itemView.findViewById(R.id.textNickname);
            textYearLevel = itemView.findViewById(R.id.textYearLevel);
            buttonConnect = itemView.findViewById(R.id.buttonConnect);
            buttonSkip = itemView.findViewById(R.id.buttonSkip);
            avatarContainer = itemView.findViewById(R.id.avatarContainer);
            interestsContainer = itemView.findViewById(R.id.interestsContainer);
        }

        public void bind(final Student student, final OnChatStartListener listener, final int position) {
            String nickname = (student.getNickname() != null) ? student.getNickname() : "Unknown";
            String yearLevel = (student.getYearLevel() != null) ? student.getYearLevel() : "";
            String avatar = student.getAvatar();

            if (avatar == null || avatar.isEmpty()) {
                avatar = nickname.isEmpty() ? "?" : nickname.substring(0, 1).toUpperCase();
            }

            textAvatar.setText(avatar);
            textNickname.setText(nickname);
            textYearLevel.setText(yearLevel);

            GradientDrawable gradient = new GradientDrawable(
                    GradientDrawable.Orientation.BR_TL,
                    new int[]{Color.parseColor("#6A0DAD"), Color.parseColor("#5A0B92")});
            gradient.setCornerRadius(0f);
            avatarContainer.setBackground(gradient);

            interestsContainer.removeAllViews();
            String interestsString = student.getInterests();
            if (interestsString != null && !interestsString.isEmpty()) {
                String[] interests = interestsString.split(",");
                for (int i = 0; i < Math.min(interests.length, 3); i++) {
                    TextView tag = new TextView(itemView.getContext());
                    tag.setText(interests[i].trim());
                    tag.setTextSize(10);
                    tag.setPadding(10, 5, 10, 5);
                    tag.setBackgroundResource(R.drawable.bg_tag_red);
                    tag.setTextColor(Color.parseColor("#6A0DAD"));
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    params.setMargins(0, 0, 8, 0);
                    tag.setLayoutParams(params);
                    interestsContainer.addView(tag);
                }
            }

            buttonConnect.setOnClickListener(v -> listener.onChatStart(student));
            buttonSkip.setOnClickListener(v -> listener.onSkip(getAdapterPosition()));
        }
    }
}