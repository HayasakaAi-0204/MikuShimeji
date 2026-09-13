package com.shimeji.miku;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Toolkit;

public class App {

    private static final int PHYSICS_TICK_RATE = 33;
    private static final int ANIMATION_TICK_RATE = 24;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            startShimeji();
        });
    }

    private static void startShimeji() {
        ResourceManager.loadImages();

        int screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
        MikuCharacter miku = new MikuCharacter(screenWidth / 2, 0);

        // Tạo cửa sổ Cha (Miku)
        MikuWindow window = new MikuWindow(miku);
        window.setVisible(true);

        // Tạo cửa sổ Con (Cọng hành) và truyền cửa sổ Cha vào
        ProjectileWindow projectileWindow = new ProjectileWindow(window, miku.getEquippedItem());

        TaskbarManager taskbarManager = new TaskbarManager();

        Timer physicsTimer = new Timer(PHYSICS_TICK_RATE, e -> {
            taskbarManager.update();

            int currentFloorY = taskbarManager.getCurrentFloorY(miku.getHeight());
            miku.updatePhysics(currentFloorY);

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
}