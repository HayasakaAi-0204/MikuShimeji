package com.shimeji.miku;

import java.awt.Toolkit;
import java.awt.image.BufferedImage;

public class MikuCharacter {
    public static final int SIDE_PADDING = 60;

    public enum AppMode {
        GAMING, CASUAL, WORKING
    }

    private AppMode appMode = AppMode.CASUAL;

    // Thay thế các hằng số tĩnh cũ bằng các biến linh hoạt có thể thay đổi
    private long workTimeMs = 3600 * 1000L;
    private long breakTimeMs = 60 * 1000L;
    private long activeBreakDuration = 60 * 1000L;

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
        
        // Đọc giá trị thời gian đã lưu từ Menu khi khởi động app
        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(MikuMenu.class);
        this.workTimeMs = prefs.getInt("mikuWorkTime", 3600) * 1000L;
        this.breakTimeMs = prefs.getInt("mikuBreakTime", 60) * 1000L;
    }
    
    // 👉 HÀM MỚI: ĐỂ MENU GỌI VÀO CẬP NHẬT THỜI GIAN
    public void updateTimers(long workSeconds, long breakSeconds) {
        long newWorkMs = workSeconds * 1000L;
        long newBreakMs = breakSeconds * 1000L;
        
        boolean workChanged = (this.workTimeMs != newWorkMs);
        
        this.workTimeMs = newWorkMs;
        this.breakTimeMs = newBreakMs;
        
        // Nếu đang trong đợt làm việc và có thay đổi Work Time -> Reset đếm ngược lại từ đầu
        if ((pPhase == PomodoroPhase.WORKING || pPhase == PomodoroPhase.WAITING_TO_LAND || pPhase == PomodoroPhase.LEAVING_TO_GROW) && workChanged) {
            this.pomodoroStartTime = System.currentTimeMillis();
            this.pomodoroDuration = this.workTimeMs; 
        }
        // Lưu ý: Nếu đang nghỉ (Miku khổng lồ), ta KHÔNG đụng chạm gì tới activeBreakDuration của đợt này.
        // Thời gian Break mới sẽ tự động được áp dụng ở đợt Miku khổng lồ TIẾP THEO.
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
            this.x = screenWidth / 2 - this.getWidth() / 2;
            this.y = -200; 
            changeState(new FallState()); 
            justShrunk = true; 
        }

        if (mode == AppMode.WORKING) {
            pPhase = PomodoroPhase.WORKING;
            pomodoroStartTime = System.currentTimeMillis();
            pomodoroDuration = this.workTimeMs; // Dùng thời gian linh hoạt
        } else {
            pPhase = PomodoroPhase.INACTIVE;
        }

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
                // 👉 Dùng activeBreakDuration (Thời gian nghỉ đã được chốt cho đợt này)
                if (now - breakStartTime >= activeBreakDuration) {
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

    private static final int BASE_HEIGHT = 150;

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

        if (scale > 1.0) {
            if (newState instanceof ClimbState) {
                if (pPhase == PomodoroPhase.LEAVING_TO_SHRINK) {
                    scale = 1.0;
                    pPhase = PomodoroPhase.WORKING;
                    pomodoroStartTime = System.currentTimeMillis();
                    pomodoroDuration = this.workTimeMs; // Dùng thời gian linh hoạt
                    boolean wasWalkingLeft = (this.x < screenWidth / 2);
                    this.x = wasWalkingLeft ? -this.getWidth() : screenWidth;
                    newState = new WalkState(!wasWalkingLeft);
                } else {
                    newState = new IdleState();
                }
            }
            else if (!(newState instanceof IdleState) &&
                    !(newState instanceof WalkState) &&
                    !(newState instanceof FallState) &&
                    !(newState instanceof DragState)) {
                newState = new IdleState();
            }
        } else if (scale == 1.0 && pPhase == PomodoroPhase.LEAVING_TO_GROW && newState instanceof ClimbState) {
            scale = 8.0; 
            pPhase = PomodoroPhase.BREAK;
            breakStartTime = System.currentTimeMillis();
            
            // 👉 CHỐT THỜI GIAN NGHỈ CHO ĐỢT NÀY! Bất biến không thay đổi.
            activeBreakDuration = this.breakTimeMs; 
            
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
            // 👉 Đã xóa điều kiện ẩn, từ nay đồng hồ làm việc luôn hiển thị!
            return formatTime(Math.max(0, remaining));
            
        } else if (pPhase == PomodoroPhase.BREAK || pPhase == PomodoroPhase.LEAVING_TO_SHRINK) {
            long remaining = activeBreakDuration - (now - breakStartTime);
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
        if (scale > 1.0) {
            return 14; 
        }
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
        if (paused && this.appMode == AppMode.GAMING) {
            return; 
        }

        this.isPaused = paused;
        if (paused && (currentState instanceof ThrowState || currentState instanceof DeleteState)) {
            equippedItem.setInactive();
            setEquippedItem(new LeekItem());
            changeState(new IdleState());
        }
    }

    public void forceExitGiantState() {
        if (this.scale <= 1.0)
            return; 

        this.scale = 1.0;

        this.pPhase = PomodoroPhase.WORKING;
        this.pomodoroStartTime = System.currentTimeMillis();
        this.pomodoroDuration = this.workTimeMs; // Dùng thời gian linh hoạt

        this.x = screenWidth / 2 - this.getWidth() / 2;
        this.y = -200;

        this.changeState(new FallState());
    }

    public boolean isClimbing() {
        return this.currentState instanceof ClimbState;
    }
}