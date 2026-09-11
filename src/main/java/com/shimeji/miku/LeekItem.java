package com.shimeji.miku;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

public class LeekItem implements ThrowableItem {

    public enum State {
        INACTIVE, HELD, FLYING
    }

    private State currentState = State.INACTIVE;

    private int x, y;
    private double velocityX, velocityY;
    private boolean isFacingRight = true;

    private static final double GRAVITY = 1.2;
    private static final int DISPLAY_WIDTH = 30; // Kích thước gọn gàng

    private double rotationAngle = 0;
    private double rotationSpeed = 0;

    @Override
    public void setInactive() {
        this.currentState = State.INACTIVE;
    }

    // =================================================================
    // TỌA ĐỘ LẤY ĐÀ
    // =================================================================
    @Override
    public void hold(MikuCharacter miku) {
        this.currentState = State.HELD;
        this.isFacingRight = miku.isFacingRight();

        int centerY = miku.getY() + 95; // Tầm ngực/tay

        if (this.isFacingRight) {
            // Nhìn phải: Tính từ mép trái (x) + một khoảng cố định
            this.x = miku.getX() + 175;
        } else {
            // Nhìn trái (Ảnh bị lật): Tính từ mép phải (x + width) - lùi lại một khoảng cố
            // định
            this.x = miku.getX() + miku.getWidth() - 175;
        }

        this.y = centerY;
        this.rotationAngle = this.isFacingRight ? Math.toRadians(30) : Math.toRadians(-30);
    }

    // =================================================================
    // TỌA ĐỘ NÉM VÀ HƯỚNG BAY VẬT LÝ
    // =================================================================
    @Override
    public void toss(MikuCharacter miku) {
        this.currentState = State.FLYING;
        this.isFacingRight = miku.isFacingRight();

        // 1. Tọa độ xuất phát (Dựa theo tọa độ hold)
        int centerY = miku.getY() + 95;

        if (this.isFacingRight) {
            // Tay duỗi thẳng ra bên phải
            this.x = miku.getX() + 195;
        } else {
            // Tay duỗi thẳng ra bên trái
            this.x = miku.getX() + miku.getWidth() - 195;
        }

        this.y = centerY - 15; // Hơi hất tay lên cao một chút lúc ném

        // 2. Tính lực ném (Parabol ngẫu nhiên)
        this.velocityY = -(10 + Math.random() * 6);
        double horizontalForce = 12 + Math.random() * 8;

        // =================================================================
        // SỬA THEO YÊU CẦU: ĐẢO NGƯỢC HƯỚNG BAY
        // Đảo ngược dấu của horizontalForce so với phiên bản trước
        // =================================================================
        if (this.isFacingRight) {
            // Miku nhìn hướng nào thì gán lực đẩy NGƯỢC LẠI so với code cũ
            this.velocityX = -Math.abs(horizontalForce);
        } else {
            this.velocityX = Math.abs(horizontalForce);
        }

        // Tốc độ xoay của cọng hành
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

        // Nếu bay ra khỏi màn hình (quá xa) thì vô hiệu hóa để tiết kiệm tài nguyên
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

        // TỐI ƯU TÀI NGUYÊN: Sử dụng RenderingHints để ảnh mượt mà khi thu nhỏ/xoay
        Object oldInterpolation = g2d.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        Object oldAntialiasing = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        AffineTransform oldTransform = g2d.getTransform();

        g2d.translate(x, y);
        g2d.rotate(rotationAngle);

        // =================================================================
        // ĐẢO NGƯỢC LOGIC LẬT ẢNH CỌNG HÀNH (Đồng bộ với hướng bay mới)
        // =================================================================
        if (isFacingRight) {
            // Vì hướng bay đã đảo ngược, logic vẽ lật cũng phải đảo ngược theo
            g2d.drawImage(img, -DISPLAY_WIDTH / 2, -displayH / 2, DISPLAY_WIDTH, displayH, null);
        } else {
            g2d.drawImage(img, DISPLAY_WIDTH / 2, -displayH / 2, -DISPLAY_WIDTH, displayH, null);
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
}