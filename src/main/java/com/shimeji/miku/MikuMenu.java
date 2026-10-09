package com.shimeji.miku;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.prefs.Preferences;

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

        JCheckBoxMenuItem casualItem = new JCheckBoxMenuItem("Chế độ Thư giãn", true);
        JCheckBoxMenuItem gamingItem = new JCheckBoxMenuItem("Chế độ Tập Trung");
        JCheckBoxMenuItem workingItem = new JCheckBoxMenuItem("Chế độ Làm việc");

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

        // ==========================================
        // KHU VỰC ÂM LƯỢNG (ĐÃ CĂN CHỈNH THẲNG HÀNG)
        // ==========================================
        Color menuBg = swingPopup.getBackground(); // 👉 Lấy màu nền chuẩn của Menu

        JPanel volumePanel = new JPanel(new BorderLayout());
        volumePanel.setBackground(menuBg); // 👉 Khung chứa đóng vai trò làm "cục tẩy" xóa bóng ma

        JLabel volLabel = new JLabel(" 🔊 ");
        volLabel.setForeground(Color.WHITE);
        volLabel.setPreferredSize(new Dimension(35, 20));

        Preferences prefs = Preferences.userNodeForPackage(MikuMenu.class);
        int savedVolume = prefs.getInt("mikuVolume", 50);

        JSlider volSlider = new JSlider(0, 100, savedVolume);
        volSlider.setOpaque(false); // 👉 TRẢ LẠI TRONG SUỐT CHO THANH TRƯỢT ĐỂ HIỆN THANH NGANG!
        musicPlayer.setVolume(savedVolume / 100.0);

        JLabel volValueLabel = new JLabel(savedVolume + "% ");
        volValueLabel.setForeground(Color.WHITE);
        volValueLabel.setPreferredSize(new Dimension(95, 20));
        volValueLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        volumePanel.add(volLabel, BorderLayout.WEST);
        volumePanel.add(volSlider, BorderLayout.CENTER);
        volumePanel.add(volValueLabel, BorderLayout.EAST);

        volSlider.addChangeListener(e -> {
            int vol = volSlider.getValue();
            musicPlayer.setVolume(vol / 100.0);
            volValueLabel.setText(vol + "% ");
            if (!volSlider.getValueIsAdjusting()) {
                prefs.putInt("mikuVolume", vol);
            }
        });

        // ==========================================
        // KHU VỰC TIẾN ĐỘ NHẠC (ĐÃ CĂN CHỈNH THẲNG HÀNG)
        // ==========================================
        JPanel progressPanel = new JPanel(new BorderLayout());
        progressPanel.setBackground(menuBg); // 👉 Khung chứa đóng vai trò làm "cục tẩy" xóa bóng ma

        JLabel progLabel = new JLabel(" ⏳ ");
        progLabel.setForeground(Color.WHITE);
        progLabel.setPreferredSize(new Dimension(35, 20));

        JSlider progSlider = new JSlider(0, 100, 0);
        progSlider.setOpaque(false); // 👉 TRẢ LẠI TRONG SUỐT CHO THANH TRƯỢT ĐỂ HIỆN THANH NGANG!

        JLabel progValueLabel = new JLabel("00:00 : 00:00 ");
        progValueLabel.setForeground(Color.WHITE);
        progValueLabel.setPreferredSize(new Dimension(95, 20));
        progValueLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        progressPanel.add(progLabel, BorderLayout.WEST);
        progressPanel.add(progSlider, BorderLayout.CENTER);
        progressPanel.add(progValueLabel, BorderLayout.EAST);

        progSlider.addChangeListener(e -> {
            if (progSlider.getValueIsAdjusting()) {
                double total = musicPlayer.getTotalDurationSeconds();
                double current = (progSlider.getValue() / 100.0) * total;
                musicPlayer.seek(current);

                progValueLabel.setText(String.format("%02d:%02d : %02d:%02d ",
                        (int) current / 60, (int) current % 60,
                        (int) total / 60, (int) total % 60));
            }
        });

        // ==========================================
        // KHU VỰC THANH ĐIỀU KHIỂN MEDIA (NẰM NGANG)
        // ==========================================
        JPanel mediaControlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        mediaControlPanel.setBackground(menuBg); // Dùng lại màu nền cho tiệp màu

        JButton btnShuffle = new JButton("🔀");
        JButton btnPrev = new JButton("⏮");
        JButton btnPlay = new JButton(musicPlayer.isPlaying() ? "⏸" : "▶");
        JButton btnNext = new JButton("⏭");
        JButton btnRepeat = new JButton("🔁");

        // Màu sắc: Nút nào bật thì sáng màu Xanh, tắt thì màu Xám
        Color colorOn = new Color(0, 150, 255);
        Color colorOff = Color.GRAY;

        // Cài đặt giao diện cho cả 5 nút cùng lúc (xóa viền, làm nền trong suốt...)
        JButton[] mediaBtns = { btnShuffle, btnPrev, btnPlay, btnNext, btnRepeat };
        for (JButton btn : mediaBtns) {
            btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
            btn.setFocusPainted(false);
            btn.setContentAreaFilled(false);
            btn.setBorderPainted(false);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.setMargin(new Insets(0, 0, 0, 0));
            mediaControlPanel.add(btn); // Nhét vào thanh ngang
        }

        // Cài đặt màu ban đầu dựa vào việc bạn có đang bật Trộn/Lặp không
        btnShuffle.setForeground(musicPlayer.isShuffle() ? colorOn : colorOff);
        btnRepeat.setForeground(musicPlayer.isRepeatOne() ? colorOn : colorOff);
        btnPrev.setForeground(Color.WHITE);
        btnNext.setForeground(Color.WHITE);
        btnPlay.setForeground(Color.WHITE);

        // Chức năng khi bấm nút Trộn
        btnShuffle.addActionListener(e -> {
            boolean newState = !musicPlayer.isShuffle();
            musicPlayer.setShuffle(newState);
            btnShuffle.setForeground(newState ? colorOn : colorOff); // Đổi màu
            SwingUtilities.invokeLater(() -> swingPopup.repaint());
        });

        // Chức năng khi bấm nút Lặp
        btnRepeat.addActionListener(e -> {
            boolean newState = !musicPlayer.isRepeatOne();
            musicPlayer.setRepeatOne(newState);
            btnRepeat.setForeground(newState ? colorOn : colorOff); // Đổi màu
            SwingUtilities.invokeLater(() -> swingPopup.repaint());
        });

        // Chức năng Phát / Dừng
        btnPlay.addActionListener(e -> {
            musicPlayer.togglePlayPause();
            btnPlay.setText(musicPlayer.isPlaying() ? "⏸" : "▶"); // Đổi icon
            SwingUtilities.invokeLater(() -> swingPopup.repaint());
        });

        // Chức năng Chuyển bài Kế tiếp
        btnNext.addActionListener(e -> {
            trackNameItem.setText("♪ Đang nạp nhạc...");
            progSlider.setValue(0);
            progValueLabel.setText("00:00 : 00:00 ");
            musicPlayer.next();
            btnPlay.setText("⏸"); // Chuyển bài thì chắc chắn là sẽ auto phát
            SwingUtilities.invokeLater(() -> swingPopup.repaint());
        });

        // Chức năng Chuyển bài Trước
        btnPrev.addActionListener(e -> {
            trackNameItem.setText("♪ Đang nạp nhạc...");
            progSlider.setValue(0);
            progValueLabel.setText("00:00 : 00:00 ");
            musicPlayer.previous();
            btnPlay.setText("⏸"); // Chuyển bài thì chắc chắn là sẽ auto phát
            SwingUtilities.invokeLater(() -> swingPopup.repaint());
        });

        // Nút chọn thư mục thì cứ để là JMenuItem thường vì bấm xong là phải đóng để
        // hiện cửa sổ
        JMenuItem changeFolderItem = new JMenuItem("📂 Chọn thư mục nhạc...");

        changeFolderItem.addActionListener(e -> {
            FileDialog dialog = new FileDialog((Frame) null,
                    "Mẹo: Hãy chọn 1 bài hát bất kỳ trong thư mục bạn muốn nạp", FileDialog.LOAD);
            dialog.setFile("*.mp3;*.wav");
            dialog.setVisible(true);

            String dir = dialog.getDirectory();
            if (dir != null) {
                musicPlayer.setMusicFolder(new java.io.File(dir));
            }
            btnPlay.setText("⏸");
        });

        JMenuItem throwItem = new JMenuItem("Ném hành");
        throwItem.addActionListener(e -> miku.setState(CharacterState.THROWING));
        JMenuItem exitItem = new JMenuItem("🚪 Thoát ứng dụng");
        exitItem.addActionListener(e -> {
            musicPlayer.saveState(); // Ép lưu trước khi tắt
            System.exit(0);
        });

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
         * prevItem : Nút Chuyển bài trước đấy
         * shuffleItem : Nút để bật lên thì bài hát kế tiếp là ngẫu nhiên
         * repeatItem : Nút để bật lên thì sẽ lặp lại bài hát đấy
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
        keepMenuOpen(casualItem, gamingItem, workingItem, volSlider, progSlider, btnShuffle, btnPrev, btnPlay, btnNext,
                btnRepeat);

        // ==========================================
        // 1. TẠO CÁC DANH MỤC MẸ
        // ==========================================
        JMenu musicMenu = new JMenu("🎧 Trình phát nhạc");
        JMenu modeMenu = new JMenu("⚙️ Trạng thái hoạt động");
        JMenu actionMenu = new JMenu("🏃 Tương tác");

        // ==========================================
        // 2. NHÉT CÁC NÚT VÀO TỪNG DANH MỤC
        // ==========================================

        // --- Danh mục: PHÁT NHẠC ---
        musicMenu.add(trackNameItem);
        musicMenu.add(progressPanel);
        musicMenu.add(volumePanel);
        musicMenu.addSeparator(); // Đường kẻ mờ phân cách
        musicMenu.add(mediaControlPanel);
        musicMenu.add(changeFolderItem);

        // --- Danh mục: CHẾ ĐỘ ---
        modeMenu.add(casualItem);
        modeMenu.add(gamingItem);
        modeMenu.add(workingItem);

        // --- Danh mục: HÀNH ĐỘNG ---
        actionMenu.add(throwItem); // Ném hành

        // ==========================================
        // 3. CUỐI CÙNG: GẮN 3 DANH MỤC NÀY VÀO MENU CHÍNH
        // ==========================================
        swingPopup.add(musicMenu);
        swingPopup.add(modeMenu);
        swingPopup.add(actionMenu);

        swingPopup.addSeparator(); // Đường kẻ ngang cuối cùng
        swingPopup.add(exitItem); // Nút Thoát

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
                // 1. Khởi tạo vùng an toàn cho Menu chính
                Rectangle safeArea = new Rectangle(pLoc.x - 100, pLoc.y - 100, pSize.width + 200, pSize.height + 200);

                // 2. 👉 Tự động dò tìm và cộng gộp vùng an toàn của TẤT CẢ các Menu con đang xổ
                // ra
                for (MenuElement el : MenuSelectionManager.defaultManager().getSelectedPath()) {
                    Component c = el.getComponent();
                    if (c != null && c.isShowing()) {
                        Rectangle subRect = new Rectangle(c.getLocationOnScreen(), c.getSize());
                        subRect.grow(100, 100); // Bơm thêm 100px "giáp bảo vệ" cho menu con
                        safeArea = safeArea.union(subRect); // Dung hợp vào vùng an toàn gốc!
                    }
                }

                int moveDist = Math.abs(mouse.x - startMouse[0].x) + Math.abs(mouse.y - startMouse[0].y);

                if (!safeArea.contains(mouse) && moveDist > 100) {
                    if (outOfBoundsTime[0] == 0) {
                        outOfBoundsTime[0] = System.currentTimeMillis();
                    } else if (System.currentTimeMillis() - outOfBoundsTime[0] >= 1000) {
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

                        progValueLabel.setText(String.format("%02d:%02d : %02d:%02d ",
                                (int) current / 60, (int) current % 60,
                                (int) total / 60, (int) total % 60));
                    }
                }
            });
        });

        musicPlayer.setOnTrackChange(() -> {
            SwingUtilities.invokeLater(() -> {
                trackNameItem.setText("♪ " + musicPlayer.getCurrentTrackName());

                double current = musicPlayer.getCurrentTimeSeconds();
                double total = musicPlayer.getTotalDurationSeconds();

                if (total > 0) {
                    if (current > total)
                        current = 0;

                    progSlider.setValue((int) ((current / total) * 100));

                    progValueLabel.setText(String.format("%02d:%02d : %02d:%02d ",
                            (int) current / 60, (int) current % 60,
                            (int) total / 60, (int) total % 60));
                }
            });
        });

        window.setSharedMenu(swingPopup);
    }
}