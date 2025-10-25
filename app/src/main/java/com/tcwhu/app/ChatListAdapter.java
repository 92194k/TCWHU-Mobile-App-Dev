package com.tcwhu.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
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
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Chat chat = chatList.get(position);

        // Safety check to prevent crashes from broken data in Firestore.
        if (chat.getUsers() == null || chat.getUsers().isEmpty()) { // CORRECTED to getUsers()
            holder.itemView.setVisibility(View.GONE);
            holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
            return;
        }

        holder.itemView.setVisibility(View.VISIBLE);
        holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Find the ID of the person we are chatting with
        String otherUserId = chat.getUsers().stream() // CORRECTED to getUsers()
                .filter(id -> !id.equals(currentUserId))
                .findFirst().orElse(null);

        Student otherUser = studentMap.get(otherUserId);
        holder.bind(otherUser, chat, listener, otherUserId);
    }

    @Override
    public int getItemCount() { return chatList.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textAvatar, textNickname, textLastMessage, textTime;
        LinearLayout avatarContainer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textAvatar = itemView.findViewById(R.id.textAvatar);
            textNickname = itemView.findViewById(R.id.textNickname);
            textLastMessage = itemView.findViewById(R.id.textLastMessage);
            textTime = itemView.findViewById(R.id.textTime);
            avatarContainer = itemView.findViewById(R.id.avatarContainer);
        }

        public void bind(final Student otherUser, final Chat chat, final OnChatSelectedListener listener, final String otherUserId) {
            if (otherUser != null) {
                textNickname.setText(otherUser.getNickname());
                textAvatar.setText(otherUser.getAvatar());

                SimpleDateFormat formatter = new SimpleDateFormat("h:mm a", Locale.US);
                textTime.setText(formatter.format(new Date(chat.getTimestamp())));

                textLastMessage.setText(chat.getLastMessage());

                itemView.setOnClickListener(v -> listener.onChatSelected(otherUserId));
            } else {
                textNickname.setText("Archived User");
                textAvatar.setText("?");
                itemView.setOnClickListener(null);
            }
        }
    }
}