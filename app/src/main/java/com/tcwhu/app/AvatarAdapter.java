package com.tcwhu.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AvatarAdapter extends RecyclerView.Adapter<AvatarAdapter.AvatarViewHolder> {

    private List<String> avatarList;
    private OnAvatarListener listener;
    private int selectedPosition = RecyclerView.NO_POSITION;

    public interface OnAvatarListener {
        void onAvatarClick(String avatar);
    }

    public AvatarAdapter(List<String> avatarList, OnAvatarListener listener) {
        this.avatarList = avatarList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AvatarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_avatar, parent, false);
        return new AvatarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AvatarViewHolder holder, int position) {
        String avatar = avatarList.get(position);
        holder.bind(avatar, listener);

        if (position == selectedPosition) {
            holder.itemView.setBackgroundResource(R.drawable.bg_avatar_grid_item_selected);
        } else {
            holder.itemView.setBackgroundResource(android.R.color.transparent);
        }
    }

    @Override
    public int getItemCount() {
        return avatarList.size();
    }

    public void setSelectedPosition(int position) {
        int previousPosition = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(previousPosition);
        notifyItemChanged(selectedPosition);
    }

    // --- THIS IS THE NEW HELPER METHOD --- ✅
    // This method allows the Activity to safely update the list of avatars.
    public void updateAvatars(List<String> newAvatars) {
        this.avatarList.clear();
        this.avatarList.addAll(newAvatars);
        notifyDataSetChanged();
    }

    static class AvatarViewHolder extends RecyclerView.ViewHolder {
        TextView avatarTextView;

        public AvatarViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarTextView = itemView.findViewById(R.id.avatarTextView);
        }

        public void bind(final String avatar, final OnAvatarListener listener) {
            avatarTextView.setText(avatar);
            itemView.setOnClickListener(v -> {
                listener.onAvatarClick(avatar);
            });
        }
    }
}