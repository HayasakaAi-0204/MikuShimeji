package com.shimeji.miku;

import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;

public class MikuCharacter {

    private static final int SIDE_PADDING = 60;
    
    // SỬA ĐỔI 1: Tinh chỉnh lại hằng số nhận diện Taskbar
    private static final int TASKBAR_HEIGHT = 48; // Chiều cao thực tế của Taskbar (px)
    private static final int TASKBAR_BUFFER = 5;  // Vùng bù trừ (tolerance) để tránh lỗi tay run khi di chuột

    private int x, y;
    private int height = 150;

    private CharacterState state;
    private int walkFrameIndex = 0;
    private int dragFrameIndex = 0;
    private int fallFrameIndex = 0;
    private int idleFrameIndex = 0;

    private int speed = 4;
    private int liftSpeed = 12; 
    
    private int velocityY = 0; 
    private static final int GRAVITY = 2; 
    private static final int MAX_FALL_SPEED = 30; 

    private int stateTimer = 0;
    private boolean isPaused = false;

    public MikuCharacter(int startX, int startY) {
        this.x = startX;
        this.y = startY;
        this.state = CharacterState.FALLING;
    }

    public int getWidth() {
        BufferedImage currentImg;

        if (isPaused) {
            currentImg = ResourceManager.getPausedImage();
        } else if (state == CharacterState.DRAGGING) {
            currentImg = ResourceManager.getDragImages()[dragFrameIndex];
        } else if (state == CharacterState.FALLING) {
            currentImg = ResourceManager.getFallImages()[fallFrameIndex];
        } else if (state == CharacterState.IDLE) {
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

    public void changeState(CharacterState newState) {
        if (this.state == newState)
            return;

        int oldWidth = getWidth();
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

        int newWidth = getWidth();
        this.x += (oldWidth - newWidth) / 2;
    }

    // THÊM MỚI 2 (Chuẩn OOP): Tách biệt logic phân tích hành vi người dùng thành một hàm Delegate
    // Giúp code tự giải thích (Self-documenting code) và dễ bảo trì
    private boolean isTaskbarHovered(Point mousePos, int screenHeight) {
        // Vùng an toàn giờ đây bao trọn toàn bộ chiều cao 48px của Taskbar + 5px bù trừ
        int safeZone = screenHeight - TASKBAR_HEIGHT - TASKBAR_BUFFER;
        return mousePos.y >= safeZone;
    }

    private int getDynamicFloorY() {
        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration();
        Rectangle screenBounds = gc.getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);

        int logicalFloor = screenBounds.height - insets.bottom;

        try {
            Point mousePos = MouseInfo.getPointerInfo().getLocation();
            
            // SỬA ĐỔI 3: Gọi hàm kiểm tra vùng an toàn
            // Đọc vào hiểu ngay: "Nếu Taskbar bị ẩn VÀ chuột đang lướt trên Taskbar -> Nâng mặt đất lên"
            if (insets.bottom == 0 && isTaskbarHovered(mousePos, screenBounds.height)) {
                logicalFloor = screenBounds.height - TASKBAR_HEIGHT;
            }
        } catch (Exception e) {
            // Ignored
        }

        return logicalFloor - this.height;
    }

    public void updatePhysics() {
        if (state == CharacterState.DRAGGING || isPaused)
            return;

        int screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
        int floorY = getDynamicFloorY();

        if (y > floorY) {
            y -= liftSpeed;
            if (y < floorY) {
                y = floorY;
            }
        } else if (y < floorY && state != CharacterState.FALLING) {
            changeState(CharacterState.FALLING); 
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
        if (isPaused)
            return;

        if (state == CharacterState.DRAGGING) {
            int totalDragFrames = ResourceManager.getDragImages().length;
            if (totalDragFrames > 0)
                dragFrameIndex = (dragFrameIndex + 1) % totalDragFrames;
        } else if (state == CharacterState.FALLING) {
            int totalFallFrames = ResourceManager.getFallImages().length;
            if (totalFallFrames > 0)
                fallFrameIndex = (fallFrameIndex + 1) % totalFallFrames;
        } else if (state == CharacterState.WALKING_LEFT || state == CharacterState.WALKING_RIGHT) {
            int totalFrames = ResourceManager.getWalkImages().length;
            if (totalFrames > 0)
                walkFrameIndex = (walkFrameIndex + 1) % totalFrames;
        } else if (state == CharacterState.IDLE) {
            int totalIdleFrames = ResourceManager.getIdleImages().length;
            if (totalIdleFrames > 0)
                idleFrameIndex = (idleFrameIndex + 1) % totalIdleFrames;
        }
    }

    // Getters & Setters
    public int getX() { return x; }
    public int getY() { return y; }
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
    public int getHeight() { return height; }
    public CharacterState getState() { return state; }
    public void setState(CharacterState state) { changeState(state); }
    public int getWalkFrameIndex() { return walkFrameIndex; }
    public int getDragFrameIndex() { return dragFrameIndex; }
    public int getFallFrameIndex() { return fallFrameIndex; }
    public int getIdleFrameIndex() { return idleFrameIndex; }
    public boolean isPaused() { return isPaused; }

    public void setPaused(boolean paused) {
        this.isPaused = paused;
        if (paused)
            changeState(CharacterState.IDLE);
    }
}