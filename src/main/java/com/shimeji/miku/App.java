package com.shimeji.miku;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Toolkit;

public class App {

    // CHUẨN OOP: Cấu hình tập trung (Centralized Configuration)
    private static final int PHYSICS_TICK_RATE = 33; // Tốc độ cập nhật vật lý (~30 FPS)
    private static final int ANIMATION_TICK_RATE = 24; // Tốc độ lật ảnh (Giảm từ 200 xuống 24 để lật nhanh, mượt hơn)

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            startShimeji();
        });
    }

    private static void startShimeji() {
        // 1. Nạp tài nguyên (Ảnh)
        ResourceManager.loadImages();

        // 2. Khởi tạo dữ liệu (Model) ở giữa màn hình
        int screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
        MikuCharacter miku = new MikuCharacter(screenWidth / 2, 0);

        // 3. Khởi tạo giao diện (View)
        MikuWindow window = new MikuWindow(miku);
        window.setVisible(true);

        // THÊM MỚI (Chuẩn OOP): Khởi tạo Trình quản lý Môi trường độc lập
        // Tránh việc MikuCharacter phải tự đi hỏi hệ điều hành
        TaskbarManager taskbarManager = new TaskbarManager();

        // 4. Vòng lặp vật lý (Chạy 30 FPS)
        Timer physicsTimer = new Timer(PHYSICS_TICK_RATE, e -> {
            // Bước A: Cập nhật hệ thống môi trường (Taskbar trượt lên/xuống)
            taskbarManager.update();

            // Bước B: Lấy độ cao mặt đất thực tế và truyền vào cho Miku (Dependency
            // Injection)
            int currentFloorY = taskbarManager.getCurrentFloorY(miku.getHeight());
            miku.updatePhysics(currentFloorY);

            // Bước C: Cập nhật View
            window.syncBounds();
            window.repaint();
        });
        physicsTimer.start();

        // 5. Vòng lặp hoạt ảnh (Sử dụng Hằng số cấu hình)
        Timer animationTimer = new Timer(ANIMATION_TICK_RATE, e -> {
            miku.updateAnimation();
        });
        animationTimer.start();
    }
}