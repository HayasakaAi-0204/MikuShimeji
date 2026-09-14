package com.shimeji.miku;

import java.awt.image.BufferedImage;

public class DeleteState implements MikuState {
    private int frameIndex = 0;
    private BufferedImage fileIcon;
    private ThrowableItem previousWeapon;

    public DeleteState(BufferedImage fileIcon) {
        this.fileIcon = fileIcon;
    }

    @Override
    public void enter(MikuCharacter miku) {
        frameIndex = 0;

        // ĐÃ SỬA: Quay mặt ngẫu nhiên Trái/Phải (Tỉ lệ 50-50) giống hệt ném hành bình thường
        miku.setFacingRight(Math.random() < 0.5);

        // Lưu lại cọng hành cũ, và nâng cấp lên Cọng hành buộc File rác
        previousWeapon = miku.getEquippedItem();
        miku.setEquippedItem(new FileLeekItem(fileIcon));
    }

    @Override
    public void updatePhysics(MikuCharacter miku, int floorY) {
        miku.setY(floorY);
    }

    @Override
    public void updateAnimation(MikuCharacter miku) {
        frameIndex++;

        // Giai đoạn 1: Lấy đà và buộc file (Frame 0001 đến 0124)
        if (frameIndex < 124) {
            miku.getEquippedItem().hold(miku);
        }
        // Giai đoạn 2: Vung tay ném (Frame 0125 trở đi)
        else if (frameIndex == 124) {
            miku.getEquippedItem().toss(miku);
        }

        // Kết thúc animation: Khi chạm mốc 236 frames
        if (frameIndex >= ResourceManager.TOTAL_DELETE_FRAMES - 1) {
            // Trả lại cọng hành bình thường cho Miku
            miku.setEquippedItem(previousWeapon);
            miku.changeState(new IdleState());
        }
    }

    @Override
    public BufferedImage getCurrentImage() {
        return ResourceManager.getDeleteImage(frameIndex);
    }
}