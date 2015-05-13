package com.makeonthelake.sumobot;

public interface OnLaterViewChangeListener {
    void onMoveForward(int percentage);
    void onMoveBackward(int percentage);
    void onMoveStop();
}
