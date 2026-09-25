package com.shimeji.miku;

import java.awt.image.BufferedImage;

public class ClimbState implements MikuState {
    private int frameIndex = 0;
    private int timer;
    private boolean isLeftWall;
    private boolean isClimbingUp; 
    private boolean isResting; 

    // Thông số căn lề bám tường (5 là mức bạn đã thấy hợp lý)
    private static final int WALL_OFFSET = 5; 

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
            // Bị kéo vào tường -> Bám tường 1-2s
            timer = getRandomTicks(1, 2);
            frameIndex = (int) (Math.random() * ResourceManager.TOTAL_CLIMB_FRAMES);
        } else {
            // Đi bộ chạm tường -> Leo lên 5-7s
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

    // Hàm phụ trợ để xử lý rơi (áp dụng cho cả rơi ngẫu nhiên và rơi do đụng trần)
    private void fallDown(MikuCharacter miku) {
        // Đẩy nhẹ Miku ra khỏi tường 5 pixel để không bị nam châm kéo dính lại vào tường
        if (isLeftWall) {
            miku.setX(-miku.getSidePadding() + 5); 
        } else {
            int rightWall = miku.getScreenWidth() - miku.getWidth() + miku.getSidePadding();
            miku.setX(rightWall - 5);
        }
        miku.changeState(new FallState());
    }

    private void decideNextAction(MikuCharacter miku) {
        double chance = Math.random(); 
        
        if (isResting) {
            // Sau khi BÁM TƯỜNG: 10% rơi, 30% bám tiếp 1-2s, 30% lên 2-4s, 30% xuống 2-4s
            if (chance < 0.10) { 
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
        } 
        else if (isClimbingUp) {
            // Sau khi LEO LÊN: 10% rơi, 30% lên tiếp 2-4s, 60% bám tường 1-2s
            if (chance < 0.10) {
                fallDown(miku);
            } else if (chance < 0.40) {
                isResting = false;
                isClimbingUp = true;
                timer = getRandomTicks(2, 4);
            } else {
                isResting = true;
                timer = getRandomTicks(1, 2);
            }
        } 
        else {
            // Sau khi LEO XUỐNG: 10% rơi, 30% xuống tiếp 2-4s, 60% bám tường 1-2s
            if (chance < 0.10) {
                fallDown(miku);
            } else if (chance < 0.40) {
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

        if (isClimbingUp) {
            miku.setY(miku.getY() - climbSpeed); 
            // 👉 THAY ĐỔI TẠI ĐÂY: Đụng nóc trần nhà -> Rớt thẳng xuống đất!
            if (miku.getY() <= 0) {
                fallDown(miku);
                return;
            }
        } else {
            miku.setY(miku.getY() + climbSpeed); 
            if (miku.getY() >= floorY) {
                miku.setY(floorY);
                miku.changeState(new IdleState());
                return;
            }
        }
    }

    @Override
    public void updateAnimation(MikuCharacter miku) {
        if (isResting) return; 

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