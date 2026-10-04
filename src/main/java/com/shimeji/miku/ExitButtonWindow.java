package com.shimeji.miku;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ExitButtonWindow extends JWindow {
    private MikuCharacter miku;
    private boolean isHovered = false;

    public ExitButtonWindow(Window owner, MikuCharacter miku) {
        super(owner, owner.getGraphicsConfiguration());
        this.miku = miku;

        // Kích thước chuẩn của nút Close trên Windows 10/11
        setSize(46, 32);
        // Đặt sát rạt vào góc trên cùng màn hình
        setLocation(0, 0);

        setAlwaysOnTop(true);
        setFocusableWindowState(false);
        setType(Window.Type.UTILITY);
        setBackground(new Color(0, 0, 0, 0));

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();

                // 👉 BÍ QUYẾT Ổ ĐÂY: Xóa sạch sẽ toàn bộ màu cũ của frame trước!
                // Tránh việc màu mờ 1/255 bị cộng dồn 30 lần/giây thành màu đen thui.
                g2d.setComposite(AlphaComposite.Clear);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                // Trở lại chế độ vẽ màu bình thường
                g2d.setComposite(AlphaComposite.SrcOver);

                // 1. Xử lý màu nền
                if (isHovered) {
                    g2d.setColor(new Color(232, 17, 35));
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                    g2d.setColor(Color.WHITE);
                } else {
                    // Lần này nền mờ 1/255 sẽ VĨNH VIỄN TÀNG HÌNH vì không bị cộng dồn nữa!
                    g2d.setColor(new Color(0, 0, 0, 1));
                    g2d.fillRect(0, 0, getWidth(), getHeight());

                    // Dấu X lúc bình thường (Màu trắng sáng mờ để dễ nhìn trên hình nền tối)
                    g2d.setColor(new Color(220, 220, 220));
                }

                // 2. Vẽ dấu X
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                int r = 5;

                g2d.drawLine(cx - r, cy - r, cx + r, cy + r);
                g2d.drawLine(cx + r, cy - r, cx - r, cy + r);

                g2d.dispose();
            }
        };
        panel.setOpaque(false);

        // Nút của Windows dùng con trỏ mặc định (mũi tên) chứ không biến thành bàn tay
        panel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                panel.repaint(); // Cập nhật lại giao diện khi rê chuột vào
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                panel.repaint(); // Cập nhật lại giao diện khi chuột đi ra
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                miku.forceExitGiantState();
            }
        });

        add(panel);
    }

    public void syncVisibility() {
        boolean shouldBeVisible = (miku.getScale() > 1.0);
        if (isVisible() != shouldBeVisible) {
            setVisible(shouldBeVisible);
        }
    }
}