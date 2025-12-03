package com.tcwhu.app;

/**
 * Interface to delegate message long-press actions outside of the adapter/view holder.
 */
// NOTE: Placeholder class (Message) is assumed to exist.
public interface MessageOptionHandler {
    void showMessageOptions(Message message);
}