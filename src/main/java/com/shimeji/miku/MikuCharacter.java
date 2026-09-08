package com.shimeji.miku;

import java.awt.Toolkit;
import java.awt.image.BufferedImage;

public class MikuCharacter {

    // 1. CHUẨN OOP: Hằng số trừ hao viền trong suốt của video
    private static final int SIDE_PADDING = 60;

    private int x, y;
    private int height = 150;

    private CharacterState state;
    private int walkFrameIndex = 0;
    private int dragFrameIndex = 0;
    private int fallFrameIndex = 0;

    // THÊM MỚI 1: Thuộc tính đếm nhịp cho hoạt ảnh chờ 240 frame (Idle)
    private int idleFrameIndex = 0;

    private int speed = 4;
    private int fallSpeed = 30;
    private int stateTimer = 0;
    private boolean isPaused = false;

    public MikuCharacter(int startX, int startY) {
        this.x = startX;
        this.y = startY;
        this.state = CharacterState.FALLING;
    }

    public int getWidth() {
        BufferedImage currentImg;

        // THÊM MỚI 2: Ưu tiên xử lý kích thước riêng khi Miku bị "Pause" (Mở Menu)
        if (isPaused) {
            currentImg = ResourceManager.getPausedImage();
        } else if (state == CharacterState.DRAGGING) {
            currentImg = ResourceManager.getDragImages()[dragFrameIndex];
        } else if (state == CharacterState.FALLING) {
            currentImg = ResourceManager.getFallImages()[fallFrameIndex];
        } else if (state == CharacterState.IDLE) {
            // SỬA ĐỔI: Nếu không Pause, lấy kích thước từ mảng 240 ảnh động Idle
            currentImg = ResourceManager.getIdleImages()[idleFrameIndex];
        } else {
            currentImg = ResourceManager.getWalkImages()[walkFrameIndex];
        }

        if (currentImg != null) {
            double ratio = (double) currentImg.getWidth() / currentImg.getHeight();
            return (int) (height * ratio);
        }
        return height;
    }

    // 2. CHUẨN OOP: Tính Đóng gói (Encapsulation) kiểm soát trạng thái
    public void changeState(CharacterState newState) {
        if (this.state == newState)
            return;

        int oldWidth = getWidth();
        this.state = newState;

        // Reset các nhịp đếm cơ bản về 0 khi đổi hành động
        walkFrameIndex = 0;
        dragFrameIndex = 0;
        fallFrameIndex = 0;

        // CHUẨN OOP: Khởi tạo ngẫu nhiên (Dynamic Initialization) cho IDLE
        if (newState == CharacterState.IDLE) {
            int totalIdleFrames = ResourceManager.getIdleImages().length;
            if (totalIdleFrames > 0) {
                // Hàm Math.random() trả về từ 0.0 đến 0.999...
                // Nhân với tổng số frame và ép kiểu (int) sẽ ra 1 vị trí ngẫu nhiên từ 0 đến
                // 239
                idleFrameIndex = (int) (Math.random() * totalIdleFrames);
            } else {
                idleFrameIndex = 0;
            }
        } else {
            // Nếu không phải IDLE (ví dụ đang mở menu), vẫn ép về 0
            idleFrameIndex = 0;
        }

        int newWidth = getWidth();

        // Tự động bù trừ chênh lệch chiều rộng để giữ tâm nhân vật đứng yên
        this.x += (oldWidth - newWidth) / 2;
    }

    // Hành vi Vật lý (Update Logic)
    public void updatePhysics() {
        if (state == CharacterState.DRAGGING || isPaused)
            return;

        int screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
        int screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;
        int floorY = screenHeight - 40 - height;

        if (state == CharacterState.FALLING) {
            y += fallSpeed;
            if (y >= floorY) {
                y = floorY;
                changeState(CharacterState.IDLE);
                stateTimer = 30 + (int) (Math.random() * 41);
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

            if (x > screenWidth - getWidth() + SIDE_PADDING) {
                x = screenWidth - getWidth() + SIDE_PADDING;
                changeState(CharacterState.IDLE);
                stateTimer = 30 + (int) (Math.random() * 41);
            } else if (stateTimer <= 0) {
                changeState(CharacterState.IDLE);
                stateTimer = 30 + (int) (Math.random() * 41);
            }
        } else if (state == CharacterState.WALKING_LEFT) {
            x -= speed;
            stateTimer--;

            if (x < -SIDE_PADDING) {
                x = -SIDE_PADDING;
                changeState(CharacterState.IDLE);
                stateTimer = 30 + (int) (Math.random() * 41);
            } else if (stateTimer <= 0) {
                changeState(CharacterState.IDLE);
                stateTimer = 30 + (int) (Math.random() * 41);
            }
        }
    }

    public void updateAnimation() {
        // THÊM MỚI 3: Nếu đang Paused (người dùng mở Menu) -> Đóng băng mọi hoạt ảnh
        if (isPaused)
            return;

        if (state == CharacterState.DRAGGING) {
            int totalDragFrames = ResourceManager.getDragImages().length;
            if (totalDragFrames > 0) {
                dragFrameIndex = (dragFrameIndex + 1) % totalDragFrames;
            }
        } else if (state == CharacterState.FALLING) {
            int totalFallFrames = ResourceManager.getFallImages().length;
            if (totalFallFrames > 0) {
                fallFrameIndex = (fallFrameIndex + 1) % totalFallFrames;
            }
        } else if (state == CharacterState.WALKING_LEFT || state == CharacterState.WALKING_RIGHT) {
            int totalFrames = ResourceManager.getWalkImages().length;
            if (totalFrames > 0) {
                walkFrameIndex = (walkFrameIndex + 1) % totalFrames;
            }
        } else if (state == CharacterState.IDLE) {
            // THÊM MỚI 4: Đồng hồ đếm nhịp riêng cho 240 khung hình Idle
            int totalIdleFrames = ResourceManager.getIdleImages().length;
            if (totalIdleFrames > 0) {
                idleFrameIndex = (idleFrameIndex + 1) % totalIdleFrames;
            }
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

    // THÊM MỚI 5: Getter cho nhịp Idle và kiểm tra trạng thái Menu
    public int getIdleFrameIndex() {
        return idleFrameIndex;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void setPaused(boolean paused) {
        this.isPaused = paused;
        if (paused) {
            changeState(CharacterState.IDLE);
        }
    }
}