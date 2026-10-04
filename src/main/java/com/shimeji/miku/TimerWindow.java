package com.shimeji.miku;

import javax.swing.*;
import java.awt.*;

public class TimerWindow extends JWindow {
    private MikuCharacter miku;

    public TimerWindow(Window owner, MikuCharacter miku) {
        super(owner, owner.getGraphicsConfiguration());
        this.miku = miku;

        setSize(250, 100);

        // Đặt ở góc trên cùng bên phải, cách lề 20px
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(screenSize.width - getWidth() - 20, 20);

        setAlwaysOnTop(true);
        setFocusableWindowState(false); // Tránh cướp chuột của Windows
        setType(Window.Type.UTILITY);
        setBackground(new Color(0, 0, 0, 0));

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();

                // Xóa nền cũ để tàng hình 100%
                g2d.setComposite(AlphaComposite.Clear);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.setComposite(AlphaComposite.SrcOver);

                String countdown = miku.getCountdownText();
                if (countdown != null && miku.getScale() > 1.0) {
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    int fontSize = 42;
                    Font font = new Font("Arial", Font.BOLD, fontSize);
                    g2d.setFont(font);
                    FontMetrics fm = g2d.getFontMetrics();

                    int textWidth = fm.stringWidth(countdown);
                    int textHeight = fm.getAscent();

                    int tx = getWidth() - textWidth - 20;
                    int ty = textHeight + 20;

                    // 👉 THUẬT TOÁN VẼ VIỀN CHỮ (Giúp click xuyên thấu)
                    java.awt.font.FontRenderContext frc = g2d.getFontRenderContext();
                    java.awt.font.TextLayout textLayout = new java.awt.font.TextLayout(countdown, font, frc);
                    Shape outline = textLayout.getOutline(java.awt.geom.AffineTransform.getTranslateInstance(tx, ty));

                    // 1. Vẽ viền ngoài màu đen (Dày 4 pixel để tạo khối)
                    g2d.setColor(new Color(0, 0, 0, 200));
                    g2d.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2d.draw(outline);

                    // 2. Tô ruột chữ màu đỏ pastel
                    g2d.setColor(new Color(255, 80, 80));
                    g2d.fill(outline);
                }

                g2d.dispose();
            }
        };
        panel.setOpaque(false);
        add(panel);
    }

    public void syncVisibility() {
        boolean shouldBeVisible = (miku.getScale() > 1.0 && miku.getCountdownText() != null);
        if (isVisible() != shouldBeVisible) {
            setVisible(shouldBeVisible);
        }
        if (isVisible()) {
            repaint(); // Vẽ lại liên tục để đồng hồ nhảy giây
        }
    }
}