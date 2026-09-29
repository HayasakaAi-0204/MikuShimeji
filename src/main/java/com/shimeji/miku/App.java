package com.shimeji.miku;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import java.awt.Toolkit;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.Image;
import java.awt.AWTException;
import java.net.URL;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.PopupMenuEvent;

public class App {

    private static final int PHYSICS_TICK_RATE = 33;
    private static final int ANIMATION_TICK_RATE = 24;

    public static void main(String[] args) {
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);

        SwingUtilities.invokeLater(() -> {
            startShimeji();
        });
    }

    private static void startShimeji() {
        ResourceManager.loadImages();

        int screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
        MikuCharacter miku = new MikuCharacter(screenWidth / 2, 0);

        MikuWindow window = new MikuWindow(miku);
        window.setVisible(true);

        ProjectileWindow projectileWindow = new ProjectileWindow(window, miku);

        TaskbarManager taskbarManager = new TaskbarManager();
        DesktopWatcher watcher = new DesktopWatcher(miku);
        watcher.startWatching();

        setupSystemTray(miku, window);

        Timer physicsTimer = new Timer(PHYSICS_TICK_RATE, e -> {
            taskbarManager.update();

            int currentFloorY = taskbarManager.getCurrentFloorY(miku.getHeight());
            miku.updatePhysics(currentFloorY);

            // 👉 THUẬT TOÁN GAMING MODE: Tự động Teleport né chuột
            if (miku.getAppMode() == MikuCharacter.AppMode.GAMING) {
                // CHỈ TỰ ĐỘNG NÉ KHI ĐÃ BÁM TƯỜNG (KHÔNG ĐI BỘ)
                if (miku.getState() != CharacterState.WALKING_LEFT) {
                    Point mouse = MouseInfo.getPointerInfo().getLocation();
                    int dx = Math.abs(mouse.x - (miku.getX() + miku.getWidth() / 2));
                    int dy = Math.abs(mouse.y - (miku.getY() + miku.getHeight() / 2));

                    if (dx < 150 && dy < 150) {
                        if (miku.getX() < screenWidth / 2) {
                            miku.changeState(new ClimbState(false, false, true));
                        } else {
                            miku.changeState(new ClimbState(true, false, true));
                        }
                    }
                }
            }

            window.syncBounds();
            window.repaint();

            projectileWindow.syncBounds();
            if (projectileWindow.isVisible()) {
                projectileWindow.repaint();
            }
        });
        physicsTimer.start();

        Timer animationTimer = new Timer(ANIMATION_TICK_RATE, e -> {
            miku.updateAnimation();
        });
        animationTimer.start();
    }

    private static void setupSystemTray(MikuCharacter miku, MikuWindow window) {
        if (!SystemTray.isSupported()) {
            return;
        }

        SystemTray tray = SystemTray.getSystemTray();

        URL iconUrl = App.class.getResource("/images/miku_icon.png");
        Image iconImage = Toolkit.getDefaultToolkit().getImage(iconUrl);
        window.setIconImage(iconImage);

        JPopupMenu swingPopup = new JPopupMenu();

        // Thêm Phần lựa chọn Chế độ
        JMenuItem titleItem = new JMenuItem("--- Chọn chế độ ---");
        titleItem.setEnabled(false);

        JCheckBoxMenuItem casualItem = new JCheckBoxMenuItem("Casual Mode", true);
        JCheckBoxMenuItem gamingItem = new JCheckBoxMenuItem("Gaming Mode");
        JCheckBoxMenuItem workingItem = new JCheckBoxMenuItem("Working Mode");
        workingItem.setEnabled(false);

        casualItem.addActionListener(e -> {
            gamingItem.setSelected(false);
            casualItem.setSelected(true);
            miku.setAppMode(MikuCharacter.AppMode.CASUAL);
        });

        gamingItem.addActionListener(e -> {
            casualItem.setSelected(false);
            gamingItem.setSelected(true);
            miku.setAppMode(MikuCharacter.AppMode.GAMING);
        });

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
                java.awt.Dimension pSize = swingPopup.getSize();

                java.awt.Rectangle safeArea = new java.awt.Rectangle(
                        pLoc.x - 15, pLoc.y - 15,
                        pSize.width + 30, pSize.height + 30);

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
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                hiddenDialog.setVisible(false);
            }

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
            System.out.println("Lỗi không thể tạo icon khay hệ thống: " + e.getMessage());
        }
    }
}