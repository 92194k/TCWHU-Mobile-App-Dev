package com.tcwhu.app;

/**
 * Interface to delegate message long-press actions outside of the adapter.
 */
public interface MessageOptionHandler {
    void showMessageOptions(Message message);
}