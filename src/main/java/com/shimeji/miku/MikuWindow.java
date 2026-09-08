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

    public MikuWindow(MikuCharacter miku) {
        this.miku = miku;
        this.initialClick = new Point();

        setupWindow();
        setupMouseEvents();

        // === TẠO MENU CHUỘT PHẢI ===
        JPopupMenu popupMenu = new JPopupMenu();

        // 1. Tạo các nút tính năng chờ
        JMenuItem feature1 = new JMenuItem("Gọi thêm Miku (Call Another)");
        JMenuItem feature2 = new JMenuItem("Đi theo chuột (Follow Cursor)");
        feature1.setEnabled(false);
        feature2.setEnabled(false);

        // 2. Tạo nút Thoát (Dismiss)
        JMenuItem exitItem = new JMenuItem("Thoát (Dismiss)");
        exitItem.addActionListener(e -> {
            System.exit(0);
        });

        // 3. Lắp ráp các nút vào Menu
        popupMenu.add(feature1);
        popupMenu.add(feature2);
        popupMenu.addSeparator();
        popupMenu.add(exitItem);

        // Lắng nghe trạng thái của Menu
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

        // 4. Bắt sự kiện Click chuột phải lên bé Miku
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }

    private void setupWindow() {
        setAlwaysOnTop(true);
        setBackground(new Color(0, 0, 0, 0));

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

    private void drawMiku(Graphics g) {
        // CHUẨN OOP: Không gán cứng ảnh mặc định nữa, để Logic tự quyết định
        BufferedImage img = null;
        CharacterState state = miku.getState();

        // 1. ƯU TIÊN 1: Kiểm tra xem có đang mở Menu không?
        if (miku.isPaused()) {
            img = ResourceManager.getPausedImage(); // Lấy ảnh tĩnh (img1.png)
        }
        // 2. Nếu không mở Menu, xử lý các hoạt ảnh động bình thường
        else if (state == CharacterState.DRAGGING) {
            img = ResourceManager.getDragImages()[miku.getDragFrameIndex()];
        } else if (state == CharacterState.FALLING) {
            img = ResourceManager.getFallImages()[miku.getFallFrameIndex()];
        } else if (state == CharacterState.IDLE) {
            // THÊM MỚI: Lấy mảng 240 ảnh chờ và nhịp hiện tại từ Model
            img = ResourceManager.getIdleImages()[miku.getIdleFrameIndex()];
        } else if (state == CharacterState.WALKING_LEFT || state == CharacterState.WALKING_RIGHT) {
            img = ResourceManager.getWalkImages()[miku.getWalkFrameIndex()];
        }

        if (img != null) {
            // Sửa lỗi Moonwalk
            if (state == CharacterState.WALKING_RIGHT && !miku.isPaused()) {
                g.drawImage(img, miku.getWidth(), 0, -miku.getWidth(), miku.getHeight(), null);
            } else {
                g.drawImage(img, 0, 0, miku.getWidth(), miku.getHeight(), null);
            }
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
                    miku.setState(CharacterState.FALLING);
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