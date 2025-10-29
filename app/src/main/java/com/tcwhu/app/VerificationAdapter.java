package com.tcwhu.app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class VerificationAdapter extends RecyclerView.Adapter<VerificationAdapter.ViewHolder> {

    public interface OnActionListener {
        void onApprove(Student student);
        void onReject(Student client);
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
        holder.bind(studentList.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return studentList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textStudentNumber, textNickname, textYearLevel, textEmail;
        ImageView selfiePhotoPreview, idPhotoPreview;
        Button buttonApprove, buttonReject;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textStudentNumber = itemView.findViewById(R.id.textStudentNumber);
            textNickname = itemView.findViewById(R.id.textNickname);
            textEmail = itemView.findViewById(R.id.textEmail);
            textYearLevel = itemView.findViewById(R.id.textYearLevel);
            selfiePhotoPreview = itemView.findViewById(R.id.selfiePhotoPreview);
            idPhotoPreview = itemView.findViewById(R.id.idPhotoPreview);
            buttonApprove = itemView.findViewById(R.id.buttonApprove);
            buttonReject = itemView.findViewById(R.id.buttonReject);
        }

        public void bind(final Student student, final OnActionListener listener) {
            final Context context = itemView.getContext();
            final String emailAddress = student.getEmail();

            textStudentNumber.setText("Student Number: " + student.getStudentNumber());
            textNickname.setText("Nickname: " + student.getNickname());
            textEmail.setText("Email: " + emailAddress);
            textYearLevel.setText("Year Level: " + student.getYearLevel());

            // --- CRITICAL FIX: Make the TextView itself clickable --- ✅
            textEmail.setOnClickListener(v -> launchEmailClient(context, emailAddress));

            // Load images with Glide
            if (student.getSelfiePhotoUrl() != null && !student.getSelfiePhotoUrl().isEmpty()) {
                Glide.with(context).load(student.getSelfiePhotoUrl()).into(selfiePhotoPreview);
                selfiePhotoPreview.setOnClickListener(v -> openPhotoViewer(context, student.getSelfiePhotoUrl()));
            }
            if (student.getIdPhotoUrl() != null && !student.getIdPhotoUrl().isEmpty()) {
                Glide.with(context).load(student.getIdPhotoUrl()).into(idPhotoPreview);
                idPhotoPreview.setOnClickListener(v -> openPhotoViewer(context, student.getIdPhotoUrl()));
            }

            buttonApprove.setOnClickListener(v -> listener.onApprove(student));
            buttonReject.setOnClickListener(v -> listener.onReject(student));
        }

        private void openPhotoViewer(Context context, String imageUrl) {
            Intent intent = new Intent(context, ViewPhotoActivity.class);
            intent.putExtra(ViewPhotoActivity.EXTRA_IMAGE_URL, imageUrl);
            context.startActivity(intent);
        }

        private void launchEmailClient(Context context, String emailAddress) {
            if (emailAddress != null && !emailAddress.isEmpty()) {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                emailIntent.setData(Uri.parse("mailto:" + emailAddress));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "TCWHU Account Verification Status");
                emailIntent.putExtra(Intent.EXTRA_TEXT, "Dear Admin,\n\nI have reviewed your account details. Please log in to complete your account setup.");

                try {
                    context.startActivity(emailIntent);
                } catch (Exception e) {
                    Toast.makeText(context, "No email client found.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "Student email not available.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}