package com.tcwhu.app;

import java.util.List;

public interface ChatWindowCallbacks {
    void onMessageListUpdated(List<Message> newMessages, boolean shouldScrollToBottom);
    void onChatPartnerDetailsLoaded(String nickname, String yearLevel, String role);
    void onProgressVisibilityChanged(int visibility);
    void showToast(String message, int duration);
    void finishActivity();
    void setAdminWarningActionsVisibility(int visibility);
    void setInputContainerVisibility(int visibility);
}