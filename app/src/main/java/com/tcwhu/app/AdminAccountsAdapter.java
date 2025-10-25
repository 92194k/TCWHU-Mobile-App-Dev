package com.tcwhu.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminAccountsAdapter extends RecyclerView.Adapter<AdminAccountsAdapter.ViewHolder> {

    public interface OnRemoveListener {
        void onRemoveClick(AdminAccount admin);
    }

    private List<AdminAccount> adminList;
    private OnRemoveListener listener;

    public AdminAccountsAdapter(List<AdminAccount> adminList, OnRemoveListener listener) {
        this.adminList = adminList;
        this.listener = listener;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_account, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(adminList.get(position), listener);
    }

    @Override
    public int getItemCount() { return adminList.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textAdminEmail, textAdminRole, textAddedDate;
        ImageButton buttonRemoveAdmin;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textAdminEmail = itemView.findViewById(R.id.textAdminEmail);
            textAdminRole = itemView.findViewById(R.id.textAdminRole);
            textAddedDate = itemView.findViewById(R.id.textAddedDate);
            buttonRemoveAdmin = itemView.findViewById(R.id.buttonRemoveAdmin);
        }

        public void bind(final AdminAccount admin, final OnRemoveListener listener) {
            textAdminEmail.setText(admin.getEmail());
            textAdminRole.setText("Role: " + admin.getRole());

            SimpleDateFormat formatter = new SimpleDateFormat("MMM d, yyyy", Locale.US);
            textAddedDate.setText("Added: " + formatter.format(new Date(admin.getAddedDate())));

            // Only allow removal if the admin is NOT the Super Admin
            if ("Super Admin".equalsIgnoreCase(admin.getRole())) {
                buttonRemoveAdmin.setVisibility(View.GONE);
            } else {
                buttonRemoveAdmin.setVisibility(View.VISIBLE);
                buttonRemoveAdmin.setOnClickListener(v -> listener.onRemoveClick(admin));
            }
        }
    }
}