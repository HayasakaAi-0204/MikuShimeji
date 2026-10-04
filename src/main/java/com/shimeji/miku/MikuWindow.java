// File 2: MikuWindow.java
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
    private long lastPopupCloseTime = 0;
    private boolean isLeftMouseDown = false;

    public MikuWindow(MikuCharacter miku) {
        this.miku = miku;
        this.initialClick = new Point();
        setupWindow();
        setupMenu();
        setupMouseEvents();
    }

    private void setupWindow() {
        // 👉 NGĂN CHẶN WINDOWS 11 ẢO TƯỞNG SỨC MẠNH:
        // Gắn mác Cửa sổ Tiện ích để Windows không bao giờ giấu Taskbar nữa!
        setType(Window.Type.UTILITY);

        setAlwaysOnTop(true);
        setFocusableWindowState(false);
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
        throwAction.addActionListener(e -> miku.setState(CharacterState.THROWING));
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
                lastPopupCloseTime = System.currentTimeMillis();
                setAlwaysOnTop(true);
                if (miku.getState() == CharacterState.DRAGGING && !isLeftMouseDown)
                    miku.setState(CharacterState.FALLING);
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                miku.setPaused(false);
                lastPopupCloseTime = System.currentTimeMillis();
                setAlwaysOnTop(true);
                if (miku.getState() == CharacterState.DRAGGING && !isLeftMouseDown)
                    miku.setState(CharacterState.FALLING);
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

        // 👉 Cân bằng hoàn hảo giữa MƯỢT MÀ và NHẸ MÁY
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (miku.isFacingRight())
            g2d.drawImage(img, w, 0, -w, h, null);
        else
            g2d.drawImage(img, 0, 0, w, h, null);

        String countdown = miku.getCountdownText();
        if (countdown != null) {

            // 👉 ÉP BUỘC: CHỈ VẼ ĐỒNG HỒ TRÊN ĐẦU NẾU LÀ MIKU NHỎ
            if (miku.getScale() <= 1.0) {
                int fontSize = (int) (16 * Math.max(1.0, miku.getScale() * 0.4));
                g2d.setFont(new Font("Arial", Font.BOLD, fontSize));
                FontMetrics fm = g2d.getFontMetrics();
                int tx = (miku.getWidth() - fm.stringWidth(countdown)) / 2;
                int ty = (int) (30 * miku.getScale());

                if (miku.isClimbing()) {
                    if (miku.isFacingRight()) {
                        tx += (int) (60 * miku.getScale());
                    } else {
                        tx += (int) (-60 * miku.getScale());
                    }
                } else {
                    if (miku.isFacingRight()) {
                        tx += (int) (0 * miku.getScale());
                    } else {
                        tx += (int) (0 * miku.getScale());
                    }
                }
                ty += (int) (0 * miku.getScale());

                g2d.setColor(new Color(255, 255, 255, 200));
                g2d.fillRoundRect(tx - 5, ty - fm.getAscent() - 5, fm.stringWidth(countdown) + 10, fm.getHeight() + 10,
                        10, 10);
                g2d.setColor(Color.RED);
                g2d.drawString(countdown, tx, ty);
            }
        }
    }

    private void showMenu(MouseEvent e) {
        int popupX = e.getX();
        int popupY = e.getY();
        Dimension menuSize = popupMenu.getPreferredSize();
        int screenCursorX = miku.getX() + popupX;
        int screenCursorY = miku.getY() + popupY;
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        if (screenCursorX + menuSize.width > screenSize.width)
            popupX = popupX - menuSize.width;
        if (screenCursorY + menuSize.height > screenSize.height)
            popupY = popupY - menuSize.height;
        popupMenu.show(e.getComponent(), popupX, popupY);
    }

    private void setupMouseEvents() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (miku.getAppMode() == MikuCharacter.AppMode.GAMING) {
                    if (miku.getX() < miku.getScreenWidth() / 2)
                        miku.changeState(new ClimbState(false, false, true));
                    else
                        miku.changeState(new ClimbState(true, false, true));
                    return;
                }

                // Khóa chuột lúc đang đi làm nhiệm vụ
                if (miku.isForcedPomodoroWalk()) {
                    return;
                }

                if (SwingUtilities.isRightMouseButton(e)) {
                    if (System.currentTimeMillis() - lastPopupCloseTime > 150)
                        showMenu(e);
                    return;
                }

                if (SwingUtilities.isLeftMouseButton(e)) {
                    isLeftMouseDown = true;
                    miku.setState(CharacterState.DRAGGING);

                    int grabX = miku.getWidth() / 2;
                    // 👉 Chuột luôn nằm ở bụng nhân vật
                    int grabY = miku.getHeight() / 2;

                    miku.setX(e.getLocationOnScreen().x - grabX);
                    miku.setY(e.getLocationOnScreen().y - grabY);
                    syncBounds();
                    initialClick = new Point(grabX, grabY);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    isLeftMouseDown = false;
                    if (miku.getState() == CharacterState.DRAGGING && !popupMenu.isVisible()) {
                        miku.setState(CharacterState.FALLING);
                    }
                }
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (miku.getState() == CharacterState.DRAGGING) {
                    if (popupMenu.isVisible())
                        popupMenu.setVisible(false);
                    miku.setPosition(e.getLocationOnScreen().x - initialClick.x,
                            e.getLocationOnScreen().y - initialClick.y);
                }
            }
        });
    }

    public void syncBounds() {
        int y = miku.getY();
        int h = miku.getHeight();

        // 👉 CẮT ĐUÔI CỬA SỔ: Chặn không cho khung hình tàng hình đè lên Taskbar
        // Việc này sẽ chấm dứt việc Windows 11 lầm tưởng và đem giấu Taskbar đi!
        try {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            GraphicsConfiguration gc = getGraphicsConfiguration();
            if (gc != null) {
                Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
                int taskbarTop = screenSize.height - insets.bottom;

                if (y + h > taskbarTop) {
                    h = Math.max(1, taskbarTop - y);
                }
            }
        } catch (Exception e) {
        }

        setBounds(miku.getX(), y, miku.getWidth(), h);
    }
}