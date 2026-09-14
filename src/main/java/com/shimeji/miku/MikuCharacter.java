package com.shimeji.miku;

import java.awt.Toolkit;
import java.awt.image.BufferedImage;

public class MikuCharacter {
    public static final int SIDE_PADDING = 60;

    private int x, y;
    private int height = 150;
    private int width = 150;
    private int speed = 4;
    private final int screenWidth;

    private boolean facingRight = true;
    private boolean isPaused = false;

    private MikuState currentState;
    private ThrowableItem equippedItem = new LeekItem();

    private MikuState pendingState = null;

    public MikuCharacter(int startX, int startY) {
        this.x = startX;
        this.y = startY;
        this.screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;

        this.currentState = new FallState();
        this.currentState.enter(this);
    }

    public void updatePhysics(int floorY) {
        equippedItem.updatePhysics(floorY);
        if (isPaused)
            return;

        currentState.updatePhysics(this, floorY);

        if (pendingState != null && !(currentState instanceof FallState) && !(currentState instanceof DragState)) {
            changeState(pendingState);
            pendingState = null;
        }
    }

    public void updateAnimation() {
        if (isPaused)
            return;
        currentState.updateAnimation(this);
    }

    public BufferedImage getCurrentImage() {
        if (isPaused)
            return ResourceManager.getPausedImage();
        return currentState.getCurrentImage();
    }

    public int getWidth() {
        BufferedImage currentImg = getCurrentImage();
        if (currentImg != null) {
            double realRatio = (double) currentImg.getWidth() / currentImg.getHeight();
            this.width = (int) (this.height * realRatio);
        }
        return this.width;
    }

    public void changeState(MikuState newState) {
        // SỬA LỖI: Dọn dẹp vũ khí rác nếu Miku bị ngắt ngang hành động Xóa file (Ví dụ: Bị chuột kéo đi)
        if (this.currentState instanceof DeleteState && !(newState instanceof DeleteState)) {
            if (!(this.equippedItem instanceof LeekItem)) {
                this.setEquippedItem(new LeekItem()); // Trả lại cọng hành mặc định
            }
        }

        this.currentState = newState;
        this.currentState.enter(this);
    }

    public void setState(CharacterState enumState) {
        MikuState mappedState = null;
        switch (enumState) {
            case DRAGGING:
                mappedState = new DragState();
                break;
            case FALLING:
                mappedState = new FallState();
                break;
            case IDLE:
                mappedState = new IdleState();
                break;
            case THROWING:
                mappedState = new ThrowState();
                break;
            case WALKING_LEFT:
                mappedState = new WalkState(true);
                break;
            case WALKING_RIGHT:
                mappedState = new WalkState(false);
                break;
        }

        if (mappedState != null) {
            boolean isAirborne = (currentState instanceof FallState || currentState instanceof DragState);

            if (isAirborne && enumState != CharacterState.DRAGGING && enumState != CharacterState.FALLING) {
                pendingState = mappedState;
            } else {
                changeState(mappedState);
            }
        }
    }

    public void triggerDeleteAction(String fileName, BufferedImage fileIcon) {
        if (currentState instanceof ThrowState || currentState instanceof DeleteState)
            return;

        DeleteState deleteState = new DeleteState(fileIcon);

        boolean isAirborne = (currentState instanceof FallState || currentState instanceof DragState);
        if (isAirborne) {
            pendingState = deleteState;
        } else {
            changeState(deleteState);
        }
    }

    public CharacterState getState() {
        if (currentState instanceof DragState)
            return CharacterState.DRAGGING;
        if (currentState instanceof FallState)
            return CharacterState.FALLING;
        if (currentState instanceof ThrowState)
            return CharacterState.THROWING;
        if (currentState instanceof WalkState)
            return CharacterState.WALKING_LEFT;
        return CharacterState.IDLE;
    }

    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    public void setPosition(int x, int y) { this.x = x; this.y = y; }
    public int getHeight() { return height; }
    public int getSpeed() { return speed; }
    public int getScreenWidth() { return screenWidth; }
    public int getSidePadding() { return SIDE_PADDING; }
    public boolean isFacingRight() { return facingRight; }
    public void setFacingRight(boolean facingRight) { this.facingRight = facingRight; }
    public ThrowableItem getEquippedItem() { return equippedItem; }
    public void setEquippedItem(ThrowableItem item) { this.equippedItem = item; }

    public void setPaused(boolean paused) {
        this.isPaused = paused;

        // SỬA LỖI: Hủy ngay hành động Xóa file nếu người dùng bấm chuột phải mở Menu
        if (paused && (currentState instanceof ThrowState || currentState instanceof DeleteState)) {
            equippedItem.setInactive();
            setEquippedItem(new LeekItem()); // Khôi phục cọng hành mặc định ngay lập tức
            changeState(new IdleState());
        }
    }
}