package com.shimeji.miku;

import java.awt.*;
import java.io.File;
import java.util.ResourceBundle;
import java.util.function.BiFunction;
import java.util.prefs.Preferences;
import javax.swing.*;

public class SettingsMenu extends JMenu {
    public SettingsMenu(MikuCharacter miku, MusicPlayer musicPlayer, ResourceBundle lang,
            Preferences prefs, JPopupMenu swingPopup) { // <- Bỏ JDialog hiddenDialog
        super(lang.getString("menu.settings"));

        // 1. Menu Ngôn ngữ
        JMenu languageMenu = new JMenu(lang.getString("menu.language"));
        String savedLang = prefs.get("mikuLanguage", "vi");
        JCheckBoxMenuItem langViItem = new JCheckBoxMenuItem("Tiếng Việt", savedLang.equals("vi"));
        JCheckBoxMenuItem langEnItem = new JCheckBoxMenuItem("English", savedLang.equals("en"));

        langViItem.addActionListener(e -> {
            prefs.put("mikuLanguage", "vi");
            restartApp(musicPlayer);
        });
        langEnItem.addActionListener(e -> {
            prefs.put("mikuLanguage", "en");
            restartApp(musicPlayer);
        });

        languageMenu.add(langViItem);
        languageMenu.add(langEnItem);
        add(languageMenu);
        addSeparator();

        // 2. Cài đặt Pomodoro
        int savedWorkTime = prefs.getInt("mikuWorkTime", 3600);
        int savedBreakTime = prefs.getInt("mikuBreakTime", 60);

        BiFunction<String, Integer, JLabel> createClickableBox = (text, maxVal) -> {
            JLabel label = new JLabel(text, SwingConstants.CENTER);
            label.setOpaque(true);
            label.setBackground(Color.WHITE);
            label.setForeground(Color.BLACK);
            label.setPreferredSize(new Dimension(35, 22));
            label.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
            label.setCursor(new Cursor(Cursor.HAND_CURSOR));

            label.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mousePressed(java.awt.event.MouseEvent e) {
                    swingPopup.putClientProperty("isEditingTime", true);
                    String input = JOptionPane.showInputDialog(swingPopup, "Nhập thời gian:", label.getText());

                    if (input != null && !input.trim().isEmpty()) {
                        try {
                            int val = Integer.parseInt(input.trim());
                            if (val >= 0 && (maxVal == 0 || val <= maxVal)) {
                                label.setText(maxVal > 0 ? String.format("%02d", val) : String.valueOf(val));
                                label.firePropertyChange("timeChanged", 0, 1);
                            }
                        } catch (Exception ignored) {
                        }
                    }
                    swingPopup.putClientProperty("isEditingTime", false);
                }
            });
            return label;
        };

        JPanel workTimePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        workTimePanel.setBackground(swingPopup.getBackground());
        JLabel workLabel = new JLabel(lang.getString("menu.worktime") + ":");
        workLabel.setForeground(Color.WHITE);
        workLabel.setPreferredSize(new Dimension(110, 22));

        JLabel workMinBox = createClickableBox.apply(String.valueOf(savedWorkTime / 60), 0);
        JLabel colon1 = new JLabel(":");
        colon1.setForeground(Color.WHITE);
        JLabel workSecBox = createClickableBox.apply(String.format("%02d", savedWorkTime % 60), 59);

        workTimePanel.add(workLabel);
        workTimePanel.add(workMinBox);
        workTimePanel.add(colon1);
        workTimePanel.add(workSecBox);

        JPanel breakTimePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        breakTimePanel.setBackground(swingPopup.getBackground());
        JLabel breakLabel = new JLabel(lang.getString("menu.breaktime") + ":");
        breakLabel.setForeground(Color.WHITE);
        breakLabel.setPreferredSize(new Dimension(110, 22));

        JLabel breakMinBox = createClickableBox.apply(String.valueOf(savedBreakTime / 60), 0);
        JLabel colon2 = new JLabel(":");
        colon2.setForeground(Color.WHITE);
        JLabel breakSecBox = createClickableBox.apply(String.format("%02d", savedBreakTime % 60), 59);

        breakTimePanel.add(breakLabel);
        breakTimePanel.add(breakMinBox);
        breakTimePanel.add(colon2);
        breakTimePanel.add(breakSecBox);

        java.beans.PropertyChangeListener autoSaver = evt -> {
            int wm = Integer.parseInt(workMinBox.getText());
            int ws = Integer.parseInt(workSecBox.getText());
            int totalWork = wm * 60 + ws;
            if (totalWork < 10) {
                totalWork = 10;
                workMinBox.setText("0");
                workSecBox.setText("10");
            }
            prefs.putInt("mikuWorkTime", totalWork);

            int bm = Integer.parseInt(breakMinBox.getText());
            int bs = Integer.parseInt(breakSecBox.getText());
            int totalBreak = bm * 60 + bs;
            if (totalBreak < 10) {
                totalBreak = 10;
                breakMinBox.setText("0");
                breakSecBox.setText("10");
            }
            prefs.putInt("mikuBreakTime", totalBreak);

            miku.updateTimers(totalWork, totalBreak);
        };

        workMinBox.addPropertyChangeListener("timeChanged", autoSaver);
        workSecBox.addPropertyChangeListener("timeChanged", autoSaver);
        breakMinBox.addPropertyChangeListener("timeChanged", autoSaver);
        breakSecBox.addPropertyChangeListener("timeChanged", autoSaver);

        MikuMenu.keepMenuOpen(workMinBox, workSecBox, breakMinBox, breakSecBox);

        add(workTimePanel);
        add(breakTimePanel);
    }

    private void restartApp(MusicPlayer musicPlayer) {
        musicPlayer.saveState();
        try {
            String javaCmd = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
            File currentFile = new File(MikuMenu.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (currentFile.getName().endsWith(".jar")) {
                new ProcessBuilder(javaCmd, "-jar", currentFile.getPath()).start();
            } else {
                new ProcessBuilder(javaCmd, "-cp", System.getProperty("java.class.path"), "com.shimeji.miku.App")
                        .start();
            }
        } catch (Exception ex) {
            System.out.println("Lỗi khởi động lại: " + ex.getMessage());
        }
        System.exit(0);
    }
}