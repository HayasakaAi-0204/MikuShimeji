package com.shimeji.miku;

import java.awt.*;
import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;
import javax.swing.*;

public class MikuMenu {
    private MikuCharacter miku;
    private MikuWindow window;
    private MusicPlayer musicPlayer;

    public MikuMenu(MikuCharacter miku, MikuWindow window, MusicPlayer musicPlayer) {
        this.miku = miku;
        this.window = window;
        this.musicPlayer = musicPlayer;
        initSystemTray();
    }

    public static void keepMenuOpen(JComponent... components) {
        for (JComponent comp : components) {
            comp.putClientProperty("doNotCancelPopup", true);
            comp.putClientProperty("CheckBoxMenuItem.doNotCloseOnMouseClick", true);
            comp.putClientProperty("MenuItem.doNotCloseOnMouseClick", true);
        }
    }

    private void initSystemTray() {
        if (!SystemTray.isSupported())
            return;

        SystemTray tray = SystemTray.getSystemTray();
        URL iconUrl = getClass().getResource("/images/miku_icon.png");
        Image iconImage = Toolkit.getDefaultToolkit().getImage(iconUrl);
        window.setIconImage(iconImage);

        JPopupMenu swingPopup = new JPopupMenu();

        Preferences prefs = Preferences.userNodeForPackage(MikuMenu.class);
        String savedLang = prefs.get("mikuLanguage", "vi");
        ResourceBundle lang = ResourceBundle.getBundle("lang.messages", Locale.of(savedLang));

        // 👉 LẮP GHÉP CÁC MENU CON ĐỘC LẬP
        MusicMenu musicMenu = new MusicMenu(musicPlayer, lang, prefs, swingPopup);
        ModeMenu modeMenu = new ModeMenu(miku, lang, swingPopup);
        SettingsMenu settingsMenu = new SettingsMenu(miku, musicPlayer, lang, prefs, swingPopup);

        JMenu actionMenu = new JMenu(lang.getString("menu.action"));
        JMenuItem throwItem = new JMenuItem(lang.getString("menu.throw"));
        throwItem.addActionListener(e -> miku.setState(CharacterState.THROWING));
        actionMenu.add(throwItem);

        JMenuItem exitItem = new JMenuItem(lang.getString("menu.exit"));
        exitItem.addActionListener(e -> {
            musicPlayer.saveState();
            System.exit(0);
        });

        swingPopup.add(musicMenu);
        swingPopup.add(modeMenu);
        swingPopup.add(actionMenu);
        swingPopup.add(settingsMenu);
        swingPopup.addSeparator();
        swingPopup.add(exitItem);

        // Timer tự đóng menu khi rê chuột ra ngoài
        final Point[] startMouse = { new Point(0, 0) };
        final long[] outOfBoundsTime = { 0 };

        Timer autoCloseTimer = new Timer(300, e -> {
            if (!swingPopup.isVisible()) {
                ((Timer) e.getSource()).stop();
                return;
            }
            if (Boolean.TRUE.equals(swingPopup.getClientProperty("isEditingTime"))) {
                outOfBoundsTime[0] = 0;
                return;
            }
            if (swingPopup.isShowing()) {
                Point mouse = MouseInfo.getPointerInfo().getLocation();
                Point pLoc = swingPopup.getLocationOnScreen();
                Dimension pSize = swingPopup.getSize();
                Rectangle safeArea = new Rectangle(pLoc.x - 100, pLoc.y - 100, pSize.width + 200, pSize.height + 200);

                for (MenuElement el : MenuSelectionManager.defaultManager().getSelectedPath()) {
                    Component c = el.getComponent();
                    if (c != null && c.isShowing()) {
                        Rectangle subRect = new Rectangle(c.getLocationOnScreen(), c.getSize());
                        subRect.grow(100, 100);
                        safeArea = safeArea.union(subRect);
                    }
                }

                int moveDist = Math.abs(mouse.x - startMouse[0].x) + Math.abs(mouse.y - startMouse[0].y);
                if (!safeArea.contains(mouse) && moveDist > 100) {
                    if (outOfBoundsTime[0] == 0) {
                        outOfBoundsTime[0] = System.currentTimeMillis();
                    } else if (System.currentTimeMillis() - outOfBoundsTime[0] >= 1000) {
                        swingPopup.setVisible(false);
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
            }

            @Override
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {
            }
        });

        TrayIcon trayIcon = new TrayIcon(iconImage, "Miku Shimeji");
        trayIcon.setImageAutoSize(true);
        trayIcon.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    Point mousePos = MouseInfo.getPointerInfo().getLocation();
                    Point winLoc = window.getLocationOnScreen();
                    // Dùng window làm invoker, tính tọa độ tương đối
                    swingPopup.show(window, mousePos.x - winLoc.x, mousePos.y - winLoc.y);
                    autoCloseTimer.start();
                }
            }
        });
        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            System.out.println("Lỗi: " + e.getMessage());
        }

        window.setSharedMenu(swingPopup);
    }
}