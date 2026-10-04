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

        // 👉 BẮT ĐẦU ĐOẠN TÙY CHỈNH TỐC ĐỘ
        int currentGravity = GRAVITY;
        int currentMaxSpeed = MAX_FALL_SPEED;

        // Nếu là Miku khổng lồ, ta sẽ tăng trọng lực lên cho ẻm rớt nặng hơn!
        if (miku.getScale() > 1.0) {
            currentGravity = 5; // Gia tốc rơi (Mặc định là 2. Bạn tăng lên 6, 8, 10 thì rớt càng lẹ)
            currentMaxSpeed = 100; // Tốc độ rớt tối đa (Mặc định là 30. Tăng lên 120, 150 để ẻm phóng như tên lửa)
        }

        velocityY += currentGravity;
        if (velocityY > currentMaxSpeed)
            velocityY = currentMaxSpeed;
        // 👉 KẾT THÚC ĐOẠN TÙY CHỈNH

        int newY = miku.getY() + velocityY;

        // 1. Kiểm tra xem đã chạm mặt đất chưa
        if (newY >= floorY) {
            miku.setY(floorY);
            miku.changeState(new IdleState());
            return;
        } else {
            miku.setY(newY);
        }

        // 2. Phản xạ bám tường!
        if (miku.isForcedPomodoroWalk()) {
            return;
        }

        int screenLimit = miku.getScreenWidth() - miku.getWidth() + miku.getSidePadding();
        if (miku.getX() <= -miku.getSidePadding()) {
            miku.setX(-miku.getSidePadding());
            // Gọi ClimbState với tham số isResting = true (để Miku dùng ảnh pause bám dính
            // vào tường)
            miku.changeState(new ClimbState(true, false, true));
        } else if (miku.getX() >= screenLimit) {
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