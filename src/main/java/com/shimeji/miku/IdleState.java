package com.shimeji.miku;

import java.awt.image.BufferedImage;

public class IdleState implements MikuState {
    private int frameIndex = 0;
    private int timer = 0;

    @Override
    public void enter(MikuCharacter miku) {
        frameIndex = (int) (Math.random() * ResourceManager.TOTAL_IDLE_FRAMES);
        timer = 30 + (int) (Math.random() * 273); // Đứng im từ 1 đến 10 giây

        // RÀ SOÁT LỖI: Tránh trường hợp Miku vô tình bị rơi lọt ra ngoài mép màn hình 
        // (Vd: ném quá mạnh ra ngoài góc) khiến cô bé bị "tàng hình" lúc đáp xuống.
        int screenLimit = miku.getScreenWidth() - miku.getWidth() + miku.getSidePadding();
        if (miku.getX() < -miku.getSidePadding()) {
            miku.setX(-miku.getSidePadding()); // Ép về lại mép trái
        } else if (miku.getX() > screenLimit) {
            miku.setX(screenLimit); // Ép về lại mép phải
        }
    }

    @Override
    public void updatePhysics(MikuCharacter miku, int floorY) {
        miku.setY(floorY); // Ghim chân sát mặt đất
        timer--;
        if (timer <= 0) {
            // Hết giờ đứng yên -> đi bộ ngẫu nhiên trái hoặc phải
            boolean goLeft = Math.random() < 0.5;
            miku.changeState(new WalkState(goLeft));
        }
    }

    @Override
    public void updateAnimation(MikuCharacter miku) {
        frameIndex = (frameIndex + 1) % ResourceManager.TOTAL_IDLE_FRAMES;
    }

    @Override
    public BufferedImage getCurrentImage() {
        return ResourceManager.getIdleImage(frameIndex);
    }
}