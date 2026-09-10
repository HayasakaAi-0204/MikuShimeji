package com.shimeji.miku;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;

public class TaskbarManager {

    // Các hằng số mô phỏng chính xác hệ điều hành
    private static final int TASKBAR_HEIGHT = 48;
    private static final int TASKBAR_BUFFER = 5;
    private static final int HIDE_DELAY_FRAMES = 15; // ~500ms (Độ trễ chờ rút chuột)
    private static final int SLIDE_SPEED = 8; // Tốc độ trượt của Taskbar

    private int currentFloorY; // Mặt đất hiện tại (Đang trượt)
    private int targetFloorY; // Mặt đất mục tiêu
    private int hideCooldown = 0;

    public TaskbarManager() {
        // Khởi tạo ban đầu ở đáy màn hình
        currentFloorY = Toolkit.getDefaultToolkit().getScreenSize().height;
        targetFloorY = currentFloorY;
    }

    // Hàm này được gọi mỗi Frame (30 FPS)
    public void update() {
        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration();
        Rectangle screenBounds = gc.getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);

        int screenBottom = screenBounds.height;

        // KIỂM TRA CHẾ ĐỘ AUTO-HIDE
        if (insets.bottom == 0) {
            // 1. Nếu đang bật Auto-Hide Taskbar
            try {
                Point mousePos = MouseInfo.getPointerInfo().getLocation();
                boolean isHovering = mousePos.y >= (screenBottom - TASKBAR_HEIGHT - TASKBAR_BUFFER);

                if (isHovering) {
                    targetFloorY = screenBottom - TASKBAR_HEIGHT; // Mục tiêu: Nổi lên
                    hideCooldown = HIDE_DELAY_FRAMES; // Reset bộ đếm trễ
                } else {
                    if (hideCooldown > 0) {
                        hideCooldown--; // Đang chờ...
                    } else {
                        targetFloorY = screenBottom; // Mục tiêu: Thụt xuống
                    }
                }
            } catch (Exception e) {
                // Bỏ qua nếu lỗi không lấy được tọa độ chuột
            }
        } else {
            // 2. Nếu TẮT Auto-hide (Taskbar luôn hiện)
            targetFloorY = screenBottom - insets.bottom;
        }

        // TẠO HOẠT ẢNH TRƯỢT TỰ NHIÊN (Linear Interpolation)
        if (currentFloorY < targetFloorY) {
            currentFloorY = Math.min(currentFloorY + SLIDE_SPEED, targetFloorY);
        } else if (currentFloorY > targetFloorY) {
            currentFloorY = Math.max(currentFloorY - SLIDE_SPEED, targetFloorY);
        }
    }

    // Hàm trả về Tọa độ Y cho phép nhân vật đứng lên
    public int getCurrentFloorY(int characterHeight) {
        return currentFloorY - characterHeight;
    }
}