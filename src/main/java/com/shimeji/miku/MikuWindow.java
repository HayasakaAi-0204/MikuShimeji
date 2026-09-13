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

    // THÊM MỚI: Biến lưu trữ thời gian đóng Menu để khắc phục lỗi của Swing
    private long lastPopupCloseTime = 0;

    public MikuWindow(MikuCharacter miku) {
        this.miku = miku;
        this.initialClick = new Point();

        setupWindow();
        setupMenu();
        setupMouseEvents();
    }

    private void setupWindow() {
        setAlwaysOnTop(true);
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
                // THÊM MỚI: Ghi lại thời gian Menu bị tắt
                lastPopupCloseTime = System.currentTimeMillis();
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                miku.setPaused(false);
                // THÊM MỚI: Ghi lại thời gian Menu bị hủy
                lastPopupCloseTime = System.currentTimeMillis();
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

    private void setupMouseEvents() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    miku.setState(CharacterState.DRAGGING);
                    initialClick = e.getPoint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (miku.getState() == CharacterState.DRAGGING) {
                        miku.setState(CharacterState.FALLING);
                    }
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    // CẬP NHẬT: Kiểm tra khoảng thời gian.
                    // Nếu thời gian từ lúc đóng Menu đến lúc thả chuột quá ngắn (< 150 mili-giây)
                    // thì từ chối mở lại Menu (để Menu được đóng hẳn).
                    if (System.currentTimeMillis() - lastPopupCloseTime > 150) {
                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (miku.getState() == CharacterState.DRAGGING) {
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