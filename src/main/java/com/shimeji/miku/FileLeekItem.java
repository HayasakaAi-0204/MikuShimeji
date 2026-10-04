package com.shimeji.miku;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

public class FileLeekItem implements ThrowableItem {
    public enum State { INACTIVE, HELD, FLYING }
    private State currentState = State.INACTIVE;
    private int x, y;
    private double velocityX, velocityY;
    private boolean isFacingRight = true;
    private static final double BASE_GRAVITY = 1.2;
    private static final int DISPLAY_WIDTH = 30;
    private static final int ICON_SIZE = 24;
    private double rotationAngle = 0;
    private double rotationSpeed = 0;
    private BufferedImage fileIcon;
    private double scale = 1.0;

    public FileLeekItem(BufferedImage fileIcon) { this.fileIcon = fileIcon; }
    @Override public double getScale() { return scale; }
    @Override public void setInactive() { this.currentState = State.INACTIVE; }

    @Override
    public void hold(MikuCharacter miku) {
        this.currentState = State.HELD;
        this.isFacingRight = miku.isFacingRight();
        this.scale = miku.getScale();
        int centerY = miku.getY() + (int)(90 * scale);
        this.x = miku.getX() + (this.isFacingRight ? miku.getWidth() - (int)(160 * scale) : (int)(160 * scale));
        this.y = centerY;
        this.rotationAngle = this.isFacingRight ? Math.toRadians(30) : Math.toRadians(-30);
    }

    @Override
    public void toss(MikuCharacter miku) {
        this.currentState = State.FLYING;
        this.isFacingRight = miku.isFacingRight();
        this.scale = miku.getScale();
        int centerY = miku.getY() + (int)(95 * scale);
        this.x = miku.getX() + (this.isFacingRight ? miku.getWidth() - (int)(15 * scale) : (int)(15 * scale));
        this.y = centerY - (int)(15 * scale);
        this.velocityY = -(12 + Math.random() * 6) * scale;
        double horizontalForce = (15 + Math.random() * 8) * scale;
        this.velocityX = this.isFacingRight ? Math.abs(horizontalForce) : -Math.abs(horizontalForce);
        this.rotationSpeed = (Math.random() * 0.4) + 0.2;
        if (!this.isFacingRight) this.rotationSpeed = -this.rotationSpeed;
    }

    @Override
    public void updatePhysics(int floorY) {
        if (currentState != State.FLYING) return;
        velocityY += (BASE_GRAVITY * scale);
        x += velocityX; y += velocityY; rotationAngle += rotationSpeed;
        if (y > floorY + 2000 || x < -2000 || x > 5000) currentState = State.INACTIVE;
    }

    @Override
    public void draw(Graphics2D g2d) {
        if (currentState == State.INACTIVE) return;
        BufferedImage img = ResourceManager.getLeekImage();
        if (img == null) return;
        int displayW = (int) (DISPLAY_WIDTH * scale);
        int displayH = (int) ((double) DISPLAY_WIDTH * img.getHeight() / img.getWidth() * scale);
        int scaledIconSize = (int)(ICON_SIZE * scale);

        Object oldInterpolation = g2d.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        Object oldAntialiasing = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        AffineTransform oldTransform = g2d.getTransform();
        int centerOffset = (int)(60 * scale);
        g2d.translate(centerOffset, centerOffset);
        g2d.rotate(rotationAngle);

        if (!isFacingRight) {
            g2d.drawImage(img, -displayW / 2, -displayH / 2, displayW, displayH, null);
            if (fileIcon != null) g2d.drawImage(fileIcon, -scaledIconSize / 2, -displayH / 2 + (int)(5*scale), scaledIconSize, scaledIconSize, null);
        } else {
            g2d.drawImage(img, displayW / 2, -displayH / 2, -displayW, displayH, null);
            if (fileIcon != null) g2d.drawImage(fileIcon, -scaledIconSize / 2, -displayH / 2 + (int)(5*scale), scaledIconSize, scaledIconSize, null);
        }

        g2d.setTransform(oldTransform);
        if (oldInterpolation != null) g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldInterpolation);
        if (oldAntialiasing != null) g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAntialiasing);
    }
    @Override public boolean isActive() { return currentState != State.INACTIVE; }
    @Override public int getX() { return x; }
    @Override public int getY() { return y; }
}