package com.tcwhu.app;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ViewHolder> {

    public interface OnChatSelectedListener {
        void onChatSelected(String otherUserId);
    }

    private List<Chat> chatList;
    private Map<String, Student> studentMap;
    private OnChatSelectedListener listener;
    private String currentUserId;

    public ChatListAdapter(List<Chat> chatList, Map<String, Student> studentMap, OnChatSelectedListener listener) {
        this.chatList = chatList;
        this.studentMap = studentMap;
        this.listener = listener;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) this.currentUserId = user.getUid();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Chat chat = chatList.get(position);
        String otherUserId = null;
        if (chat.getUsers() != null) {
            for (String id : chat.getUsers()) {
                if (!id.equals(currentUserId)) {
                    otherUserId = id;
                    break;
                }
            }
        }

        Student otherUser = studentMap.get(otherUserId);
        if (otherUser != null && currentUserId != null) {
            holder.showItem();
            holder.bind(otherUser, chat, listener, otherUserId, currentUserId);
        } else {
            holder.hideItem();
        }
    }

    @Override
    public int getItemCount() {
        return chatList != null ? chatList.size() : 0;
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

        public void hideItem() {
            itemView.setVisibility(View.GONE);
            itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
        }

        public void showItem() {
            itemView.setVisibility(View.VISIBLE);
            itemView.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        public void bind(final Student otherUser, final Chat chat, final OnChatSelectedListener listener, final String otherUserId, final String currentUserId) {
            textNickname.setText(otherUser.getNickname() != null ? otherUser.getNickname() : "Unknown");
            textAvatar.setText(otherUser.getAvatar() != null ? otherUser.getAvatar() : "?");
            textLastMessage.setText(chat.getLastMessage());

            if (chat.getTimestamp() > 0) {
                SimpleDateFormat formatter = new SimpleDateFormat("h:mm a", Locale.getDefault());
                textTime.setText(formatter.format(new Date(chat.getTimestamp())));
            }

            boolean isNewMessage = chat.getLastSenderId() != null
                    && !chat.getLastSenderId().equals(currentUserId)
                    && !chat.isRead();

            if (isNewMessage) {
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

            itemView.setOnClickListener(v -> listener.onChatSelected(otherUserId));
        }
    }
}