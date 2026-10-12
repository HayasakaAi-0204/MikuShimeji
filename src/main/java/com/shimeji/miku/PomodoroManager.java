package com.shimeji.miku;

import java.util.prefs.Preferences;

public class PomodoroManager {

    public enum Phase {
        INACTIVE, WORKING, WAITING_TO_LAND, LEAVING_TO_GROW, BREAK, LEAVING_TO_SHRINK
    }

    private Phase phase = Phase.INACTIVE;
    private long workTimeMs = 3600 * 1000L;
    private long breakTimeMs = 60 * 1000L;
    private long activeBreakDuration = 60 * 1000L;

    private long pomodoroStartTime = 0;
    private long pomodoroDuration = 0;
    private long breakStartTime = 0;
    private boolean enteredLeftWall = false;

    public PomodoroManager() {
        Preferences prefs = Preferences.userNodeForPackage(MikuMenu.class);
        this.workTimeMs = Math.max(10, prefs.getInt("mikuWorkTime", 3600)) * 1000L;
        this.breakTimeMs = Math.max(10, prefs.getInt("mikuBreakTime", 60)) * 1000L;
    }

    public boolean onModeChanged(MikuCharacter miku, MikuCharacter.AppMode mode) {
        boolean justShrunk = false;

        if (miku.getScale() > 1.0) {
            miku.setScale(1.0);
            miku.setX(miku.getScreenWidth() / 2 - miku.getWidth() / 2);
            miku.setY(-200);
            miku.changeState(new FallState());
            justShrunk = true;
        }

        if (mode == MikuCharacter.AppMode.WORKING) {
            phase = Phase.WORKING;
            pomodoroStartTime = System.currentTimeMillis();
            pomodoroDuration = this.workTimeMs;
        } else {
            phase = Phase.INACTIVE;
        }

        return justShrunk;
    }

    public void updatePhysics(MikuCharacter miku) {
        long now = System.currentTimeMillis();

        if (phase == Phase.WORKING) {
            if (now - pomodoroStartTime >= pomodoroDuration) {
                boolean isAirborne = (miku.getCurrentState() instanceof ClimbState
                        || miku.getCurrentState() instanceof FallState
                        || miku.getCurrentState() instanceof DragState);
                if (isAirborne) {
                    phase = Phase.WAITING_TO_LAND;
                    if (miku.getCurrentState() instanceof ClimbState) {
                        int landingX = (miku.getX() < miku.getScreenWidth() / 2)
                                ? -miku.getSidePadding() + 5
                                : miku.getScreenWidth() - miku.getWidth() + miku.getSidePadding() - 5;
                        miku.setX(landingX);
                    }
                    miku.changeState(new FallState());
                    miku.setPaused(false);
                } else {
                    phase = Phase.LEAVING_TO_GROW;
                    miku.changeState(new WalkState(miku.getX() < miku.getScreenWidth() / 2));
                    miku.setPaused(false);
                }
            }
        } else if (phase == Phase.WAITING_TO_LAND) {
            boolean isAirborne = (miku.getCurrentState() instanceof ClimbState
                    || miku.getCurrentState() instanceof FallState
                    || miku.getCurrentState() instanceof DragState);
            if (!isAirborne) {
                phase = Phase.LEAVING_TO_GROW;
                miku.changeState(new WalkState(miku.getX() < miku.getScreenWidth() / 2));
            }
        } else if (phase == Phase.BREAK) {
            if (now - breakStartTime >= activeBreakDuration) {
                phase = Phase.LEAVING_TO_SHRINK;
                miku.changeState(new WalkState(!enteredLeftWall));
            }
        }
    }

    public MikuState filterNewState(MikuCharacter miku, MikuState newState) {
        int screenWidth = miku.getScreenWidth();

        if (miku.getScale() > 1.0) {
            if (newState instanceof ClimbState) {
                if (phase == Phase.LEAVING_TO_SHRINK) {
                    miku.setScale(1.0);
                    phase = Phase.WORKING;
                    pomodoroStartTime = System.currentTimeMillis();
                    pomodoroDuration = this.workTimeMs;
                    boolean wasWalkingLeft = (miku.getX() < screenWidth / 2);
                    miku.setX(wasWalkingLeft ? -miku.getWidth() : screenWidth);
                    newState = new WalkState(!wasWalkingLeft);
                } else {
                    newState = new IdleState();
                }
            } else if (!(newState instanceof IdleState)
                    && !(newState instanceof WalkState)
                    && !(newState instanceof FallState)
                    && !(newState instanceof DragState)) {
                newState = new IdleState();
            }
        } else if (miku.getScale() == 1.0 && phase == Phase.LEAVING_TO_GROW && newState instanceof ClimbState) {
            miku.setScale(8.0);
            phase = Phase.BREAK;
            breakStartTime = System.currentTimeMillis();
            activeBreakDuration = this.breakTimeMs;

            enteredLeftWall = (miku.getX() < screenWidth / 2);
            miku.setX(enteredLeftWall ? -miku.getWidth() : screenWidth);
            newState = new WalkState(!enteredLeftWall);
        }

        return newState;
    }

    public void updateTimers(long workSeconds, long breakSeconds) {
        long newWorkMs = Math.max(10, workSeconds) * 1000L;
        long newBreakMs = Math.max(10, breakSeconds) * 1000L;

        boolean workChanged = (this.workTimeMs != newWorkMs);
        this.workTimeMs = newWorkMs;
        this.breakTimeMs = newBreakMs;

        if ((phase == Phase.WORKING || phase == Phase.WAITING_TO_LAND || phase == Phase.LEAVING_TO_GROW)
                && workChanged) {
            this.pomodoroStartTime = System.currentTimeMillis();
            this.pomodoroDuration = this.workTimeMs;
        }
    }

    public String getCountdownText() {
        long now = System.currentTimeMillis();

        if (phase == Phase.WORKING || phase == Phase.WAITING_TO_LAND || phase == Phase.LEAVING_TO_GROW) {
            long remaining = pomodoroDuration - (now - pomodoroStartTime);
            return formatTime(Math.max(0, remaining));
        } else if (phase == Phase.BREAK || phase == Phase.LEAVING_TO_SHRINK) {
            long remaining = activeBreakDuration - (now - breakStartTime);
            return formatTime(Math.max(0, remaining));
        }
        return null;
    }

    public boolean isForcedPomodoroWalk() {
        return phase == Phase.LEAVING_TO_GROW || phase == Phase.LEAVING_TO_SHRINK || phase == Phase.WAITING_TO_LAND;
    }

    public void forceExitGiantState(MikuCharacter miku) {
        if (miku.getScale() <= 1.0)
            return;

        miku.setScale(1.0);
        this.phase = Phase.WORKING;
        this.pomodoroStartTime = System.currentTimeMillis();
        this.pomodoroDuration = this.workTimeMs;

        miku.setX(miku.getScreenWidth() / 2 - miku.getWidth() / 2);
        miku.setY(-200);
        miku.changeState(new FallState());
    }

    private String formatTime(long ms) {
        if (ms < 0)
            ms = 0;
        long s = ms / 1000;
        return String.format("%02d:%02d", s / 60, s % 60);
    }
}