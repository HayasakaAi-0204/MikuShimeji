package com.shimeji.miku;

import java.awt.Graphics2D;

public interface ThrowableItem {
    void hold(MikuCharacter miku);
    void toss(MikuCharacter miku);
    void updatePhysics(int floorY);
    void draw(Graphics2D g2d);
    void setInactive();
    boolean isActive();
    int getX();
    int getY();
    double getScale(); // Hệ số phóng to
}