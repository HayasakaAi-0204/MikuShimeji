// File: MikuMenu.java
package com.shimeji.miku;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class MikuMenu {
    // Các thuộc tính (Properties)
    private MikuCharacter miku;
    private MikuWindow window;
    private MusicPlayer musicPlayer;

    // Hàm khởi tạo (Constructor) - Truyền vào các đối tượng cần thiết để Menu tương
    // tác
    public MikuMenu(MikuCharacter miku, MikuWindow window, MusicPlayer musicPlayer) {
        this.miku = miku;
        this.window = window;
        this.musicPlayer = musicPlayer;

        initSystemTray(); // Gọi hàm xây dựng menu ngay khi khởi tạo
    }

    // 👉 HÀM TIỆN ÍCH OOP (PHIÊN BẢN TỐI THƯỢNG - CHỐNG ĐÓNG MỌI THỂ LOẠI NÚT)
    private void keepMenuOpen(JComponent... components) {
        for (JComponent comp : components) {
            comp.putClientProperty("doNotCancelPopup", true); // Thần chú 1: Dành cho JSlider, Java cũ
            comp.putClientProperty("CheckBoxMenuItem.doNotCloseOnMouseClick", true); // Thần chú 2: Dành cho Checkbox
                                                                                     // (Casual, Gaming...)
            comp.putClientProperty("MenuItem.doNotCloseOnMouseClick", true); // Thần chú 3: Dành cho Nút thường (Dự
                                                                             // phòng)
        }
    }

    // Phương thức ẩn chứa toàn bộ logic tạo Menu
    private void initSystemTray() {
        if (!SystemTray.isSupported())
            return;

        SystemTray tray = SystemTray.getSystemTray();
        URL iconUrl = getClass().getResource("/images/miku_icon.png");
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
            SwingUtilities.invokeLater(() -> swingPopup.repaint()); // 👉 Đợi xử lý click xong rồi ép vẽ lại!
        });

        gamingItem.addActionListener(e -> {
            casualItem.setSelected(false);
            workingItem.setSelected(false);
            gamingItem.setSelected(true);
            miku.setAppMode(MikuCharacter.AppMode.GAMING);
            SwingUtilities.invokeLater(() -> swingPopup.repaint()); // 👉 Đợi xử lý click xong rồi ép vẽ lại!
        });

        workingItem.addActionListener(e -> {
            casualItem.setSelected(false);
            gamingItem.setSelected(false);
            workingItem.setSelected(true);
            miku.setAppMode(MikuCharacter.AppMode.WORKING);
            SwingUtilities.invokeLater(() -> swingPopup.repaint()); // 👉 Đợi xử lý click xong rồi ép vẽ lại!
        });

        JMenuItem trackNameItem = new JMenuItem("🎵 Nhạc: Ngừng phát");
        trackNameItem.setEnabled(false);

        JPanel volumePanel = new JPanel(new BorderLayout());
        volumePanel.setOpaque(false);
        JLabel volLabel = new JLabel(" 🔊 ");
        volLabel.setForeground(Color.WHITE);
        JSlider volSlider = new JSlider(0, 100, 50);
        volSlider.setOpaque(false);
        volumePanel.add(volLabel, BorderLayout.WEST);
        volumePanel.add(volSlider, BorderLayout.CENTER);

        volSlider.addChangeListener(e -> {
            musicPlayer.setVolume(volSlider.getValue() / 100.0);
        });

        JPanel progressPanel = new JPanel(new BorderLayout());
        progressPanel.setOpaque(false);
        JLabel progLabel = new JLabel(" ⏳ ");
        progLabel.setForeground(Color.WHITE);
        JSlider progSlider = new JSlider(0, 100, 0);
        progSlider.setOpaque(false);
        progressPanel.add(progLabel, BorderLayout.WEST);
        progressPanel.add(progSlider, BorderLayout.CENTER);

        progSlider.addChangeListener(e -> {
            if (progSlider.getValueIsAdjusting()) {
                double total = musicPlayer.getTotalDurationSeconds();
                musicPlayer.seek((progSlider.getValue() / 100.0) * total);
            }
        });

        // 👉 BẮT BUỘC ĐỔI SANG JCheckBoxMenuItem THÌ BÙA CHÚ MỚI LINH NGHIỆM!
        JCheckBoxMenuItem playItem = new JCheckBoxMenuItem("▶ Phát / Tạm dừng");
        JCheckBoxMenuItem nextItem = new JCheckBoxMenuItem("⏭ Chuyển bài kế tiếp");

        // Nút chọn thư mục thì cứ để là JMenuItem thường vì bấm xong là phải đóng để
        // hiện cửa sổ
        JMenuItem changeFolderItem = new JMenuItem("📂 Chọn thư mục nhạc...");

        playItem.addActionListener(e -> {
            musicPlayer.togglePlayPause();
            playItem.setSelected(false);
            SwingUtilities.invokeLater(() -> swingPopup.repaint());
        });

        nextItem.addActionListener(e -> {
            musicPlayer.next();
            nextItem.setSelected(false);
            SwingUtilities.invokeLater(() -> swingPopup.repaint());
        });

        changeFolderItem.addActionListener(e -> {
            FileDialog dialog = new FileDialog((Frame) null,
                    "Mẹo: Hãy chọn 1 bài hát bất kỳ trong thư mục bạn muốn nạp", FileDialog.LOAD);
            dialog.setFile("*.mp3;*.wav");
            dialog.setVisible(true);

            String dir = dialog.getDirectory();
            if (dir != null) {
                musicPlayer.setMusicFolder(new java.io.File(dir));
            }
        });

        JMenuItem throwItem = new JMenuItem("Ném hành (Throw Leek)");
        throwItem.addActionListener(e -> miku.setState(CharacterState.THROWING));
        JMenuItem exitItem = new JMenuItem("Thoát (Dismiss)");
        exitItem.addActionListener(e -> System.exit(0));

        /*
         * =============================================================================
         * =
         * 📌 DANH SÁCH TÊN CÁC NÚT (CHEAT SHEET) ĐỂ BẠN DỄ QUẢN LÝ VIỆC ĐÓNG/MỞ MENU:
         * (Mẹo: Muốn nút nào KHÔNG ĐÓNG menu khi click, hãy điền tên nó vào hàm
         * keepMenuOpen)
         * 
         * --- [Phần chọn Chế Độ] ---
         * casualItem : Nút chọn Casual Mode
         * gamingItem : Nút chọn Gaming Mode
         * workingItem : Nút chọn Working Mode
         * 
         * --- [Phần Âm Nhạc] ---
         * volSlider : Thanh trượt âm lượng
         * progSlider : Thanh trượt tiến độ (Tua bài hát)
         * playItem : Nút Phát / Tạm dừng nhạc
         * nextItem : Nút Chuyển bài kế tiếp
         * changeFolderItem : Nút Chọn thư mục nhạc (Gợi ý: Không nên cho vào vì nó cần
         * mở cửa sổ mới)
         * 
         * --- [Phần Hành Động] ---
         * throwItem : Nút Ném hành (Throw Leek)
         * exitItem : Nút Thoát app (Chắc chắn không nên cho vào, ấn thoát là phải đóng
         * luôn)
         * =============================================================================
         * =
         */

        // 👉 DÙNG HÀM TIỆN ÍCH OOP MÀ CHÚNG TA VỪA LÀM
        keepMenuOpen(playItem, nextItem, casualItem, gamingItem, workingItem, volSlider, progSlider);

        swingPopup.addSeparator();
        swingPopup.add(trackNameItem);
        swingPopup.add(progressPanel);
        swingPopup.add(volumePanel);
        swingPopup.add(playItem);
        swingPopup.add(nextItem);
        swingPopup.add(changeFolderItem);

        swingPopup.addSeparator();
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
        hiddenDialog.setType(Window.Type.UTILITY);
        hiddenDialog.setFocusableWindowState(false);
        hiddenDialog.setAlwaysOnTop(true);

        final Point[] startMouse = { new Point(0, 0) };
        final long[] outOfBoundsTime = { 0 };

        Timer autoCloseTimer = new Timer(300, e -> {
            if (!swingPopup.isVisible()) {
                ((Timer) e.getSource()).stop();
                return;
            }
            if (swingPopup.isShowing()) {
                Point mouse = MouseInfo.getPointerInfo().getLocation();
                Point pLoc = swingPopup.getLocationOnScreen();
                Dimension pSize = swingPopup.getSize();
                Rectangle safeArea = new Rectangle(pLoc.x - 100, pLoc.y - 100, pSize.width + 200, pSize.height + 200);

                int moveDist = Math.abs(mouse.x - startMouse[0].x) + Math.abs(mouse.y - startMouse[0].y);

                if (!safeArea.contains(mouse) && moveDist > 100) {
                    if (outOfBoundsTime[0] == 0) {
                        outOfBoundsTime[0] = System.currentTimeMillis();
                    } else if (System.currentTimeMillis() - outOfBoundsTime[0] >= 2000) {
                        swingPopup.setVisible(false);
                        hiddenDialog.setVisible(false);
                        ((Timer) e.getSource()).stop();
                    }
                } else {
                    outOfBoundsTime[0] = 0;
                }
            }
        });

        swingPopup.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
                startMouse[0] = MouseInfo.getPointerInfo().getLocation();
                outOfBoundsTime[0] = 0;
                autoCloseTimer.start();

                musicPlayer.scanMusic();
                trackNameItem.setText("♪ " + musicPlayer.getCurrentTrackName());
                changeFolderItem.setText("📂 Chọn thư mục (Đang mở: " + musicPlayer.getMusicFolderName() + ")");
            }

            @Override
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
                hiddenDialog.setVisible(false);
            }

            @Override
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {
                hiddenDialog.setVisible(false);
            }
        });

        TrayIcon trayIcon = new TrayIcon(iconImage, "Miku Shimeji");
        trayIcon.setImageAutoSize(true);
        trayIcon.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
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

        musicPlayer.setOnProgressUpdate(() -> {
            SwingUtilities.invokeLater(() -> {
                if (!progSlider.getValueIsAdjusting()) {
                    double current = musicPlayer.getCurrentTimeSeconds();
                    double total = musicPlayer.getTotalDurationSeconds();
                    if (total > 0) {
                        progSlider.setValue((int) ((current / total) * 100));
                    }
                }
            });
        });

        musicPlayer.setOnTrackChange(() -> {
            SwingUtilities.invokeLater(() -> {
                trackNameItem.setText("♪ " + musicPlayer.getCurrentTrackName());
                progSlider.setValue(0);
            });
        });

        window.setSharedMenu(swingPopup);
    }
}