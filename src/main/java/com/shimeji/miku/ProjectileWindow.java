package com.shimeji.miku;

import javax.swing.*;
import java.awt.*;

public class ProjectileWindow extends JWindow {
    private ThrowableItem item;

    // Đảm bảo cửa sổ đủ lớn để chứa cọng hành xoay mà không bị cắt viền
    private static final int WINDOW_SIZE = 120;

    public ProjectileWindow(Window owner, ThrowableItem item) {
        // CHUẨN OOP: Ép buộc cửa sổ Con kế thừa chính xác cấu hình đồ họa
        // của cửa sổ Cha để KHÔNG BỊ MẤT khả năng nền trong suốt
        super(owner, owner.getGraphicsConfiguration());

        this.item = item;
        setAlwaysOnTop(true);

        // Đặt màu nền trong suốt cho toàn bộ Cửa sổ và Lớp chứa (ContentPane)
        setBackground(new Color(0, 0, 0, 0));
        getContentPane().setBackground(new Color(0, 0, 0, 0));

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (item.isActive()) {
                    item.draw((Graphics2D) g);
                }
            }
        };

        // Panel cũng phải trong suốt
        panel.setOpaque(false);
        add(panel);
        setSize(WINDOW_SIZE, WINDOW_SIZE);
    }

    public void syncBounds() {
        if (!item.isActive()) {
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