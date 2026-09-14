package com.shimeji.miku;

import java.awt.image.BufferedImage;

public class DragState implements MikuState {
    private int frameIndex = 0;

    @Override
    public void enter(MikuCharacter miku) {
        frameIndex = 0;
        // Khi nhấc Miku lên thì cọng hành sẽ biến mất
        if (miku.getEquippedItem().isActive()) {
            miku.getEquippedItem().setInactive();
        }
    }

    @Override
    public void updatePhysics(MikuCharacter miku, int floorY) {
        // Chuột nắm quyền di chuyển tọa độ (Ở bên MikuWindow) nên bỏ qua vật lý
    }

    @Override
    public void updateAnimation(MikuCharacter miku) {
        frameIndex = (frameIndex + 1) % ResourceManager.TOTAL_DRAG_FRAMES;
    }

    @Override
    public BufferedImage getCurrentImage() {
        return ResourceManager.getDragImage(frameIndex);
    }
}