// File 1: App.java
package com.shimeji.miku;

import com.formdev.flatlaf.FlatDarkLaf; // 👉 Thêm dòng này lên nhóm import đầu file
import javax.swing.*;
import java.awt.*;

public class App {
    private static final int PHYSICS_TICK_RATE = 33;
    private static final int ANIMATION_TICK_RATE = 24;

    public static void main(String[] args) {
        // 👉 ÉP ĐỘNG CƠ JAVA SỬ DỤNG CARD ĐỒ HỌA (GPU) ĐỂ XỬ LÝ ẢNH TRONG SUỐT, GIẢI
        // CỨU CPU!
        System.setProperty("sun.java2d.d3d", "true");
        System.setProperty("sun.java2d.transaccel", "true");
        try {
            // 👉 KÍCH NỔ ĐỘNG CƠ JAVAFX ĐỂ CHẠY NHẠC
            javafx.application.Platform.startup(() -> {
            });

            // 👉 BẮT BUỘC DÙNG PHÔNG CHỮ SEGOE UI...
            UIManager.put("defaultFont", new Font("Dialog", Font.PLAIN, 13));

            // 👉 NỚI RỘNG KHOẢNG CÁCH DÒNG CHO THOÁNG (CHUẨN FLUENT DESIGN)
            UIManager.put("MenuItem.margin", new Insets(4, 8, 4, 8));
            UIManager.put("CheckBoxMenuItem.margin", new Insets(4, 8, 4, 8));
            // 👉 BẬT BỘ KHỬ RĂNG CƯA PHẦN CỨNG (CLEARTYPE) ĐỂ CHỮ NÉT CĂNG
            System.setProperty("awt.useSystemAAFontSettings", "lcd");
            System.setProperty("swing.aatext", "true");

            // 👉 ÉP MÀU CHỮ THÀNH TRẮNG TINH (SÁNG RÕ GIỐNG WINDOWS)
            UIManager.put("MenuItem.foreground", Color.WHITE);
            UIManager.put("CheckBoxMenuItem.foreground", Color.WHITE);

            // 👉 LÀM GIẢ HIỆU ỨNG KÍNH TRONG SUỐT (TRANSLUCENT) CỦA WINDOWS 11
            UIManager.put("PopupMenu.background", new Color(35, 35, 35, 220)); // Nền đen pha độ đục 220
            UIManager.put("MenuItem.background", new Color(0, 0, 0, 0)); // Làm trong suốt các dòng chữ
            UIManager.put("CheckBoxMenuItem.background", new Color(0, 0, 0, 0));

            // Kích hoạt giao diện
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception e) {
        }

        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        SwingUtilities.invokeLater(() -> startShimeji());
    }

    private static void startShimeji() {
        ResourceManager.loadImages();
        int screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
        MikuCharacter miku = new MikuCharacter(screenWidth / 2, 0);

        MikuWindow window = new MikuWindow(miku);
        window.setVisible(true);

        ProjectileWindow projectileWindow = new ProjectileWindow(window, miku);
        ExitButtonWindow exitWindow = new ExitButtonWindow(window, miku);
        TimerWindow timerWindow = new TimerWindow(window, miku); // 👉 Đăng ký đồng hồ góc phải
        TaskbarManager taskbarManager = new TaskbarManager();
        DesktopWatcher watcher = new DesktopWatcher(miku);
        watcher.startWatching();

        // 👉 KHỞI ĐỘNG CỖ MÁY ÂM THANH
        MusicPlayer musicPlayer = new MusicPlayer();
        new MikuMenu(miku, window, musicPlayer);

        Timer physicsTimer = new Timer(PHYSICS_TICK_RATE, e -> {
            taskbarManager.update();
            int currentFloorY = taskbarManager.getCurrentFloorY(miku.getHeight());

            // 👉 ĐÂY CHÍNH LÀ NƠI TRIỆT TIÊU KHOẢNG TRỐNG VÔ HÌNH:
            // Mức chuẩn là 15.
            // - Nếu khổng lồ vẫn lơ lửng: Bạn nhích nhẹ lên 17, 20.
            // - Nếu khổng lồ bị lún gót giày: Bạn hạ xuống 12, 10.
            // Nhờ có phép nhân với Scale, bạn chỉ cần canh chuẩn 1 lần là cả Miku nhỏ lẫn
            // bự đều sẽ chạm đất hoàn hảo!
            int emptySpace = 15;
            int feetOffset = (int) (emptySpace * miku.getScale());
            miku.updatePhysics(currentFloorY + feetOffset);

            if (miku.getAppMode() == MikuCharacter.AppMode.GAMING) {
                // 👉 LỌC NGHIÊM NGẶT NHẤT: Chỉ cho phép tự động né chuột khi đã yên vị bám trên
                // vách tường (IDLE).
                // Lúc đang Rơi hoặc đang Đi bộ lạch bạch dưới sàn, ẻm sẽ hoàn toàn phớt lờ con
                // chuột!
                if (miku.getState() == CharacterState.IDLE) {
                    Point mouse = MouseInfo.getPointerInfo().getLocation();
                    int dx = Math.abs(mouse.x - (miku.getX() + miku.getWidth() / 2));
                    int dy = Math.abs(mouse.y - (miku.getY() + miku.getHeight() / 2));
                    if (dx < 150 && dy < 150) {
                        if (miku.getX() < screenWidth / 2)
                            miku.changeState(new ClimbState(false, false, true));
                        else
                            miku.changeState(new ClimbState(true, false, true));
                    }
                }
            }

            window.syncBounds();
            window.repaint();

            projectileWindow.syncBounds();
            if (projectileWindow.isVisible())
                projectileWindow.repaint();

            // 👉 THÊM 2 DÒNG NÀY ĐỂ ĐỒNG BỘ NÚT X
            exitWindow.syncVisibility();
            if (exitWindow.isVisible())
                exitWindow.repaint();

            // 👉 Kích hoạt và đồng bộ hóa đồng hồ đếm ngược
            timerWindow.syncVisibility();
        });
        physicsTimer.start();

        Timer animationTimer = new Timer(ANIMATION_TICK_RATE, e -> miku.updateAnimation());
        animationTimer.start();

        Timer zOrderTimer = new Timer(2000, e -> {
            window.setAlwaysOnTop(false);
            window.setAlwaysOnTop(true);
        });
        zOrderTimer.start();
    }

}