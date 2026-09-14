package com.shimeji.miku;

import java.awt.image.BufferedImage;

public class FallState implements MikuState {
    private int frameIndex = 0;
    private int velocityY = 0;
    private static final int GRAVITY = 2; // Gia tốc rơi
    private static final int MAX_FALL_SPEED = 30; // Tốc độ rơi tối đa

    @Override
    public void enter(MikuCharacter miku) {
        frameIndex = 0;
        velocityY = 0;
    }

    @Override
    public void updatePhysics(MikuCharacter miku, int floorY) {
        velocityY += GRAVITY; // Rơi nhanh dần đều
        if (velocityY > MAX_FALL_SPEED)
            velocityY = MAX_FALL_SPEED; // Đạt tốc độ rơi tối đa

        int newY = miku.getY() + velocityY; // Tính nhẩm vị trí tiếp theo
        // Kiểm tra vị trí tiếp theo có phải mặt đất không
        if (newY >= floorY) {
            miku.setY(floorY); // Perfect landing
            miku.changeState(new IdleState()); // Chờ-ing
        } else {
            miku.setY(newY); // Tiếp tục rơi nếu chưa đến mặt đất
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