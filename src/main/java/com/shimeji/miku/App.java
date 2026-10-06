// File 1: App.java
package com.shimeji.miku;

import com.formdev.flatlaf.FlatDarkLaf; // 👉 Thêm dòng này lên nhóm import đầu file
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import javax.swing.event.*;

public class App {
    private static final int PHYSICS_TICK_RATE = 33;
    private static final int ANIMATION_TICK_RATE = 24;

    public static void main(String[] args) {
        try {
            // 👉 BẮT BUỘC DÙNG PHÔNG CHỮ SEGOE UI CỦA MICROSOFT
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
        setupSystemTray(miku, window, musicPlayer); // Cập nhật để truyền máy phát nhạc vào Menu

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
                if (miku.getState() != CharacterState.WALKING_LEFT) {
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
    }

    private static void setupSystemTray(MikuCharacter miku, MikuWindow window, MusicPlayer musicPlayer) {
        if (!SystemTray.isSupported())
            return;
        SystemTray tray = SystemTray.getSystemTray();
        URL iconUrl = App.class.getResource("/images/miku_icon.png");
        Image iconImage = Toolkit.getDefaultToolkit().getImage(iconUrl);
        window.setIconImage(iconImage);

        JPopupMenu swingPopup = new JPopupMenu();
        JMenuItem titleItem = new JMenuItem("--- Chọn chế độ ---");
        titleItem.setEnabled(false);

        JCheckBoxMenuItem casualItem = new JCheckBoxMenuItem("Casual Mode", true);
        JCheckBoxMenuItem gamingItem = new JCheckBoxMenuItem("Gaming Mode");
        JCheckBoxMenuItem workingItem = new JCheckBoxMenuItem("Working Mode");

        casualItem.addActionListener(e -> {
            gamingItem.setSelected(false);
            workingItem.setSelected(false);
            casualItem.setSelected(true);
            miku.setAppMode(MikuCharacter.AppMode.CASUAL);
        });

        gamingItem.addActionListener(e -> {
            casualItem.setSelected(false);
            workingItem.setSelected(false);
            gamingItem.setSelected(true);
            miku.setAppMode(MikuCharacter.AppMode.GAMING);
        });

        workingItem.addActionListener(e -> {
            casualItem.setSelected(false);
            gamingItem.setSelected(false);
            workingItem.setSelected(true);
            miku.setAppMode(MikuCharacter.AppMode.WORKING);
        });
        // 👉 TẠO CÁC NÚT ĐIỀU KHIỂN NHẠC
        JMenuItem trackNameItem = new JMenuItem("🎵 Nhạc: Ngừng phát");
        trackNameItem.setEnabled(false); // Làm mờ đi vì nó chỉ dùng để làm màn hình hiển thị chữ

        JMenuItem playItem = new JMenuItem("▶ Phát / Tạm dừng");
        JMenuItem nextItem = new JMenuItem("⏭ Chuyển bài kế tiếp");
        // 👉 THÊM NÚT CHỌN THƯ MỤC
        JMenuItem changeFolderItem = new JMenuItem("📂 Chọn thư mục nhạc...");

        // Gán chức năng cho nút
        playItem.addActionListener(e -> musicPlayer.togglePlayPause());
        nextItem.addActionListener(e -> musicPlayer.next());

        // Lệnh gọi cửa sổ Windows Native siêu mượt
        changeFolderItem.addActionListener(e -> {
            // Dùng cửa sổ File gốc của Windows thay vì JFileChooser
            java.awt.FileDialog dialog = new java.awt.FileDialog((java.awt.Frame) null,
                    "Mẹo: Hãy chọn 1 bài hát bất kỳ trong thư mục bạn muốn nạp",
                    java.awt.FileDialog.LOAD);

            dialog.setFile("*.mp3;*.wav"); // Chỉ hiển thị các file nhạc
            dialog.setVisible(true); // Hiển thị cửa sổ

            // Tự động suy ra thư mục gốc chứa bài hát bạn vừa chọn
            String dir = dialog.getDirectory();
            if (dir != null) {
                musicPlayer.setMusicFolder(new java.io.File(dir));
            }
        });

        // Nhét tất cả vào Menu
        swingPopup.addSeparator();
        swingPopup.add(trackNameItem);
        swingPopup.add(playItem);
        swingPopup.add(nextItem);
        swingPopup.add(changeFolderItem); // 👉 NHỚ ADD THÊM NÚT NÀY VÀO MENU

        // ... (Phần code cũ) JMenuItem throwItem = new JMenuItem("Ném hành (Throw
        // Leek)");
        JMenuItem throwItem = new JMenuItem("Ném hành (Throw Leek)");
        throwItem.addActionListener(e -> miku.setState(CharacterState.THROWING));
        JMenuItem exitItem = new JMenuItem("Thoát (Dismiss)");
        exitItem.addActionListener(e -> System.exit(0));

        swingPopup.add(titleItem);
        swingPopup.add(casualItem);
        swingPopup.add(gamingItem);
        swingPopup.add(workingItem);
        swingPopup.addSeparator();
        swingPopup.add(throwItem);
        swingPopup.addSeparator();
        swingPopup.add(exitItem);

        JDialog hiddenDialog = new JDialog();
        hiddenDialog.setUndecorated(true);
        hiddenDialog.setSize(0, 0);
        hiddenDialog.setType(java.awt.Window.Type.UTILITY);
        hiddenDialog.setFocusableWindowState(false);
        hiddenDialog.setAlwaysOnTop(true);

        Timer autoCloseTimer = new Timer(300, e -> {
            if (!swingPopup.isVisible()) {
                ((Timer) e.getSource()).stop();
                return;
            }
            if (swingPopup.isShowing()) {
                Point mouse = MouseInfo.getPointerInfo().getLocation();
                Point pLoc = swingPopup.getLocationOnScreen();
                Dimension pSize = swingPopup.getSize();
                Rectangle safeArea = new Rectangle(pLoc.x - 15, pLoc.y - 15, pSize.width + 30, pSize.height + 30);
                if (!safeArea.contains(mouse)) {
                    swingPopup.setVisible(false);
                    hiddenDialog.setVisible(false);
                    ((Timer) e.getSource()).stop();
                }
            }
        });

        swingPopup.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                // 👉 FIX LỖI MENU KHÔNG CHỊU TẮT: Kích hoạt cảm biến chuột
                autoCloseTimer.start();
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                hiddenDialog.setVisible(false);
            }

            // ... (Các phần dưới giữ nguyên)
            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                hiddenDialog.setVisible(false);
            }
        });

        TrayIcon trayIcon = new TrayIcon(iconImage, "Miku Shimeji");
        trayIcon.setImageAutoSize(true);
        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    Point mousePos = MouseInfo.getPointerInfo().getLocation();
                    hiddenDialog.setLocation(mousePos.x, mousePos.y);
                    hiddenDialog.setVisible(true);
                    swingPopup.show(hiddenDialog, 0, 0);
                    autoCloseTimer.start();
                }
            }
        });

        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            System.out.println("Lỗi: " + e.getMessage());
        }

        // 👉 CẢM BIẾN TỰ ĐỘNG CẬP NHẬT TÊN BÀI HÁT KHI MỞ MENU LÊN
        // 👉 CẢM BIẾN TỰ ĐỘNG CẬP NHẬT TÊN BÀI HÁT KHI MỞ MENU LÊN
        swingPopup.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
                // Cập nhật lại danh sách bài hát phòng khi có file mp3 mới
                musicPlayer.scanMusic();

                trackNameItem.setText("♪ " + musicPlayer.getCurrentTrackName());

                // 👉 THÊM DÒNG NÀY: Cập nhật tên Thư mục trực tiếp lên nút Chọn
                changeFolderItem.setText("📂 Chọn thư mục (Đang mở: " + musicPlayer.getMusicFolderName() + ")");
            }

            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
            }

            // ...
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {
            }
        });

        // window.setSharedMenu(swingPopup); // (Đây là dòng cũ bạn giữ nguyên)
        // 👉 TRUYỀN MENU XỊN TỪ TASKBAR SANG CHO MIKU DÙNG CHUNG
        window.setSharedMenu(swingPopup);
    }
}