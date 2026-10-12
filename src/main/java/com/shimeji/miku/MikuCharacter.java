package com.shimeji.miku;

import java.awt.Toolkit;
import java.awt.image.BufferedImage;

public class MikuCharacter {
    public static final int SIDE_PADDING = 60;
    private static final int BASE_HEIGHT = 150;

    public enum AppMode {
        GAMING, CASUAL, WORKING
    }

    private AppMode appMode = AppMode.CASUAL;
    private final PomodoroManager pomodoroManager;

    private double scale = 1.0;
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
        this.pomodoroManager = new PomodoroManager();
        this.currentState = new FallState();
        this.currentState.enter(this);
    }

    public void updateTimers(long workSeconds, long breakSeconds) {
        pomodoroManager.updateTimers(workSeconds, breakSeconds);
    }

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

    public AppMode getAppMode() {
        return appMode;
    }

    public void setAppMode(AppMode mode) {
        this.appMode = mode;
        boolean justShrunk = pomodoroManager.onModeChanged(this, mode);

        if (mode == AppMode.GAMING) {
            if (!justShrunk) {
                boolean isLeftWall = (this.x < screenWidth / 2);
                if (currentState instanceof FallState || currentState instanceof DragState) {
                    this.pendingState = new WalkState(isLeftWall);
                } else if (currentState instanceof ClimbState) {
                    this.pendingState = null;
                } else {
                    this.pendingState = null;
                    changeState(new WalkState(isLeftWall));
                }
            } else {
                this.pendingState = null;
            }
        } else {
            if (currentState instanceof WalkState && !justShrunk)
                changeState(new IdleState());
        }
    }

    public void updatePhysics(int floorY) {
        equippedItem.updatePhysics(floorY);

        if (appMode == AppMode.WORKING) {
            pomodoroManager.updatePhysics(this);
        }

        // 👉 Nếu đang rơi (FallState) thì trọng lực bắt buộc phải kéo xuống sàn, không
        // cho treo giữa trời!
        if (isPaused && !(currentState instanceof FallState))
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
            if (currentState instanceof ClimbState)
                return ResourceManager.getClimbPauseImage();
            if (currentState instanceof FallState || currentState instanceof DragState)
                return currentState.getCurrentImage();
            return ResourceManager.getPausedImage();
        }
        return currentState.getCurrentImage();
    }

    public int getWidth() {
        BufferedImage currentImg = getCurrentImage();
        if (currentImg != null) {
            double aspect = (double) currentImg.getWidth() / currentImg.getHeight();
            this.width = (int) (this.getHeight() * aspect);
        }
        return this.width;
    }

    public int getHeight() {
        BufferedImage currentImg = getCurrentImage();
        if (currentImg != null) {
            double heightRatio = (double) currentImg.getHeight() / 250.0;
            this.height = (int) (BASE_HEIGHT * scale * heightRatio);
        }
        return this.height;
    }

    public void changeState(MikuState newState) {
        if (this.currentState instanceof DeleteState && !(newState instanceof DeleteState)) {
            if (!(this.equippedItem instanceof LeekItem))
                this.setEquippedItem(new LeekItem());
        }
        if (this.currentState instanceof ThrowState && !(newState instanceof IdleState)) {
            this.equippedItem.setInactive();
            this.setEquippedItem(new LeekItem());
        }

        // Lọc qua PomodoroManager để xử lý phóng to/thu nhỏ
        newState = pomodoroManager.filterNewState(this, newState);

        this.currentState = newState;
        this.currentState.enter(this);
    }

    public void setState(CharacterState enumState) {
        if (appMode == AppMode.GAMING && enumState != CharacterState.DRAGGING && enumState != CharacterState.FALLING)
            return;

        if (scale > 1.0) {
            if (enumState != CharacterState.IDLE &&
                    enumState != CharacterState.WALKING_LEFT &&
                    enumState != CharacterState.WALKING_RIGHT &&
                    enumState != CharacterState.DRAGGING &&
                    enumState != CharacterState.FALLING) {
                return;
            }
        }

        if (isForcedPomodoroWalk() && enumState != CharacterState.FALLING)
            return;

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
            boolean isAirborne = (currentState instanceof FallState || currentState instanceof DragState
                    || currentState instanceof ClimbState);
            if (isAirborne && enumState != CharacterState.DRAGGING && enumState != CharacterState.FALLING) {
                pendingState = mappedState;
                if (currentState instanceof ClimbState) {
                    this.x = (this.x < screenWidth / 2) ? -this.getSidePadding() + 5
                            : screenWidth - this.getWidth() + this.getSidePadding() - 5;
                    changeState(new FallState());
                }
            } else {
                pendingState = null;
                changeState(mappedState);
            }
        }
    }

    public void triggerDeleteAction(String fileName, BufferedImage fileIcon) {
        if (appMode == AppMode.GAMING || scale > 1.0)
            return;

        if (isForcedPomodoroWalk())
            return;

        if (currentState instanceof ThrowState || currentState instanceof DeleteState)
            return;

        DeleteState deleteState = new DeleteState(fileIcon);
        boolean isAirborne = (currentState instanceof FallState || currentState instanceof DragState
                || currentState instanceof ClimbState);
        if (isAirborne)
            pendingState = deleteState;
        else
            changeState(deleteState);
    }

    public String getCountdownText() {
        if (appMode != AppMode.WORKING)
            return null;
        return pomodoroManager.getCountdownText();
    }

    public boolean isForcedPomodoroWalk() {
        return pomodoroManager.isForcedPomodoroWalk();
    }

    public void forceExitGiantState() {
        pomodoroManager.forceExitGiantState(this);
    }

    public CharacterState getState() {
        if (currentState instanceof DragState)
            return CharacterState.DRAGGING;
        if (currentState instanceof FallState)
            return CharacterState.FALLING;
        if (currentState instanceof ThrowState)
            return CharacterState.THROWING;
        if (currentState instanceof WalkState)
            return ((WalkState) currentState).isLeft() ? CharacterState.WALKING_LEFT : CharacterState.WALKING_RIGHT;
        return CharacterState.IDLE;
    }

    public MikuState getCurrentState() {
        return currentState;
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

    public int getSpeed() {
        if (scale > 1.0)
            return 14;
        return speed;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getSidePadding() {
        return (int) (SIDE_PADDING * scale);
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

    public void setEquippedItem(ThrowableItem item) {
        this.equippedItem = item;
    }

    public void setPaused(boolean paused) {
        // 👉 Miễn nhiễm tạm dừng khi đang ở Gaming Mode HOẶC đang đi bộ làm nhiệm vụ
        // Pomodoro
        if (paused && (this.appMode == AppMode.GAMING || isForcedPomodoroWalk())) {
            return;
        }

        this.isPaused = paused;
        if (paused && (currentState instanceof ThrowState || currentState instanceof DeleteState)) {
            equippedItem.setInactive();
            setEquippedItem(new LeekItem());
            changeState(new IdleState());
        }
    }

    public boolean isClimbing() {
        return this.currentState instanceof ClimbState;
    }
}