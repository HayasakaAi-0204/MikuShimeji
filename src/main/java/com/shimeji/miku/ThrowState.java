package com.shimeji.miku;

import java.awt.image.BufferedImage;

public class ThrowState implements MikuState {
    private int frameIndex = 0;

    @Override
    public void enter(MikuCharacter miku) {
        frameIndex = 0;
        miku.setFacingRight(Math.random() < 0.5);
        miku.getEquippedItem().hold(miku);
    }

    @Override
    public void updatePhysics(MikuCharacter miku, int floorY) {
        miku.setY(floorY);
    }

    @Override
    public void updateAnimation(MikuCharacter miku) {
        frameIndex++;

        if (frameIndex < 101) {
            miku.getEquippedItem().hold(miku);
        } else if (frameIndex == 101) {
            miku.getEquippedItem().toss(miku);
        }

        if (frameIndex >= ResourceManager.TOTAL_THROW_FRAMES - 1) {
            miku.changeState(new IdleState());
        }
    }

    @Override
    public BufferedImage getCurrentImage() {
        return ResourceManager.getThrowImage(frameIndex);
    }
}