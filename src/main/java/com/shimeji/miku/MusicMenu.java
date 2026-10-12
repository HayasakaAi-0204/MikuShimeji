package com.shimeji.miku;

import java.awt.*;
import java.io.File;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;
import javax.swing.*;

public class MusicMenu extends JMenu {

    public MusicMenu(MusicPlayer musicPlayer, ResourceBundle lang, Preferences prefs, JPopupMenu swingPopup) {
        super(lang.getString("menu.music"));

        Color menuBg = swingPopup.getBackground();
        JMenuItem trackNameItem = new JMenuItem(lang.getString("menu.track.stop"));
        trackNameItem.setEnabled(false);

        // 1. Panel Âm lượng
        JPanel volumePanel = new JPanel(new BorderLayout());
        volumePanel.setBackground(menuBg);
        JLabel volLabel = new JLabel(" 🔊 ");
        volLabel.setForeground(Color.WHITE);
        volLabel.setPreferredSize(new Dimension(35, 20));

        int savedVolume = prefs.getInt("mikuVolume", 50);
        JSlider volSlider = new JSlider(0, 100, savedVolume);
        volSlider.setOpaque(false);
        musicPlayer.setVolume(savedVolume / 100.0);

        JLabel volValueLabel = new JLabel(savedVolume + "% ");
        volValueLabel.setForeground(Color.WHITE);
        volValueLabel.setPreferredSize(new Dimension(95, 20));
        volValueLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        volumePanel.add(volLabel, BorderLayout.WEST);
        volumePanel.add(volSlider, BorderLayout.CENTER);
        volumePanel.add(volValueLabel, BorderLayout.EAST);

        volSlider.addChangeListener(e -> {
            int vol = volSlider.getValue();
            musicPlayer.setVolume(vol / 100.0);
            volValueLabel.setText(vol + "% ");
            if (!volSlider.getValueIsAdjusting()) {
                prefs.putInt("mikuVolume", vol);
            }
        });

        // 2. Panel Tiến độ bài hát
        JPanel progressPanel = new JPanel(new BorderLayout());
        progressPanel.setBackground(menuBg);
        JLabel progLabel = new JLabel(" ⏳ ");
        progLabel.setForeground(Color.WHITE);
        progLabel.setPreferredSize(new Dimension(35, 20));

        JSlider progSlider = new JSlider(0, 100, 0);
        progSlider.setOpaque(false);

        JLabel progValueLabel = new JLabel("00:00 : 00:00 ");
        progValueLabel.setForeground(Color.WHITE);
        progValueLabel.setPreferredSize(new Dimension(95, 20));
        progValueLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        progressPanel.add(progLabel, BorderLayout.WEST);
        progressPanel.add(progSlider, BorderLayout.CENTER);
        progressPanel.add(progValueLabel, BorderLayout.EAST);

        progSlider.addChangeListener(e -> {
            if (progSlider.getValueIsAdjusting()) {
                double total = musicPlayer.getTotalDurationSeconds();
                double current = (progSlider.getValue() / 100.0) * total;
                musicPlayer.seek(current);
                progValueLabel.setText(String.format("%02d:%02d : %02d:%02d ",
                        (int) current / 60, (int) current % 60, (int) total / 60, (int) total % 60));
            }
        });

        // 3. Panel Nút bấm Media
        JPanel mediaControlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        mediaControlPanel.setBackground(menuBg);

        JButton btnShuffle = new JButton("🔀");
        JButton btnPrev = new JButton("⏮");
        JButton btnPlay = new JButton(musicPlayer.isPlaying() ? "⏸" : "▶");
        JButton btnNext = new JButton("⏭");
        JButton btnRepeat = new JButton("🔁");

        Color colorOn = new Color(0, 150, 255);
        Color colorOff = Color.GRAY;

        JButton[] mediaBtns = { btnShuffle, btnPrev, btnPlay, btnNext, btnRepeat };
        for (JButton btn : mediaBtns) {
            btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
            btn.setFocusPainted(false);
            btn.setContentAreaFilled(false);
            btn.setBorderPainted(false);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.setMargin(new Insets(0, 0, 0, 0));
            mediaControlPanel.add(btn);
        }

        btnShuffle.setForeground(musicPlayer.isShuffle() ? colorOn : colorOff);
        btnRepeat.setForeground(musicPlayer.isRepeatOne() ? colorOn : colorOff);
        btnPrev.setForeground(Color.WHITE);
        btnNext.setForeground(Color.WHITE);
        btnPlay.setForeground(Color.WHITE);

        btnShuffle.addActionListener(e -> {
            boolean newState = !musicPlayer.isShuffle();
            musicPlayer.setShuffle(newState);
            btnShuffle.setForeground(newState ? colorOn : colorOff);
            SwingUtilities.invokeLater(swingPopup::repaint);
        });

        btnRepeat.addActionListener(e -> {
            boolean newState = !musicPlayer.isRepeatOne();
            musicPlayer.setRepeatOne(newState);
            btnRepeat.setForeground(newState ? colorOn : colorOff);
            SwingUtilities.invokeLater(swingPopup::repaint);
        });

        btnPlay.addActionListener(e -> {
            musicPlayer.togglePlayPause();
            btnPlay.setText(musicPlayer.isPlaying() ? "⏸" : "▶");
            SwingUtilities.invokeLater(swingPopup::repaint);
        });

        btnNext.addActionListener(e -> {
            trackNameItem.setText(lang.getString("menu.track.loading"));
            progSlider.setValue(0);
            progValueLabel.setText("00:00 : 00:00 ");
            musicPlayer.next();
            btnPlay.setText("⏸");
            SwingUtilities.invokeLater(swingPopup::repaint);
        });

        btnPrev.addActionListener(e -> {
            trackNameItem.setText(lang.getString("menu.track.loading"));
            progSlider.setValue(0);
            progValueLabel.setText("00:00 : 00:00 ");
            musicPlayer.previous();
            btnPlay.setText("⏸");
            SwingUtilities.invokeLater(swingPopup::repaint);
        });

        // 4. Chọn thư mục nhạc
        JMenuItem changeFolderItem = new JMenuItem(lang.getString("menu.folder"));
        changeFolderItem.addActionListener(e -> {
            FileDialog dialog = new FileDialog((Frame) null, lang.getString("menu.folder.tip"), FileDialog.LOAD);
            dialog.setFile("*.mp3;*.wav");
            dialog.setVisible(true);
            String dir = dialog.getDirectory();
            if (dir != null) {
                musicPlayer.setMusicFolder(new File(dir));
            }
            btnPlay.setText("⏸");
        });

        MikuMenu.keepMenuOpen(volSlider, progSlider, btnShuffle, btnPrev, btnPlay, btnNext, btnRepeat);

        add(trackNameItem);
        add(progressPanel);
        add(volumePanel);
        addSeparator();
        add(mediaControlPanel);
        add(changeFolderItem);

        // Lắng nghe cập nhật từ MusicPlayer
        musicPlayer.setOnProgressUpdate(() -> SwingUtilities.invokeLater(() -> {
            if (!progSlider.getValueIsAdjusting()) {
                double current = musicPlayer.getCurrentTimeSeconds();
                double total = musicPlayer.getTotalDurationSeconds();
                if (total > 0) {
                    progSlider.setValue((int) ((current / total) * 100));
                    progValueLabel.setText(String.format("%02d:%02d : %02d:%02d ",
                            (int) current / 60, (int) current % 60, (int) total / 60, (int) total % 60));
                }
            }
        }));

        musicPlayer.setOnTrackChange(() -> SwingUtilities.invokeLater(() -> {
            trackNameItem.setText("♪ " + musicPlayer.getCurrentTrackName());
            double current = musicPlayer.getCurrentTimeSeconds();
            double total = musicPlayer.getTotalDurationSeconds();
            if (total > 0) {
                if (current > total)
                    current = 0;
                progSlider.setValue((int) ((current / total) * 100));
                progValueLabel.setText(String.format("%02d:%02d : %02d:%02d ",
                        (int) current / 60, (int) current % 60, (int) total / 60, (int) total % 60));
            }
        }));
    }
}