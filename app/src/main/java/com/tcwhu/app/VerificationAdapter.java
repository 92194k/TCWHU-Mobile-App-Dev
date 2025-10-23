package com.tcwhu.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class VerificationAdapter extends RecyclerView.Adapter<VerificationAdapter.ViewHolder> {

    public interface OnActionListener {
        void onApprove(Student student);
        void onReject(Student student);
    }

    private List<Student> studentList;
    private OnActionListener listener;

    public VerificationAdapter(List<Student> studentList, OnActionListener listener) {
        this.studentList = studentList;
        this.listener = listener;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_verification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Student student = studentList.get(position);
        holder.bind(student, listener);
    }

    @Override
    public int getItemCount() {
        return studentList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textStudentNumber, textNickname, textYearLevel, textInterests, textAvatar;
        Button buttonApprove, buttonReject;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textStudentNumber = itemView.findViewById(R.id.textStudentNumber);
            textNickname = itemView.findViewById(R.id.textNickname);
            textYearLevel = itemView.findViewById(R.id.textYearLevel);
            textInterests = itemView.findViewById(R.id.textInterests);
            textAvatar = itemView.findViewById(R.id.textAvatar);
            buttonApprove = itemView.findViewById(R.id.buttonApprove);
            buttonReject = itemView.findViewById(R.id.buttonReject);
        }

        public void bind(final Student student, final OnActionListener listener) {
            textStudentNumber.setText("Student Number: " + student.getStudentNumber());
            textNickname.setText("Nickname: " + student.getNickname());
            textYearLevel.setText("Year Level: " + student.getYearLevel());
            textInterests.setText("Interests: " + student.getInterests());
            textAvatar.setText(student.getAvatar());

            buttonApprove.setOnClickListener(v -> listener.onApprove(student));
            buttonReject.setOnClickListener(v -> listener.onReject(student));
        }
    }
}