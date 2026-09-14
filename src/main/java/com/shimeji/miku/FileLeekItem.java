package com.shimeji.miku;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

public class FileLeekItem implements ThrowableItem {

    public enum State {
        INACTIVE, HELD, FLYING
    }

    private State currentState = State.INACTIVE;
    private int x, y;
    private double velocityX, velocityY;
    private boolean isFacingRight = true;

    private static final double GRAVITY = 1.2;
    private static final int DISPLAY_WIDTH = 30;
    private static final int ICON_SIZE = 24;

    private double rotationAngle = 0;
    private double rotationSpeed = 0;

    private BufferedImage fileIcon;

    public FileLeekItem(BufferedImage fileIcon) {
        this.fileIcon = fileIcon;
    }

    @Override
    public void setInactive() {
        this.currentState = State.INACTIVE;
    }

    @Override
    public void hold(MikuCharacter miku) {
        this.currentState = State.HELD;
        this.isFacingRight = miku.isFacingRight();
        int centerY = miku.getY() + 90;

        if (this.isFacingRight) {
            this.x = miku.getX() + miku.getWidth() - 160;
        } else {
            this.x = miku.getX() + 160;
        }

        this.y = centerY;
        this.rotationAngle = this.isFacingRight ? Math.toRadians(30) : Math.toRadians(-30);
    }

    @Override
    public void toss(MikuCharacter miku) {
        this.currentState = State.FLYING;
        this.isFacingRight = miku.isFacingRight();
        int centerY = miku.getY() + 95;

        if (this.isFacingRight) {
            this.x = miku.getX() + miku.getWidth() - 15;
        } else {
            this.x = miku.getX() + 15;
        }

        this.y = centerY - 15;

        this.velocityY = -(12 + Math.random() * 6);
        double horizontalForce = 15 + Math.random() * 8;

        if (this.isFacingRight) {
            this.velocityX = Math.abs(horizontalForce);
        } else {
            this.velocityX = -Math.abs(horizontalForce);
        }

        this.rotationSpeed = (Math.random() * 0.4) + 0.2;
        if (!this.isFacingRight) {
            this.rotationSpeed = -this.rotationSpeed;
        }
    }

    @Override
    public void updatePhysics(int floorY) {
        if (currentState != State.FLYING)
            return;

        velocityY += GRAVITY;
        x += velocityX;
        y += velocityY;
        rotationAngle += rotationSpeed;

        if (y > floorY + 1000 || x < -500 || x > 3000) {
            currentState = State.INACTIVE;
        }
    }

    @Override
    public void draw(Graphics2D g2d) {
        if (currentState == State.INACTIVE)
            return;

        BufferedImage img = ResourceManager.getLeekImage();
        if (img == null)
            return;

        int realWidth = img.getWidth();
        int realHeight = img.getHeight();
        int displayH = (int) ((double) DISPLAY_WIDTH * realHeight / realWidth);

        Object oldInterpolation = g2d.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        Object oldAntialiasing = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        AffineTransform oldTransform = g2d.getTransform();
        g2d.translate(60, 60);
        g2d.rotate(rotationAngle);

        // ĐÃ SỬA LỖI: Thêm dấu "!" để lật ngược đầu/đuôi cọng hành cho đúng chiều
        if (!isFacingRight) {
            g2d.drawImage(img, -DISPLAY_WIDTH / 2, -displayH / 2, DISPLAY_WIDTH, displayH, null);
            if (fileIcon != null) {
                g2d.drawImage(fileIcon, -ICON_SIZE / 2, -displayH / 2 + 5, ICON_SIZE, ICON_SIZE, null);
            }
        } else {
            g2d.drawImage(img, DISPLAY_WIDTH / 2, -displayH / 2, -DISPLAY_WIDTH, displayH, null);
            if (fileIcon != null) {
                g2d.drawImage(fileIcon, -ICON_SIZE / 2, -displayH / 2 + 5, ICON_SIZE, ICON_SIZE, null);
            }
        }

        g2d.setTransform(oldTransform);
        if (oldInterpolation != null)
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldInterpolation);
        if (oldAntialiasing != null)
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAntialiasing);
    }

    @Override
    public boolean isActive() {
        return currentState != State.INACTIVE;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }
}