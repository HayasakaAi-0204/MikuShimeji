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

    // Tối ưu hóa OOP: Lưu đối tượng Menu vào thuộc tính class thay vì khởi tạo lại
    private JPopupMenu popupMenu;

    public MikuWindow(MikuCharacter miku) {
        this.miku = miku;
        this.initialClick = new Point();

        setupWindow();
        setupMenu(); // Tách hàm cho code sạch (Clean Code)
        setupMouseEvents();
    }

    private void setupWindow() {
        setAlwaysOnTop(true);
        setBackground(new Color(0, 0, 0, 0)); // Bật nền trong suốt 100%

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawMiku(g);
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

        JMenuItem exitItem = new JMenuItem("Thoát (Dismiss)");
        exitItem.addActionListener(e -> System.exit(0));

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
        // CHUẨN OOP: Giao phó (Delegate) việc lấy ảnh cho Model. View không cần biết
        // logic trạng thái.
        BufferedImage img = miku.getCurrentImage();
        if (img == null)
            return;

        // TỐI ƯU 1: Caching biến cục bộ. Tránh gọi hàm getter hàng chục lần trong 1
        // vòng lặp vẽ.
        int w = miku.getWidth();
        int h = miku.getHeight();
        CharacterState state = miku.getState();
        boolean isPaused = miku.isPaused();

        // TỐI ƯU 2: Kích hoạt phần cứng đồ họa (Hardware Acceleration) để vẽ sắc nét
        // hơn
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Vẽ lật ảnh (Moonwalk fix) hoặc vẽ bình thường
        if (state == CharacterState.WALKING_RIGHT && !isPaused) {
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
                // Tối ưu gộp 2 hàm MouseListener cũ lại làm 1
                if (SwingUtilities.isLeftMouseButton(e)) {
                    miku.setState(CharacterState.FALLING);
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (miku.getState() == CharacterState.DRAGGING) {
                    int newX = getLocation().x + e.getX() - initialClick.x;
                    int newY = getLocation().y + e.getY() - initialClick.y;
                    miku.setPosition(newX, newY);
                }
            }
        });
    }

    public void syncBounds() {
        setBounds(miku.getX(), miku.getY(), miku.getWidth(), miku.getHeight());
    }
}