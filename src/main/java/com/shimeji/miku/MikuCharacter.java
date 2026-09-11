package com.shimeji.miku;

import java.awt.Toolkit;
import java.awt.image.BufferedImage;

public class MikuCharacter {

    private static final int SIDE_PADDING = 60;

    private int x, y;
    private int height = 150;
    private int width = 150;

    private CharacterState state;
    private int walkFrameIndex = 0;
    private int dragFrameIndex = 0;
    private int fallFrameIndex = 0;
    private int idleFrameIndex = 0;
    private int throwFrameIndex = 0;

    // =================================================================
    // NÂNG CẤP OOP (Polymorphism): Sử dụng Interface thay vì Class cụ thể.
    // Sau này Miku có thể ném bất cứ vũ khí gì implement ThrowableItem.
    // =================================================================
    private ThrowableItem equippedItem = new LeekItem();

    private boolean facingRight = true;

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
            case THROWING:
                return ResourceManager.getThrowImages()[throwFrameIndex];
            case WALKING_LEFT:
            case WALKING_RIGHT:
                return ResourceManager.getWalkImages()[walkFrameIndex];
            default:
                return ResourceManager.getPausedImage();
        }
    }

    public int getWidth() {
        BufferedImage currentImg = getCurrentImage();
        if (currentImg != null) {
            double realRatio = (double) currentImg.getWidth() / currentImg.getHeight();
            this.width = (int) (this.height * realRatio);
        }
        return this.width;
    }

    public void changeState(CharacterState newState) {
        if (this.state == newState)
            return;

        // FIX BUG EDGE CASE: Thu hồi vũ khí nếu Miku bị nhấc lên đột ngột
        if (this.state == CharacterState.THROWING && equippedItem.isActive()) {
            equippedItem.setInactive();
        }

        this.state = newState;
        walkFrameIndex = 0;
        dragFrameIndex = 0;
        fallFrameIndex = 0;
        throwFrameIndex = 0;

        if (newState == CharacterState.IDLE) {
            int totalIdleFrames = ResourceManager.getIdleImages().length;
            idleFrameIndex = totalIdleFrames > 0 ? (int) (Math.random() * totalIdleFrames) : 0;
        } else if (newState == CharacterState.FALLING) {
            idleFrameIndex = 0;
            velocityY = 0;
        } else if (newState == CharacterState.THROWING) {
            this.facingRight = Math.random() < 0.5;
            equippedItem.hold(this);
        }
    }

    public void updatePhysics(int floorY) {
        equippedItem.updatePhysics(floorY);

        if (state == CharacterState.DRAGGING || isPaused)
            return;

        if (state == CharacterState.IDLE || state == CharacterState.WALKING_LEFT
                || state == CharacterState.WALKING_RIGHT || state == CharacterState.THROWING) {
            this.y = floorY;
        }

        if (state == CharacterState.FALLING) {
            velocityY += GRAVITY;
            if (velocityY > MAX_FALL_SPEED)
                velocityY = MAX_FALL_SPEED;
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
            facingRight = true;

            if (x > screenWidth - getWidth() + SIDE_PADDING || stateTimer <= 0) {
                if (x > screenWidth - getWidth() + SIDE_PADDING)
                    x = screenWidth - getWidth() + SIDE_PADDING;
                changeState(CharacterState.IDLE);
                resetStateTimer();
            }
        } else if (state == CharacterState.WALKING_LEFT) {
            x -= speed;
            stateTimer--;
            facingRight = false;

            if (x < -SIDE_PADDING || stateTimer <= 0) {
                if (x < -SIDE_PADDING)
                    x = -SIDE_PADDING;
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
        } else if (state == CharacterState.THROWING) {
            int total = ResourceManager.getThrowImages().length;
            if (total > 0) {
                throwFrameIndex++;

                if (throwFrameIndex < 101) {
                    equippedItem.hold(this);
                } else if (throwFrameIndex == 101) {
                    equippedItem.toss(this);
                }

                if (throwFrameIndex >= total - 1) {
                    changeState(CharacterState.IDLE);
                    resetStateTimer();
                }
            }
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

    public boolean isFacingRight() {
        return facingRight;
    }

    public void setFacingRight(boolean facingRight) {
        this.facingRight = facingRight;
    }

    // Đã đổi kiểu trả về thành ThrowableItem
    public ThrowableItem getEquippedItem() {
        return equippedItem;
    }

    public void setPaused(boolean paused) {
        this.isPaused = paused;
        if (paused)
            changeState(CharacterState.IDLE);
    }
}