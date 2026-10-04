// File 3: MikuCharacter.java
package com.shimeji.miku;

import java.awt.Toolkit;
import java.awt.image.BufferedImage;

public class MikuCharacter {
    public static final int SIDE_PADDING = 60;

    public enum AppMode {
        GAMING, CASUAL, WORKING
    }

    private AppMode appMode = AppMode.CASUAL;

    public static final long WORK_MIN_MS = 1 * 15 * 1000L;
    public static final long WORK_MAX_MS = 1 * 15 * 1000L;
    public static final long WARNING_TIME_MS = 1 * 10 * 1000L;
    public static final long BREAK_TIME_MS = 60 * 1000L;

    public enum PomodoroPhase {
        INACTIVE, WORKING, WAITING_TO_LAND, LEAVING_TO_GROW, BREAK, LEAVING_TO_SHRINK
    }

    private PomodoroPhase pPhase = PomodoroPhase.INACTIVE;

    private long pomodoroStartTime = 0;
    private long pomodoroDuration = 0;
    private long breakStartTime = 0;
    private boolean enteredLeftWall = false;
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
        this.currentState = new FallState();
        this.currentState.enter(this);
    }

    public double getScale() {
        return scale;
    }

    public AppMode getAppMode() {
        return appMode;
    }

    public void setAppMode(AppMode mode) {
        this.appMode = mode;

        boolean justShrunk = false;

        // 👉 XỬ LÝ MƯỢT MÀ KHI TẮT NGANG MIKU KHỔNG LỒ
        if (this.scale > 1.0) {
            this.scale = 1.0;
            this.x = screenWidth / 2 - this.getWidth() / 2; // Căn giữa màn hình
            this.y = -200; // Nhấc bổng lên trần nhà ngoài màn hình
            changeState(new FallState()); // Thả rơi tự do!
            justShrunk = true; // Cắm cờ báo hiệu ẻm đang bận rớt!
        }

        if (mode == AppMode.WORKING) {
            pPhase = PomodoroPhase.WORKING;
            pomodoroStartTime = System.currentTimeMillis();
            pomodoroDuration = WORK_MIN_MS + (long) (Math.random() * (WORK_MAX_MS - WORK_MIN_MS));
        } else {
            pPhase = PomodoroPhase.INACTIVE;
        }

        if (mode == AppMode.GAMING) {
            this.pendingState = null;

            // Nếu ẻm ĐANG bận rớt từ nóc nhà xuống, thì cứ để ẻm rớt phịch xuống đất.
            // Cấm ép ẻm đu bám tường lúc đang ở trên trần nhà!
            if (!justShrunk) {
                boolean isAirborne = (currentState instanceof FallState || currentState instanceof DragState
                        || currentState instanceof ClimbState);
                boolean isLeftWall = (this.x < screenWidth / 2);
                if (isAirborne)
                    changeState(new ClimbState(isLeftWall, false, true));
                else
                    changeState(new WalkState(isLeftWall));
            }
        } else {
            if (currentState instanceof WalkState && !justShrunk)
                changeState(new IdleState());
        }
    }

    public void updatePhysics(int floorY) {
        equippedItem.updatePhysics(floorY);

        if (appMode == AppMode.WORKING) {
            long now = System.currentTimeMillis();
            if (pPhase == PomodoroPhase.WORKING) {
                if (now - pomodoroStartTime >= pomodoroDuration) {
                    boolean isAirborne = (currentState instanceof ClimbState || currentState instanceof FallState
                            || currentState instanceof DragState);
                    if (isAirborne) {
                        pPhase = PomodoroPhase.WAITING_TO_LAND;
                        if (currentState instanceof ClimbState) {
                            this.x = (this.x < screenWidth / 2) ? -this.getSidePadding() + 5
                                    : screenWidth - this.getWidth() + this.getSidePadding() - 5;
                        }
                        changeState(new FallState());
                        this.isPaused = false;
                    } else {
                        pPhase = PomodoroPhase.LEAVING_TO_GROW;
                        changeState(new WalkState(this.x < screenWidth / 2));
                        this.isPaused = false;
                    }
                }
            } else if (pPhase == PomodoroPhase.WAITING_TO_LAND) {
                boolean isAirborne = (currentState instanceof ClimbState || currentState instanceof FallState
                        || currentState instanceof DragState);
                if (!isAirborne) {
                    pPhase = PomodoroPhase.LEAVING_TO_GROW;
                    changeState(new WalkState(this.x < screenWidth / 2));
                }
            } else if (pPhase == PomodoroPhase.BREAK) {
                if (now - breakStartTime >= BREAK_TIME_MS) {
                    pPhase = PomodoroPhase.LEAVING_TO_SHRINK;
                    changeState(new WalkState(!enteredLeftWall));
                }
            }
        }

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
            if (currentState instanceof ClimbState)
                return ResourceManager.getClimbPauseImage();
            if (currentState instanceof FallState || currentState instanceof DragState)
                return currentState.getCurrentImage();
            return ResourceManager.getPausedImage();
        }
        return currentState.getCurrentImage();
    }

    private static final int BASE_HEIGHT = 150; // Khung xương gốc 150px

    public int getWidth() {
        BufferedImage currentImg = getCurrentImage();
        if (currentImg != null) {
            double aspect = (double) currentImg.getWidth() / currentImg.getHeight();
            this.width = (int) (BASE_HEIGHT * scale * aspect);
        }
        return this.width;
    }

    public int getHeight() {
        if (getCurrentImage() != null) {
            this.height = (int) (BASE_HEIGHT * scale);
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

        // 👉 KHÓA HOẠT ĐỘNG CHO MIKU KHỔNG LỒ (Tự động chặn luôn các tính năng thêm vào
        // trong tương lai)
        if (scale > 1.0) {
            if (newState instanceof ClimbState) {
                if (pPhase == PomodoroPhase.LEAVING_TO_SHRINK) {
                    scale = 1.0;
                    pPhase = PomodoroPhase.WORKING;
                    pomodoroStartTime = System.currentTimeMillis();
                    pomodoroDuration = WORK_MIN_MS + (long) (Math.random() * (WORK_MAX_MS - WORK_MIN_MS));
                    boolean wasWalkingLeft = (this.x < screenWidth / 2);
                    this.x = wasWalkingLeft ? -this.getWidth() : screenWidth;
                    newState = new WalkState(!wasWalkingLeft);
                } else {
                    newState = new IdleState();
                }
            }
            // Nếu trạng thái nội bộ không nằm trong 4 danh sách cho phép (Đứng, Đi, Rơi,
            // Kéo) -> Ép về Đứng im!
            else if (!(newState instanceof IdleState) &&
                    !(newState instanceof WalkState) &&
                    !(newState instanceof FallState) &&
                    !(newState instanceof DragState)) {
                newState = new IdleState();
            }
        } else if (scale == 1.0 && pPhase == PomodoroPhase.LEAVING_TO_GROW && newState instanceof ClimbState) {
            scale = 8.0; // 👉 Đã giữ nguyên thông số 8.0 của bạn!
            pPhase = PomodoroPhase.BREAK;
            breakStartTime = System.currentTimeMillis();
            enteredLeftWall = (this.x < screenWidth / 2);
            this.x = enteredLeftWall ? -this.getWidth() : screenWidth;
            newState = new WalkState(!enteredLeftWall);
        }

        this.currentState = newState;
        this.currentState.enter(this);
    }

    public void setState(CharacterState enumState) {
        if (appMode == AppMode.GAMING && enumState != CharacterState.DRAGGING && enumState != CharacterState.FALLING)
            return;

        // 👉 MIKU KHỔNG LỒ MODE: Chỉ nhận lệnh từ bên ngoài (Menu) đối với các hành
        // động dưới đây.
        // Lệnh ném hành hay bất kỳ lệnh lạ nào khác đều bị từ chối phục vụ!
        if (scale > 1.0) {
            if (enumState != CharacterState.IDLE &&
                    enumState != CharacterState.WALKING_LEFT &&
                    enumState != CharacterState.WALKING_RIGHT &&
                    enumState != CharacterState.DRAGGING &&
                    enumState != CharacterState.FALLING) {
                return;
            }
        }

        if (isForcedPomodoroWalk())
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
        long now = System.currentTimeMillis();

        if (pPhase == PomodoroPhase.WORKING || pPhase == PomodoroPhase.WAITING_TO_LAND
                || pPhase == PomodoroPhase.LEAVING_TO_GROW) {
            long remaining = pomodoroDuration - (now - pomodoroStartTime);
            if (remaining <= WARNING_TIME_MS)
                return formatTime(Math.max(0, remaining));
        } else if (pPhase == PomodoroPhase.BREAK || pPhase == PomodoroPhase.LEAVING_TO_SHRINK) {
            long remaining = BREAK_TIME_MS - (now - breakStartTime);
            return formatTime(Math.max(0, remaining));
        }
        return null;
    }

    private String formatTime(long ms) {
        if (ms < 0)
            ms = 0;
        long s = ms / 1000;
        return String.format("%02d:%02d", s / 60, s % 60);
    }

    public boolean isForcedPomodoroWalk() {
        return pPhase == PomodoroPhase.LEAVING_TO_GROW || pPhase == PomodoroPhase.LEAVING_TO_SHRINK
                || pPhase == PomodoroPhase.WAITING_TO_LAND;
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

    public int getSpeed() {
        // 👉 CHỈNH TỐC ĐỘ MIKU KHỔNG LỒ Ở ĐÂY
        if (scale > 1.0) {
            return 14; // Mặc định là 12. Bạn có thể tăng lên 16, 20... tùy thích!
        }

        // Tốc độ của Miku nhỏ (vẫn giữ nguyên không bị ảnh hưởng)
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
        this.isPaused = paused;
        if (paused && (currentState instanceof ThrowState || currentState instanceof DeleteState)) {
            equippedItem.setInactive();
            setEquippedItem(new LeekItem());
            changeState(new IdleState());
        }
    }

    public void forceExitGiantState() {
        if (this.scale <= 1.0)
            return; // Nếu đang nhỏ sẵn thì bỏ qua

        // 1. Ép biến lại thành Miku nhỏ
        this.scale = 1.0;

        // 2. Ép quay lại ca làm việc bình thường ngay lập tức
        this.pPhase = PomodoroPhase.WORKING;
        this.pomodoroStartTime = System.currentTimeMillis();
        this.pomodoroDuration = WORK_MIN_MS + (long) (Math.random() * (WORK_MAX_MS - WORK_MIN_MS));

        // 3. Dịch chuyển lên sát trần nhà giữa màn hình (giống y hệt lúc khởi động app)
        this.x = screenWidth / 2 - this.getWidth() / 2;
        this.y = -200;

        // 4. Kích hoạt trạng thái rơi tự do
        this.changeState(new FallState());
    }

    public boolean isClimbing() {
        return this.currentState instanceof ClimbState;
    }
}