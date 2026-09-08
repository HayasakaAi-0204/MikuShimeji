package com.shimeji.miku;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Toolkit;

public class App {

    // CHUẨN OOP: Cấu hình tập trung (Centralized Configuration)
    // Loại bỏ hoàn toàn số ma thuật. Dễ dàng bảo trì và tinh chỉnh.
    private static final int PHYSICS_TICK_RATE = 33; // Tốc độ cập nhật vật lý (~30 FPS)
    private static final int ANIMATION_TICK_RATE = 24; // Tốc độ lật ảnh (Giảm từ 200 xuống 40 để lật nhanh, mượt hơn)

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

        // 4. Vòng lặp vật lý (Chạy 30 FPS)
        Timer physicsTimer = new Timer(PHYSICS_TICK_RATE, e -> {
            miku.updatePhysics();
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