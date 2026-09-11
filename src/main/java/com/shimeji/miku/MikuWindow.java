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

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawMiku(g);
                drawLeek((Graphics2D) g);
            }
        };
        panel.setOpaque(false);
        add(panel);
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
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                miku.setPaused(false);
            }
        });
    }

    private void drawMiku(Graphics g) {
        BufferedImage img = miku.getCurrentImage();
        if (img == null)
            return;

        int w = miku.getWidth();
        int h = miku.getHeight();
        int x = miku.getX();
        int y = miku.getY();

        Graphics2D g2d = (Graphics2D) g;

        // =================================================================
        // FIX BUG HƯỚNG MẶT CUỐI CÙNG (Chuẩn OOP: Single Source of Truth)
        // =================================================================
        // View KHÔNG ĐƯỢC PHÉP can thiệp logic lật ảnh dựa theo State.
        // Toàn bộ ảnh gốc của Miku (đứng, đi, ném) đều quay về bên TRÁI.
        // Do đó:
        // - Nếu Model báo Miku nhìn sang PHẢI -> Lật ảnh.
        // - Nếu Model báo Miku nhìn sang TRÁI -> Không lật.

        if (miku.isFacingRight()) {
            g2d.drawImage(img, x + w, y, -w, h, null); // Vẽ lật (Flip X)
        } else {
            g2d.drawImage(img, x, y, w, h, null); // Vẽ thuận
        }
    }

    private void drawLeek(Graphics2D g2d) {
        ThrowableItem item = miku.getEquippedItem(); // Sửa dòng này
        if (item != null) {
            item.draw(g2d);
        }
    }

    private void setupMouseEvents() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int mx = e.getX();
                int my = e.getY();
                int mikuX = miku.getX();
                int mikuY = miku.getY();
                int mikuW = miku.getWidth();
                int mikuH = miku.getHeight();

                boolean isClickOnMiku = (mx >= mikuX && mx <= mikuX + mikuW && my >= mikuY && my <= mikuY + mikuH);

                if (SwingUtilities.isLeftMouseButton(e) && isClickOnMiku) {
                    miku.setState(CharacterState.DRAGGING);
                    initialClick = new Point(mx - mikuX, my - mikuY);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (miku.getState() == CharacterState.DRAGGING) {
                        miku.setState(CharacterState.FALLING);
                    }
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (miku.getState() == CharacterState.DRAGGING) {
                    int newX = e.getX() - initialClick.x;
                    int newY = e.getY() - initialClick.y;
                    miku.setPosition(newX, newY);
                }
            }
        });
    }

    public void syncBounds() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setBounds(0, 0, screenSize.width, screenSize.height);
    }
}