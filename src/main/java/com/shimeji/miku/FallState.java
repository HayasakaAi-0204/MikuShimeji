package com.shimeji.miku;

import java.awt.image.BufferedImage;

public class FallState implements MikuState {
    private int frameIndex = 0;
    private int velocityY = 0;
    private static final int GRAVITY = 2;
    private static final int MAX_FALL_SPEED = 30;

    @Override
    public void enter(MikuCharacter miku) {
        frameIndex = 0;
        velocityY = 0;
    }

    @Override
    public void updatePhysics(MikuCharacter miku, int floorY) {
        velocityY += GRAVITY;
        if (velocityY > MAX_FALL_SPEED)
            velocityY = MAX_FALL_SPEED;

        int newY = miku.getY() + velocityY;
        if (newY >= floorY) {
            miku.setY(floorY);
            miku.changeState(new IdleState());
        } else {
            miku.setY(newY);
        }
    }

    @Override
    public void updateAnimation(MikuCharacter miku) {
        frameIndex = (frameIndex + 1) % ResourceManager.TOTAL_FALL_FRAMES;
    }

    @Override
    public BufferedImage getCurrentImage() {
        return ResourceManager.getFallImage(frameIndex);
    }
}