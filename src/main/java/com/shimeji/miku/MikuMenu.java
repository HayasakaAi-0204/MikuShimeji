package com.shimeji.miku;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.Window;
import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

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

        JDialog hiddenDialog = new JDialog();
        hiddenDialog.setUndecorated(true);
        hiddenDialog.setSize(0, 0);
        hiddenDialog.setType(Window.Type.UTILITY);
        hiddenDialog.setFocusableWindowState(true);
        hiddenDialog.setAlwaysOnTop(true);

        // 👉 ĐỌC NGÔN NGỮ TỪ HỆ THỐNG VÀ NẠP FILE TƯƠNG ỨNG
        Preferences prefs = Preferences.userNodeForPackage(MikuMenu.class);
        String savedLang = prefs.get("mikuLanguage", "vi");
        ResourceBundle lang = ResourceBundle.getBundle("lang.messages", Locale.of(savedLang));

        JCheckBoxMenuItem casualItem = new JCheckBoxMenuItem(lang.getString("menu.casual"), true);
        JCheckBoxMenuItem gamingItem = new JCheckBoxMenuItem(lang.getString("menu.gaming"));
        JCheckBoxMenuItem workingItem = new JCheckBoxMenuItem(lang.getString("menu.working"));

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

        JMenuItem trackNameItem = new JMenuItem(lang.getString("menu.track.stop"));

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
            trackNameItem.setText(lang.getString("menu.track.loading"));
            progSlider.setValue(0);
            progValueLabel.setText("00:00 : 00:00 ");
            musicPlayer.next();
            btnPlay.setText("⏸"); // Chuyển bài thì chắc chắn là sẽ auto phát
            SwingUtilities.invokeLater(() -> swingPopup.repaint());
        });

        // Chức năng Chuyển bài Trước
        btnPrev.addActionListener(e -> {
            trackNameItem.setText(lang.getString("menu.track.loading"));
            progSlider.setValue(0);
            progValueLabel.setText("00:00 : 00:00 ");
            musicPlayer.previous();
            btnPlay.setText("⏸"); // Chuyển bài thì chắc chắn là sẽ auto phát
            SwingUtilities.invokeLater(() -> swingPopup.repaint());
        });

        // Nút chọn thư mục thì cứ để là JMenuItem thường vì bấm xong là phải đóng để
        // hiện cửa sổ
        JMenuItem changeFolderItem = new JMenuItem(lang.getString("menu.folder"));

        changeFolderItem.addActionListener(e -> {
            FileDialog dialog = new FileDialog((Frame) null,
                    lang.getString("menu.folder.tip"), FileDialog.LOAD);
            dialog.setFile("*.mp3;*.wav");
            dialog.setVisible(true);

            String dir = dialog.getDirectory();
            if (dir != null) {
                musicPlayer.setMusicFolder(new java.io.File(dir));
            }
            btnPlay.setText("⏸");
        });

        JMenuItem throwItem = new JMenuItem(lang.getString("menu.throw"));
        throwItem.addActionListener(e -> miku.setState(CharacterState.THROWING));
        JMenuItem exitItem = new JMenuItem(lang.getString("menu.exit"));

        exitItem.addActionListener(e -> {
            musicPlayer.saveState(); // Ép lưu trước khi tắt
            System.exit(0);
        });

        // ==========================================
        // KHU VỰC THIẾT LẬP (SETTINGS)
        // ==========================================
        // Đọc ngôn ngữ đã lưu trong máy (mặc định là 'vi' - Tiếng Việt)

        JMenu settingsMenu = new JMenu(lang.getString("menu.settings"));
        JMenu languageMenu = new JMenu(lang.getString("menu.language"));

        JCheckBoxMenuItem langViItem = new JCheckBoxMenuItem("Tiếng Việt", savedLang.equals("vi"));
        JCheckBoxMenuItem langEnItem = new JCheckBoxMenuItem("English", savedLang.equals("en"));

        // Chức năng khi bấm chọn Tiếng Việt
        langViItem.addActionListener(e -> {
            langViItem.setSelected(true);
            langEnItem.setSelected(false);
            prefs.put("mikuLanguage", "vi");
            restartApp(); // 👉 Gọi hàm tự khởi động lại
        });

        // Chức năng khi bấm chọn Tiếng Anh
        langEnItem.addActionListener(e -> {
            langEnItem.setSelected(true);
            langViItem.setSelected(false);
            prefs.put("mikuLanguage", "en");
            restartApp(); // 👉 Gọi hàm tự khởi động lại
        });

        // Lắp 2 nút vào Menu Ngôn ngữ
        languageMenu.add(langViItem);
        languageMenu.add(langEnItem);

        // Lắp Menu Ngôn ngữ vào Menu Thiết lập
        settingsMenu.add(languageMenu);

        // ==========================================
        // TÍNH NĂNG ĐẶT THỜI GIAN LÀM VIỆC & NGHỈ NGƠI (DÙNG HỘP THOẠI CHUẨN CỦA JAVA)
        // ==========================================

        int savedWorkTime = prefs.getInt("mikuWorkTime", 3600);
        int savedBreakTime = prefs.getInt("mikuBreakTime", 60);

        // Hàm OOP tạo ô nhập chữ (Dùng hộp thoại Dialog chuẩn - Mượt mà & Không bao giờ
        // bị đè)
        java.util.function.BiFunction<String, Integer, JLabel> createClickableBox = (text, maxVal) -> {
            JLabel label = new JLabel(text, SwingConstants.CENTER);
            label.setOpaque(true);
            label.setBackground(Color.WHITE);
            label.setForeground(Color.BLACK);
            label.setPreferredSize(new Dimension(35, 22));
            label.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
            label.setCursor(new Cursor(Cursor.HAND_CURSOR)); // Đổi thành icon bàn tay cho dễ bấm

            label.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mousePressed(java.awt.event.MouseEvent e) {
                    // Đánh dấu không cho phép tự động tắt menu
                    swingPopup.putClientProperty("isEditingTime", true);

                    // Mở hộp thoại nhập liệu chuẩn của Java (Không bao giờ bị đè, luôn nổi trên
                    // cùng)
                    String input = javax.swing.JOptionPane.showInputDialog(
                            hiddenDialog,
                            "Nhập thời gian:",
                            label.getText());

                    // Nếu người dùng ấn OK và có nhập số
                    if (input != null && !input.trim().isEmpty()) {
                        try {
                            int val = Integer.parseInt(input.trim());
                            if (val >= 0 && (maxVal == 0 || val <= maxVal)) {
                                label.setText(maxVal > 0 ? String.format("%02d", val) : String.valueOf(val));
                                label.firePropertyChange("timeChanged", 0, 1);
                            }
                        } catch (Exception ex) {
                            // Nhập bậy bạ (chữ cái) thì bỏ qua
                        }
                    }

                    // Nhả đánh dấu để menu hoạt động bình thường lại
                    swingPopup.putClientProperty("isEditingTime", false);
                }
            });

            return label;
        };

        // --- Panel Thời Gian Làm Việc ---
        JPanel workTimePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        workTimePanel.setBackground(swingPopup.getBackground());

        JLabel workLabel = new JLabel(lang.getString("menu.worktime") + ":");
        workLabel.setForeground(Color.WHITE);
        workLabel.setPreferredSize(new Dimension(110, 22));

        JLabel workMinBox = createClickableBox.apply(String.valueOf(savedWorkTime / 60), 0);
        JLabel colon1 = new JLabel(":");
        colon1.setForeground(Color.WHITE);
        JLabel workSecBox = createClickableBox.apply(String.format("%02d", savedWorkTime % 60), 59);

        workTimePanel.add(workLabel);
        workTimePanel.add(workMinBox);
        workTimePanel.add(colon1);
        workTimePanel.add(workSecBox);

        // --- Panel Thời Gian Nghỉ ---
        JPanel breakTimePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        breakTimePanel.setBackground(swingPopup.getBackground());

        JLabel breakLabel = new JLabel(lang.getString("menu.breaktime") + ":");
        breakLabel.setForeground(Color.WHITE);
        breakLabel.setPreferredSize(new Dimension(110, 22));

        JLabel breakMinBox = createClickableBox.apply(String.valueOf(savedBreakTime / 60), 0);
        JLabel colon2 = new JLabel(":");
        colon2.setForeground(Color.WHITE);
        JLabel breakSecBox = createClickableBox.apply(String.format("%02d", savedBreakTime % 60), 59);

        breakTimePanel.add(breakLabel);
        breakTimePanel.add(breakMinBox);
        breakTimePanel.add(colon2);
        breakTimePanel.add(breakSecBox);

        // 👉 Hàm tự động Lưu thời gian mỗi khi bạn nhập số xong
        java.beans.PropertyChangeListener autoSaver = evt -> {
            int wm = Integer.parseInt(workMinBox.getText());
            int ws = Integer.parseInt(workSecBox.getText());
            int totalWork = wm * 60 + ws;
            prefs.putInt("mikuWorkTime", totalWork);

            int bm = Integer.parseInt(breakMinBox.getText());
            int bs = Integer.parseInt(breakSecBox.getText());
            int totalBreak = bm * 60 + bs;
            prefs.putInt("mikuBreakTime", totalBreak);

            // 👉 DÒNG QUAN TRỌNG NHẤT BỊ THIẾU: Báo tin cho Miku để reset đồng hồ ngay lập
            // tức!
            miku.updateTimers(totalWork, totalBreak);
        };

        workMinBox.addPropertyChangeListener("timeChanged", autoSaver);
        workSecBox.addPropertyChangeListener("timeChanged", autoSaver);
        breakMinBox.addPropertyChangeListener("timeChanged", autoSaver);
        breakSecBox.addPropertyChangeListener("timeChanged", autoSaver);

        // Thần chú giữ menu không bị đóng khi click vào ô chim mồi
        keepMenuOpen(workMinBox, workSecBox, breakMinBox, breakSecBox);

        settingsMenu.addSeparator();
        settingsMenu.add(workTimePanel);
        settingsMenu.add(breakTimePanel);

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
        keepMenuOpen(volSlider, progSlider, btnShuffle, btnPrev, btnPlay, btnNext, btnRepeat);

        // ==========================================
        // 1. TẠO CÁC DANH MỤC MẸ
        // ==========================================
        JMenu musicMenu = new JMenu(lang.getString("menu.music"));
        JMenu modeMenu = new JMenu(lang.getString("menu.mode"));
        JMenu actionMenu = new JMenu(lang.getString("menu.action"));

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
        swingPopup.add(settingsMenu);

        swingPopup.addSeparator(); // Đường kẻ ngang cuối cùng
        swingPopup.add(exitItem); // Nút Thoát

        final Point[] startMouse = { new Point(0, 0) };
        final long[] outOfBoundsTime = { 0 };

        Timer autoCloseTimer = new Timer(300, e -> {
            if (!swingPopup.isVisible()) {
                ((Timer) e.getSource()).stop();
                return;
            }

            // 👉 THÊM 4 DÒNG NÀY ĐỂ BẢO VỆ MENU KHÔNG BỊ TẮT KHI ĐANG GÕ SỐ:
            if (Boolean.TRUE.equals(swingPopup.getClientProperty("isEditingTime"))) {
                outOfBoundsTime[0] = 0; // Reset bộ đếm về 0 liên tục
                return; // Ngưng chạy phần tắt menu ở bên dưới
            }

            if (swingPopup.isShowing()) {
                Point mouse = MouseInfo.getPointerInfo().getLocation();
                // ... (Các code cũ ở bên dưới giữ nguyên)
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
                changeFolderItem.setText(
                        String.format(lang.getString("menu.folder.current"), musicPlayer.getMusicFolderName()));
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

    // 👇 HÀM TỰ ĐỘNG KHỞI ĐỘNG LẠI APP 👇
    private void restartApp() {
        musicPlayer.saveState(); // Lưu lại bài hát đang nghe trước khi tắt
        try {
            String javaCmd = System.getProperty("java.home") + java.io.File.separator + "bin" + java.io.File.separator
                    + "java";
            java.io.File currentFile = new java.io.File(
                    MikuMenu.class.getProtectionDomain().getCodeSource().getLocation().toURI());

            // Tự nhận diện đang chạy code trong IDE hay đang chạy file .jar / .exe
            if (currentFile.getName().endsWith(".jar")) {
                new ProcessBuilder(javaCmd, "-jar", currentFile.getPath()).start();
            } else {
                new ProcessBuilder(javaCmd, "-cp", System.getProperty("java.class.path"), "com.shimeji.miku.App")
                        .start();
            }
        } catch (Exception ex) {
            System.out.println("Lỗi khởi động lại: " + ex.getMessage());
        }
        System.exit(0); // Tắt app ngay lập tức
    }
}