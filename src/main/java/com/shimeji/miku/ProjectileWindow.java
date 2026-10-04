package com.shimeji.miku;

import javax.swing.*;
import java.awt.*;

public class ProjectileWindow extends JWindow {
    private MikuCharacter miku;
    private static final int BASE_WINDOW_SIZE = 120;

    public ProjectileWindow(Window owner, MikuCharacter miku) {
        super(owner, owner.getGraphicsConfiguration());
        this.miku = miku; 
        setAlwaysOnTop(true);
        setFocusableWindowState(false);
        setBackground(new Color(0, 0, 0, 0));
        getContentPane().setBackground(new Color(0, 0, 0, 0));

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                ThrowableItem item = miku.getEquippedItem();
                if (item != null && item.isActive()) item.draw((Graphics2D) g);
            }
        };
        panel.setOpaque(false);
        add(panel);
        setSize(BASE_WINDOW_SIZE, BASE_WINDOW_SIZE);
    }

    public void syncBounds() {
        ThrowableItem item = miku.getEquippedItem();
        if (item == null || !item.isActive()) {
            if (isVisible()) setVisible(false);
            return;
        }
        if (!isVisible()) setVisible(true);

        int scaledSize = (int)(BASE_WINDOW_SIZE * item.getScale());
        if (getSize().width != scaledSize) setSize(scaledSize, scaledSize);
        setLocation(item.getX() - scaledSize / 2, item.getY() - scaledSize / 2);
    }
}