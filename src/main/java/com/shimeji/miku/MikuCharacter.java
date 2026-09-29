package com.shimeji.miku;

import java.awt.Toolkit;
import java.awt.image.BufferedImage;

public class MikuCharacter {
    public static final int SIDE_PADDING = 60;

    public enum AppMode {
        GAMING, CASUAL, WORKING
    }
    private AppMode appMode = AppMode.CASUAL; 

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

    public AppMode getAppMode() {
        return appMode;
    }

    public void setAppMode(AppMode mode) {
        this.appMode = mode;
        
        if (mode == AppMode.GAMING) {
            this.pendingState = null; 
            
            boolean isAirborne = (currentState instanceof FallState || currentState instanceof DragState || currentState instanceof ClimbState);
            boolean isLeftWall = (this.x < screenWidth / 2);
            
            if (isAirborne) {
                changeState(new ClimbState(isLeftWall, false, true));
            } else {
                changeState(new WalkState(isLeftWall));
            }
        } else {
            // Xử lý khi tắt Gaming Mode (Chuyển về Casual/Working)
            // Nếu Miku đang cặm cụi đi bộ ra tường do lệnh của Gaming Mode cũ,
            // lập tức cho ẻm đứng lại nghỉ ngơi (Idle)
            if (currentState instanceof WalkState) {
                changeState(new IdleState());
            }
        }
    }

    public void updatePhysics(int floorY) {
        equippedItem.updatePhysics(floorY);
        if (isPaused)
            return;

        currentState.updatePhysics(this, floorY);

        if (pendingState != null && !(currentState instanceof FallState)
                && !(currentState instanceof DragState) && !(currentState instanceof ClimbState)) {
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
        if (isPaused) {
            if (currentState instanceof ClimbState) {
                return ResourceManager.getClimbPauseImage();
            }

            if (currentState instanceof FallState || currentState instanceof DragState) {
                return currentState.getCurrentImage();
            }

            return ResourceManager.getPausedImage();
        }

        return currentState.getCurrentImage();
    }

    public int getWidth() {
        BufferedImage currentImg = getCurrentImage();
        if (currentImg != null) {
            this.width = currentImg.getWidth();
        }
        return this.width;
    }

    public int getHeight() {
        BufferedImage currentImg = getCurrentImage();
        if (currentImg != null) {
            this.height = currentImg.getHeight();
        }
        return this.height;
    }

    public void changeState(MikuState newState) {
        // Dọn dẹp item nếu bị ngắt ngang hành động Xóa file
        if (this.currentState instanceof DeleteState && !(newState instanceof DeleteState)) {
            if (!(this.equippedItem instanceof LeekItem)) {
                this.setEquippedItem(new LeekItem());
            }
        }

        // Dọn dẹp cọng hành nếu bị ngắt ngang lúc đang giơ tay ném (vd: chuyển Mode)
        if (this.currentState instanceof ThrowState && !(newState instanceof IdleState)) {
            this.equippedItem.setInactive(); 
            this.setEquippedItem(new LeekItem()); 
        }

        this.currentState = newState;
        this.currentState.enter(this);
    }

    public void setState(CharacterState enumState) {
        if (appMode == AppMode.GAMING && enumState != CharacterState.DRAGGING && enumState != CharacterState.FALLING) {
            return;
        }

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
            boolean isAirborne = (currentState instanceof FallState
                    || currentState instanceof DragState || currentState instanceof ClimbState);

            if (isAirborne && enumState != CharacterState.DRAGGING && enumState != CharacterState.FALLING) {
                pendingState = mappedState;

                if (currentState instanceof ClimbState) {
                    if (this.x < screenWidth / 2) {
                        this.x = -this.getSidePadding() + 5;
                    } else {
                        this.x = screenWidth - this.getWidth() + this.getSidePadding() - 5;
                    }
                    changeState(new FallState());
                }
            } else {
                pendingState = null;
                changeState(mappedState);
            }
        }
    }

    public void triggerDeleteAction(String fileName, BufferedImage fileIcon) {
        if (appMode == AppMode.GAMING) return;
        
        if (currentState instanceof ThrowState || currentState instanceof DeleteState)
            return;

        DeleteState deleteState = new DeleteState(fileIcon);

        boolean isAirborne = (currentState instanceof FallState
                || currentState instanceof DragState || currentState instanceof ClimbState);

        if (isAirborne) {
            pendingState = deleteState;
        } else {
            changeState(deleteState);
        }
    }

    public CharacterState getState() {
        if (currentState instanceof DragState) return CharacterState.DRAGGING;
        if (currentState instanceof FallState) return CharacterState.FALLING;
        if (currentState instanceof ThrowState) return CharacterState.THROWING;
        if (currentState instanceof WalkState) return CharacterState.WALKING_LEFT;
        return CharacterState.IDLE;
    }

    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    public void setPosition(int x, int y) { this.x = x; this.y = y; }
    public int getSpeed() { return speed; }
    public int getScreenWidth() { return screenWidth; }
    public int getSidePadding() { return SIDE_PADDING; }
    public boolean isFacingRight() { return facingRight; }
    public void setFacingRight(boolean facingRight) { this.facingRight = facingRight; }
    public ThrowableItem getEquippedItem() { return equippedItem; }
    public void setEquippedItem(ThrowableItem item) { this.equippedItem = item; }
    
    public void setPaused(boolean paused) {
        this.isPaused = paused;
        if (paused && (currentState instanceof ThrowState || currentState instanceof DeleteState)) {
            equippedItem.setInactive();
            setEquippedItem(new LeekItem());
            changeState(new IdleState());
        }
    }
}