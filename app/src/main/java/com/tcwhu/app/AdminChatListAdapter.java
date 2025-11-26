package com.tcwhu.app;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminChatListAdapter extends RecyclerView.Adapter<AdminChatListAdapter.ViewHolder> {

    public interface OnChatClickListener {
        void onChatClick(Chat chat);
    }

    private List<Chat> chatList;
    private Map<String, Student> studentMap;
    private OnChatClickListener listener;
    private String adminId = ReportsManagementActivity.ADMIN_USER_ID;

    public AdminChatListAdapter(List<Chat> chatList, Map<String, Student> studentMap, OnChatClickListener listener) {
        this.chatList = chatList;
        this.studentMap = studentMap;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_chat_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Chat chat = chatList.get(position);

        // Get the student's ID
        String studentId = null;
        if (chat.getUsers() != null) {
            for (String id : chat.getUsers()) {
                if (!id.equals(adminId)) {
                    studentId = id;
                    break;
                }
            }
        }

        Student student = studentMap.get(studentId);
        holder.bind(chat, student, listener, adminId);
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textAvatar, textNickname, textLastMessage, textTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textAvatar = itemView.findViewById(R.id.textAvatar);
            textNickname = itemView.findViewById(R.id.textNickname);
            textLastMessage = itemView.findViewById(R.id.textLastMessage);
            textTime = itemView.findViewById(R.id.textTime);
        }

        public void bind(final Chat chat, final Student student, final OnChatClickListener listener, String adminId) {
            if (student != null) {
                textAvatar.setText(student.getAvatar() != null ? student.getAvatar() : "?");
                textNickname.setText(student.getNickname() != null ? student.getNickname() : "Unknown User");
            } else {
                textAvatar.setText("?");
                textNickname.setText("Deleted User");
            }

            textLastMessage.setText(chat.getLastMessage());

            if (chat.getTimestamp() > 0) {
                SimpleDateFormat formatter = new SimpleDateFormat("h:mm a", Locale.getDefault());
                textTime.setText(formatter.format(new Date(chat.getTimestamp())));
            } else {
                textTime.setText("");
            }

            // Unread (naka bold), Read (dili naka bold)
            boolean isUnread = chat.getLastSenderId() != null
                    && !chat.getLastSenderId().equals(adminId)
                    && !chat.isRead();

            if (isUnread) {
                textNickname.setTypeface(null, Typeface.BOLD);
                textLastMessage.setTypeface(null, Typeface.BOLD);
                textTime.setTypeface(null, Typeface.BOLD);
                textLastMessage.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_primary));
                textTime.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.deep_purple));
            } else {
                textNickname.setTypeface(null, Typeface.NORMAL);
                textLastMessage.setTypeface(null, Typeface.NORMAL);
                textTime.setTypeface(null, Typeface.NORMAL);
                textLastMessage.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_secondary));
                textTime.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_secondary));
            }

            itemView.setOnClickListener(v -> listener.onChatClick(chat));
        }
    }
}