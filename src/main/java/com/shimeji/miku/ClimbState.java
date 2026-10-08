package com.shimeji.miku;

import java.awt.image.BufferedImage;

public class ClimbState implements MikuState {
    private int frameIndex = 0;
    private int timer;
    private boolean isLeftWall;
    private boolean isClimbingUp;
    private boolean isResting;

    private static final int WALL_OFFSET = 12;

    public ClimbState(boolean isLeftWall, boolean isClimbingUp) {
        this(isLeftWall, isClimbingUp, false);
    }

    public ClimbState(boolean isLeftWall, boolean isClimbingUp, boolean isResting) {
        this.isLeftWall = isLeftWall;
        this.isClimbingUp = isClimbingUp;
        this.isResting = isResting;
    }

    private int getRandomTicks(int minSec, int maxSec) {
        int min = minSec * 30;
        int max = maxSec * 30;
        return min + (int) (Math.random() * (max - min + 1));
    }

    @Override
    public void enter(MikuCharacter miku) {
        if (isResting) {
            timer = getRandomTicks(1, 2);
            frameIndex = (int) (Math.random() * ResourceManager.TOTAL_CLIMB_FRAMES);
        } else {
            timer = getRandomTicks(5, 7);
            isClimbingUp = true;
            frameIndex = 0;
        }

        miku.setFacingRight(!isLeftWall);

        if (isLeftWall) {
            miku.setX(-miku.getSidePadding() - WALL_OFFSET);
        } else {
            miku.setX(miku.getScreenWidth() - miku.getWidth() + miku.getSidePadding() + WALL_OFFSET);
        }
    }

    private void fallDown(MikuCharacter miku) {
        if (isLeftWall) {
            // 👉 SỬA LỖI Ở ĐÂY: Khi tuột tay rớt xuống, đạp mạnh văng ra khỏi tường 5 pixel
            miku.setX(-miku.getSidePadding() + 5);
        } else {
            int rightWall = miku.getScreenWidth() - miku.getWidth() + miku.getSidePadding();
            // Văng ra khỏi tường bên phải 5 pixel
            miku.setX(rightWall - 5);
        }
        miku.changeState(new FallState());
    }

    private void decideNextAction(MikuCharacter miku) {
        double chance = Math.random();
        boolean isGaming = (miku.getAppMode() == MikuCharacter.AppMode.GAMING);

        if (isResting) {
            // 👉 BÌNH THƯỜNG: 10% rơi, 30% bám tiếp, 30% lên, 30% xuống
            // 👉 GAMING: 0% rơi, 40% bám tiếp, 30% lên, 30% xuống
            double fallThreshold = isGaming ? 0.0 : 0.10;

            if (chance < fallThreshold) {
                fallDown(miku);
            } else if (chance < 0.40) {
                isResting = true;
                timer = getRandomTicks(1, 2);
            } else if (chance < 0.70) {
                isResting = false;
                isClimbingUp = true;
                timer = getRandomTicks(2, 4);
                frameIndex = 0;
            } else {
                isResting = false;
                isClimbingUp = false;
                timer = getRandomTicks(2, 4);
                frameIndex = ResourceManager.TOTAL_CLIMB_FRAMES - 1;
            }
        } else if (isClimbingUp) {
            // 👉 BÌNH THƯỜNG: 10% rơi, 30% lên tiếp, 60% bám tường
            // 👉 GAMING: 0% rơi, 30% lên tiếp, 70% bám tường
            double fallThreshold = isGaming ? 0.0 : 0.10;
            double upThreshold = isGaming ? 0.30 : 0.40;

            if (chance < fallThreshold) {
                fallDown(miku);
            } else if (chance < upThreshold) {
                isResting = false;
                isClimbingUp = true;
                timer = getRandomTicks(2, 4);
            } else {
                isResting = true;
                timer = getRandomTicks(1, 2);
            }
        } else {
            // 👉 BÌNH THƯỜNG: 10% rơi, 30% xuống tiếp, 60% bám tường
            // 👉 GAMING: 0% rơi, 30% xuống tiếp, 70% bám tường
            double fallThreshold = isGaming ? 0.0 : 0.10;
            double downThreshold = isGaming ? 0.30 : 0.40;

            if (chance < fallThreshold) {
                fallDown(miku);
            } else if (chance < downThreshold) {
                isResting = false;
                isClimbingUp = false;
                timer = getRandomTicks(2, 4);
            } else {
                isResting = true;
                timer = getRandomTicks(1, 2);
            }
        }
    }

    @Override
    public void updatePhysics(MikuCharacter miku, int floorY) {
        timer--;
        if (timer <= 0) {
            decideNextAction(miku);
            return;
        }

        if (isResting) {
            return;
        }

        int climbSpeed = miku.getSpeed() / 2;
        boolean isGaming = (miku.getAppMode() == MikuCharacter.AppMode.GAMING);

        if (isClimbingUp) {
            miku.setY(miku.getY() - climbSpeed);
            // 👉 NGĂN RỚT KHI ĐỤNG TRẦN Ở GAMING MODE
            if (miku.getY() <= 0) {
                if (isGaming) {
                    miku.setY(0);
                    isResting = true;
                    timer = getRandomTicks(1, 2);
                } else {
                    fallDown(miku);
                }
                return;
            }
        } else {
            miku.setY(miku.getY() + climbSpeed);
            // 👉 NGĂN RỚT KHI ĐỤNG SÀN Ở GAMING MODE
            if (miku.getY() >= floorY) {
                if (isGaming) {
                    miku.setY(floorY);
                    isResting = true;
                    timer = getRandomTicks(1, 2);
                } else {
                    miku.setY(floorY);
                    miku.changeState(new IdleState());
                }
                return;
            }
        }
    }

    @Override
    public void updateAnimation(MikuCharacter miku) {
        if (isResting)
            return;

        if (isClimbingUp) {
            frameIndex = (frameIndex + 1) % ResourceManager.TOTAL_CLIMB_FRAMES;
        } else {
            frameIndex = (frameIndex - 1 + ResourceManager.TOTAL_CLIMB_FRAMES) % ResourceManager.TOTAL_CLIMB_FRAMES;
        }
    }

    @Override
    public BufferedImage getCurrentImage() {
        return ResourceManager.getClimbImage(frameIndex);
    }
}