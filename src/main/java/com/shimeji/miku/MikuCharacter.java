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

    // TÍNH NĂNG MỚI: HÀNG CHỜ LỆNH (Command Queue)
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

        // CHUẨN OOP: Xử lý hàng chờ lệnh
        // Nếu Miku ĐÃ ĐÁP ĐẤT AN TOÀN (không phải đang rơi hay bị kéo) và có lệnh đang
        // chờ
        // thì cô ấy sẽ tự động thực thi lệnh đó ngay lập tức!
        if (pendingState != null && !(currentState instanceof FallState) && !(currentState instanceof DragState)) {
            changeState(pendingState);
            pendingState = null; // Xóa lệnh khỏi hàng chờ sau khi thực thi
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

            // TÍNH NĂNG MỚI: Đưa vào hàng chờ
            // Nếu Miku đang trên không mà bị ra lệnh (không phải lệnh rơi/kéo)
            // thì đưa lệnh đó vào hàng chờ để tránh bị dịch chuyển tức thời (teleport)
            if (isAirborne && enumState != CharacterState.DRAGGING && enumState != CharacterState.FALLING) {
                pendingState = mappedState;
            } else {
                changeState(mappedState);
            }
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

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getHeight() {
        return height;
    }

    public int getSpeed() {
        return speed;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getSidePadding() {
        return SIDE_PADDING;
    }

    public boolean isFacingRight() {
        return facingRight;
    }

    public void setFacingRight(boolean facingRight) {
        this.facingRight = facingRight;
    }

    public ThrowableItem getEquippedItem() {
        return equippedItem;
    }

    public void setPaused(boolean paused) {
        this.isPaused = paused;

        if (paused && currentState instanceof ThrowState) {
            equippedItem.setInactive();
            changeState(new IdleState());
        }
    }
}