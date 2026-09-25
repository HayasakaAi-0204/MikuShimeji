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
        velocityY += GRAVITY; 
        if (velocityY > MAX_FALL_SPEED)
            velocityY = MAX_FALL_SPEED; 

        int newY = miku.getY() + velocityY; 
        
        // 1. Kiểm tra xem đã chạm mặt đất chưa
        if (newY >= floorY) {
            miku.setY(floorY); 
            miku.changeState(new IdleState()); 
            return;
        } else {
            miku.setY(newY); 
        }

        // 2. CẬP NHẬT: Phản xạ bám tường!
        int screenLimit = miku.getScreenWidth() - miku.getWidth() + miku.getSidePadding();
        
        if (miku.getX() <= -miku.getSidePadding()) {
            miku.setX(-miku.getSidePadding());
            // Gọi ClimbState với tham số isResting = true (để Miku dùng ảnh pause bám dính vào tường)
            miku.changeState(new ClimbState(true, false, true)); 
        } 
        else if (miku.getX() >= screenLimit) {
            miku.setX(screenLimit);
            // Bám tường phải
            miku.changeState(new ClimbState(false, false, true));
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