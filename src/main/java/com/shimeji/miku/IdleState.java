package com.shimeji.miku;

import java.awt.image.BufferedImage;

public class IdleState implements MikuState {
    private int frameIndex = 0;
    private int timer = 0;

    @Override
    public void enter(MikuCharacter miku) {
        frameIndex = (int) (Math.random() * ResourceManager.TOTAL_IDLE_FRAMES);
        timer = 30 + (int) (Math.random() * 273); 

        int screenLimit = miku.getScreenWidth() - miku.getWidth() + miku.getSidePadding();
        if (miku.getX() < -miku.getSidePadding()) {
            miku.setX(-miku.getSidePadding()); 
        } else if (miku.getX() > screenLimit) {
            miku.setX(screenLimit); 
        }
    }

    @Override
    public void updatePhysics(MikuCharacter miku, int floorY) {
        miku.setY(floorY); 
        
        // 👉 ĐÃ THAY ĐỔI: Chống lười biếng! Nếu đang Gaming Mode mà bị rớt xuống mặt đất, 
        // lập tức phải đi bộ ra tường để tránh cản tầm nhìn chơi game của User!
        if (miku.getAppMode() == MikuCharacter.AppMode.GAMING) {
            boolean isLeftWall = (miku.getX() < miku.getScreenWidth() / 2);
            miku.changeState(new WalkState(isLeftWall));
            return;
        }

        timer--;
        if (timer <= 0) {
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