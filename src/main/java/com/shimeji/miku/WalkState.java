package com.shimeji.miku;

import java.awt.image.BufferedImage;

public class WalkState implements MikuState {
    private int frameIndex = 0;
    private int timer = 0;
    private boolean isLeft;

    public WalkState(boolean isLeft) { this.isLeft = isLeft; }

    @Override
    public void enter(MikuCharacter miku) {
        frameIndex = 0;
        timer = 150 + (int) (Math.random() * 300);
        if (miku.getScale() > 1.0) timer += (int)(miku.getWidth() / miku.getSpeed()) + 150; 
        miku.setFacingRight(!isLeft);
    }

    @Override
    public void updatePhysics(MikuCharacter miku, int floorY) {
        miku.setY(floorY);
        int speed = miku.getSpeed();
        int newX = isLeft ? miku.getX() - speed : miku.getX() + speed;
        miku.setX(newX);
        timer--;

        // 👉 TÍNH TOÁN BIÊN GIỚI TƯỜNG BÌNH THƯỜNG
        int leftLimit = -miku.getSidePadding();
        int rightLimit = miku.getScreenWidth() - miku.getWidth() + miku.getSidePadding();
        
        boolean isForcedWalk = (miku.getAppMode() == MikuCharacter.AppMode.GAMING) || miku.isForcedPomodoroWalk();
        
        // 👉 CẬP NHẬT: Ép Miku đi lọt thỏm 100% ra khỏi màn hình (khuất bóng) khi đi làm nhiệm vụ!
        if (miku.isForcedPomodoroWalk()) {
            leftLimit = -miku.getWidth();
            rightLimit = miku.getScreenWidth();
        }
        
        if (isLeft) {
            if (newX < leftLimit || (timer <= 0 && !isForcedWalk)) {
                if (newX < leftLimit) {
                    miku.setX(leftLimit);
                    miku.changeState(new ClimbState(true, true));
                    return;
                }
                miku.changeState(new IdleState());
            }
        } else {
            if (newX > rightLimit || (timer <= 0 && !isForcedWalk)) {
                if (newX > rightLimit) {
                    miku.setX(rightLimit);
                    miku.changeState(new ClimbState(false, true));
                    return;
                }
                miku.changeState(new IdleState());
            }
        }
    }

    @Override
    public void updateAnimation(MikuCharacter miku) { frameIndex = (frameIndex + 1) % ResourceManager.TOTAL_WALK_FRAMES; }
    @Override
    public BufferedImage getCurrentImage() { return ResourceManager.getWalkImage(frameIndex); }
}