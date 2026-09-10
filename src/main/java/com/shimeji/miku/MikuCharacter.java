package com.shimeji.miku;

import java.awt.Toolkit;
import java.awt.image.BufferedImage;

public class MikuCharacter {

    private static final int SIDE_PADDING = 60;

    private int x, y;

    // TỐI ƯU HÓA HIỆU NĂNG & TỶ LỆ KHUNG HÌNH (Lazy Initialization)
    private int height = 150;
    // Gán width = -1 để đánh dấu là "chưa được tính toán"
    private int width = -1;

    private CharacterState state;

    private int walkFrameIndex = 0;
    private int dragFrameIndex = 0;
    private int fallFrameIndex = 0;
    private int idleFrameIndex = 0;

    private int speed = 4;
    private int velocityY = 0;
    private static final int GRAVITY = 2;
    private static final int MAX_FALL_SPEED = 30;

    private int stateTimer = 0;
    private boolean isPaused = false;

    private final int screenWidth;

    public MikuCharacter(int startX, int startY) {
        this.x = startX;
        this.y = startY;
        this.state = CharacterState.FALLING;
        this.screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
    }

    public BufferedImage getCurrentImage() {
        if (isPaused)
            return ResourceManager.getPausedImage();

        switch (state) {
            case DRAGGING:
                return ResourceManager.getDragImages()[dragFrameIndex];
            case FALLING:
                return ResourceManager.getFallImages()[fallFrameIndex];
            case IDLE:
                return ResourceManager.getIdleImages()[idleFrameIndex];
            case WALKING_LEFT:
            case WALKING_RIGHT:
                return ResourceManager.getWalkImages()[walkFrameIndex];
            default:
                return ResourceManager.getPausedImage();
        }
    }

    // =================================================================
    // SỬA LẠI (Chuẩn OOP): Tính Width tự động dựa trên tỷ lệ thật của ảnh gốc
    // =================================================================
    public int getWidth() {
        // Nếu width chưa từng được tính toán (đang là -1)
        if (this.width == -1) {
            BufferedImage currentImg = getCurrentImage();

            // Nếu ảnh đã tải xong, lấy tỷ lệ thật để tính Width
            if (currentImg != null) {
                double realRatio = (double) currentImg.getWidth() / currentImg.getHeight();
                this.width = (int) (this.height * realRatio);
            } else {
                // Phao cứu sinh (Fallback): Nếu ảnh chưa kịp tải, tạm trả về height
                return this.height;
            }
        }

        // Từ frame thứ 2 trở đi, nó chỉ trả về con số đã lưu trong Cache.
        // CPU không phải làm toán nữa!
        return this.width;
    }

    public void changeState(CharacterState newState) {
        if (this.state == newState)
            return;
        this.state = newState;

        walkFrameIndex = 0;
        dragFrameIndex = 0;
        fallFrameIndex = 0;

        if (newState == CharacterState.IDLE) {
            int totalIdleFrames = ResourceManager.getIdleImages().length;
            if (totalIdleFrames > 0) {
                idleFrameIndex = (int) (Math.random() * totalIdleFrames);
            } else {
                idleFrameIndex = 0;
            }
        } else {
            idleFrameIndex = 0;
            if (newState == CharacterState.FALLING) {
                velocityY = 0;
            }
        }
    }

    public void updatePhysics(int floorY) {
        if (state == CharacterState.DRAGGING || isPaused)
            return;

        if (state == CharacterState.IDLE || state == CharacterState.WALKING_LEFT
                || state == CharacterState.WALKING_RIGHT) {
            this.y = floorY;
        }

        if (state == CharacterState.FALLING) {
            velocityY += GRAVITY;
            if (velocityY > MAX_FALL_SPEED) {
                velocityY = MAX_FALL_SPEED;
            }
            y += velocityY;

            if (y >= floorY) {
                y = floorY;
                changeState(CharacterState.IDLE);
                resetStateTimer();
            }
        } else if (state == CharacterState.IDLE) {
            stateTimer--;
            if (stateTimer <= 0) {
                changeState((Math.random() < 0.5) ? CharacterState.WALKING_LEFT : CharacterState.WALKING_RIGHT);
                stateTimer = 40 + (int) (Math.random() * 61);
            }
        } else if (state == CharacterState.WALKING_RIGHT) {
            x += speed;
            stateTimer--;

            // Gọi hàm getWidth() (đã được cache) để tính va chạm biên
            if (x > screenWidth - getWidth() + SIDE_PADDING || stateTimer <= 0) {
                if (x > screenWidth - getWidth() + SIDE_PADDING) {
                    x = screenWidth - getWidth() + SIDE_PADDING;
                }
                changeState(CharacterState.IDLE);
                resetStateTimer();
            }
        } else if (state == CharacterState.WALKING_LEFT) {
            x -= speed;
            stateTimer--;

            if (x < -SIDE_PADDING || stateTimer <= 0) {
                if (x < -SIDE_PADDING) {
                    x = -SIDE_PADDING;
                }
                changeState(CharacterState.IDLE);
                resetStateTimer();
            }
        }
    }

    private void resetStateTimer() {
        this.stateTimer = 30 + (int) (Math.random() * 41);
    }

    public void updateAnimation() {
        if (isPaused)
            return;

        if (state == CharacterState.DRAGGING) {
            int total = ResourceManager.getDragImages().length;
            if (total > 0)
                dragFrameIndex = (dragFrameIndex + 1) % total;
        } else if (state == CharacterState.FALLING) {
            int total = ResourceManager.getFallImages().length;
            if (total > 0)
                fallFrameIndex = (fallFrameIndex + 1) % total;
        } else if (state == CharacterState.WALKING_LEFT || state == CharacterState.WALKING_RIGHT) {
            int total = ResourceManager.getWalkImages().length;
            if (total > 0)
                walkFrameIndex = (walkFrameIndex + 1) % total;
        } else if (state == CharacterState.IDLE) {
            int total = ResourceManager.getIdleImages().length;
            if (total > 0)
                idleFrameIndex = (idleFrameIndex + 1) % total;
        }
    }

    // Getters & Setters
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getHeight() {
        return height;
    }

    public CharacterState getState() {
        return state;
    }

    public void setState(CharacterState state) {
        changeState(state);
    }

    public int getWalkFrameIndex() {
        return walkFrameIndex;
    }

    public int getDragFrameIndex() {
        return dragFrameIndex;
    }

    public int getFallFrameIndex() {
        return fallFrameIndex;
    }

    public int getIdleFrameIndex() {
        return idleFrameIndex;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void setPaused(boolean paused) {
        this.isPaused = paused;
        if (paused)
            changeState(CharacterState.IDLE);
    }
}