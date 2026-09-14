package com.shimeji.miku;

import javax.swing.*;
import java.awt.*;

public class ProjectileWindow extends JWindow {
    // SỬA LỖI: Không lưu vũ khí cố định nữa, mà lưu lại Miku để luôn cập nhật
    private MikuCharacter miku;

    private static final int WINDOW_SIZE = 120;

    public ProjectileWindow(Window owner, MikuCharacter miku) {
        super(owner, owner.getGraphicsConfiguration());

        this.miku = miku; // Lưu Miku lại
        setAlwaysOnTop(true);

        setBackground(new Color(0, 0, 0, 0));
        getContentPane().setBackground(new Color(0, 0, 0, 0));

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // CHUẨN OOP: Hỏi Miku xem đang cầm vũ khí gì để vẽ
                ThrowableItem item = miku.getEquippedItem();
                if (item != null && item.isActive()) {
                    item.draw((Graphics2D) g);
                }
            }
        };

        panel.setOpaque(false);
        add(panel);
        setSize(WINDOW_SIZE, WINDOW_SIZE);
    }

    public void syncBounds() {
        // CHUẨN OOP: Cập nhật tọa độ theo vũ khí hiện tại
        ThrowableItem item = miku.getEquippedItem();
        
        if (item == null || !item.isActive()) {
            if (isVisible()) {
                setVisible(false);
            }
            return;
        }

        if (!isVisible()) {
            setVisible(true);
        }

        setLocation(item.getX() - WINDOW_SIZE / 2, item.getY() - WINDOW_SIZE / 2);
    }
}