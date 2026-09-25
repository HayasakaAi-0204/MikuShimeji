package com.shimeji.miku;

import java.awt.image.BufferedImage;

public class WalkState implements MikuState {
    private int frameIndex = 0;
    private int timer = 0;
    private boolean isLeft;

    public WalkState(boolean isLeft) {
        this.isLeft = isLeft;
    }

    @Override
    public void enter(MikuCharacter miku) {
        frameIndex = 0;
        timer = 150 + (int) (Math.random() * 300);
        miku.setFacingRight(!isLeft);
    }

    @Override
    public void updatePhysics(MikuCharacter miku, int floorY) {
        miku.setY(floorY);
        int speed = miku.getSpeed();
        int newX = isLeft ? miku.getX() - speed : miku.getX() + speed;
        miku.setX(newX);
        timer--;

        int screenLimit = miku.getScreenWidth() - miku.getWidth() + miku.getSidePadding();
        
        if (isLeft) {
            if (newX < -miku.getSidePadding() || timer <= 0) {
                if (newX < -miku.getSidePadding()) {
                    miku.setX(-miku.getSidePadding());
                    // CẬP NHẬT: 100% leo lên khi đụng tường (Thay vì 50% như cũ)
                    miku.changeState(new ClimbState(true, true));
                    return;
                }
                miku.changeState(new IdleState());
            }
        } else {
            if (newX > screenLimit || timer <= 0) {
                if (newX > screenLimit) {
                    miku.setX(screenLimit);
                    // CẬP NHẬT: 100% leo lên khi đụng tường
                    miku.changeState(new ClimbState(false, true));
                    return;
                }
                miku.changeState(new IdleState());
            }
        }
    }

    @Override
    public void updateAnimation(MikuCharacter miku) {
        frameIndex = (frameIndex + 1) % ResourceManager.TOTAL_WALK_FRAMES;
    }

    @Override
    public BufferedImage getCurrentImage() {
        return ResourceManager.getWalkImage(frameIndex);
    }
}