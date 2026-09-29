package com.shimeji.miku;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

public class MikuWindow extends JWindow {
    private MikuCharacter miku;
    private Point initialClick;
    private JPopupMenu popupMenu;
    private JPanel renderPanel;

    private long lastPopupCloseTime = 0;
    private boolean isLeftMouseDown = false; // 👉 THÊM MỚI: Biến cảm nhận ngón tay đang đè chuột trái

    public MikuWindow(MikuCharacter miku) {
        this.miku = miku;
        this.initialClick = new Point();

        setupWindow();
        setupMenu();
        setupMouseEvents();
    }

    private void setupWindow() {
        setAlwaysOnTop(true);
        setFocusableWindowState(false);
        setBackground(new Color(0, 0, 0, 0));

        renderPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawMiku(g);
            }
        };
        renderPanel.setOpaque(false);
        add(renderPanel);
    }

    private void setupMenu() {
        popupMenu = new JPopupMenu();

        JMenuItem feature1 = new JMenuItem("Gọi thêm Miku (Call Another)");
        JMenuItem feature2 = new JMenuItem("Đi theo chuột (Follow Cursor)");
        feature1.setEnabled(false);
        feature2.setEnabled(false);

        JMenuItem throwAction = new JMenuItem("Ném hành (Throw Leek)");
        throwAction.addActionListener(e -> {
            miku.setState(CharacterState.THROWING);
        });

        JMenuItem exitItem = new JMenuItem("Thoát (Dismiss)");
        exitItem.addActionListener(e -> System.exit(0));

        popupMenu.add(throwAction);
        popupMenu.addSeparator();
        popupMenu.add(feature1);
        popupMenu.add(feature2);
        popupMenu.addSeparator();
        popupMenu.add(exitItem);

        popupMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                miku.setPaused(true);
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                miku.setPaused(false);
                lastPopupCloseTime = System.currentTimeMillis();
                setAlwaysOnTop(true);

                // 👉 SỬA LỖI TUỘT TAY:
                // Khi tắt Menu, CHỈ cho Miku rơi xuống nếu bạn KHÔNG CÒN giữ chuột trái!
                if (miku.getState() == CharacterState.DRAGGING && !isLeftMouseDown) {
                    miku.setState(CharacterState.FALLING);
                }
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                miku.setPaused(false);
                lastPopupCloseTime = System.currentTimeMillis();
                setAlwaysOnTop(true);

                if (miku.getState() == CharacterState.DRAGGING && !isLeftMouseDown) {
                    miku.setState(CharacterState.FALLING);
                }
            }
        });
    }

    private void drawMiku(Graphics g) {
        BufferedImage img = miku.getCurrentImage();
        if (img == null)
            return;

        int w = miku.getWidth();
        int h = miku.getHeight();
        Graphics2D g2d = (Graphics2D) g;

        if (miku.isFacingRight()) {
            g2d.drawImage(img, w, 0, -w, h, null);
        } else {
            g2d.drawImage(img, 0, 0, w, h, null);
        }
    }

    private void showMenu(MouseEvent e) {
        int popupX = e.getX();
        int popupY = e.getY();

        Dimension menuSize = popupMenu.getPreferredSize();
        int menuWidth = menuSize.width;
        int menuHeight = menuSize.height;

        int screenCursorX = miku.getX() + popupX;
        int screenCursorY = miku.getY() + popupY;

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        if (screenCursorX + menuWidth > screenSize.width) {
            popupX = popupX - menuWidth;
        }

        if (screenCursorY + menuHeight > screenSize.height) {
            popupY = popupY - menuHeight;
        }

        popupMenu.show(e.getComponent(), popupX, popupY);
    }

    private void setupMouseEvents() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // 👉 ĐÃ THÊM MỚI: Né đòn Click chuột trong Gaming Mode
                if (miku.getAppMode() == MikuCharacter.AppMode.GAMING) {
                    // Dù click trái hay phải, lập tức bốc hơi sang bờ tường đối diện!
                    int screenWidth = miku.getScreenWidth();
                    if (miku.getX() < screenWidth / 2) {
                        miku.changeState(new ClimbState(false, false, true));
                    } else {
                        miku.changeState(new ClimbState(true, false, true));
                    }
                    return; // Block lệnh click, không mở Menu hay Drag gì cả!
                }

                // --- PHẦN CODE CŨ (ĐƯỢC GIỮ NGUYÊN) ---
                if (SwingUtilities.isRightMouseButton(e)) {
                    if (System.currentTimeMillis() - lastPopupCloseTime > 150) {
                        showMenu(e);
                    }
                    return;
                }

                if (SwingUtilities.isLeftMouseButton(e)) {
                    isLeftMouseDown = true; // Ghi nhận là ngón tay đang đè chuột trái

                    miku.setState(CharacterState.DRAGGING);

                    int newWidth = miku.getWidth();
                    Point screenPos = e.getLocationOnScreen();

                    int grabX = newWidth / 2;
                    int grabY = 50;

                    miku.setX(screenPos.x - grabX);
                    miku.setY(screenPos.y - grabY);

                    syncBounds();
                    initialClick = new Point(grabX, grabY);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    isLeftMouseDown = false; // Ghi nhận là ngón tay đã thả ra

                    if (miku.getState() == CharacterState.DRAGGING) {
                        if (!popupMenu.isVisible()) {
                            miku.setState(CharacterState.FALLING);
                        }
                    }
                }
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (miku.getState() == CharacterState.DRAGGING) {

                    if (popupMenu.isVisible()) {
                        popupMenu.setVisible(false);
                    }

                    Point screenLocation = e.getLocationOnScreen();
                    int newX = screenLocation.x - initialClick.x;
                    int newY = screenLocation.y - initialClick.y;
                    miku.setPosition(newX, newY);
                }
            }
        });
    }

    public void syncBounds() {
        setBounds(miku.getX(), miku.getY(), miku.getWidth(), miku.getHeight());
    }
}