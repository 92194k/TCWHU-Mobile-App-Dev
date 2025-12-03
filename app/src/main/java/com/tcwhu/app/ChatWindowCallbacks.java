package com.tcwhu.app;

import java.util.List;

/**
 * Interface for ChatDataManager and ChatFileUploader to communicate updates back to ChatWindowActivity.
 */
public interface ChatWindowCallbacks {
    void onMessageListUpdated(List<Message> newMessages, boolean shouldScrollToBottom);
    void onChatPartnerDetailsLoaded(String nickname, String yearLevel, String role);
    void onProgressVisibilityChanged(int visibility); // Used by ChatFileUploader
    void showToast(String message, int duration);
    void finishActivity();
    void setAdminWarningActionsVisibility(int visibility);
    void setInputContainerVisibility(int visibility);
}