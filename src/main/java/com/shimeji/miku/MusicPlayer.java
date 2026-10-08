package com.shimeji.miku;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

public class MusicPlayer {
    private List<File> playlist;
    private int currentTrackIndex = 0;
    private MediaPlayer mediaPlayer;
    private File musicFolder;
    private final Preferences prefs;
    private boolean isPlaying = false;

    // Cảm biến để báo cáo thời gian thực cho Thanh trượt giao diện
    private Runnable onProgressUpdate;
    private Runnable onTrackChange;

    public MusicPlayer() {
        prefs = Preferences.userNodeForPackage(MusicPlayer.class);
        playlist = new ArrayList<>();
        String savedPath = prefs.get("musicFolderPath", null);
        if (savedPath != null) {
            musicFolder = new File(savedPath);
            scanMusic();
            if (!playlist.isEmpty()) {
                prepareTrack(0);
            }
        }
    }

    public void setMusicFolder(File folder) {
        if (folder != null && folder.exists() && folder.isDirectory()) {
            this.musicFolder = folder;
            prefs.put("musicFolderPath", folder.getAbsolutePath());
            scanMusic();
            if (mediaPlayer != null) {
                mediaPlayer.stop();
            }
            if (!playlist.isEmpty()) {
                prepareTrack(0);
                play();
            }
        }
    }

    public String getMusicFolderName() {
        return (musicFolder != null) ? musicFolder.getName() : "Chưa chọn";
    }

    public void scanMusic() {
        playlist.clear();
        if (musicFolder != null && musicFolder.exists()) {
            File[] files = musicFolder.listFiles((dir, name) -> {
                String lower = name.toLowerCase();
                return lower.endsWith(".mp3") || lower.endsWith(".wav"); // JavaFX chơi được cả WAV
            });
            if (files != null) {
                for (File f : files) {
                    playlist.add(f);
                }
            }
        }
    }

    private void prepareTrack(int index) {
        if (playlist.isEmpty())
            return;

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose(); // Hủy bài cũ để giải phóng RAM
        }

        currentTrackIndex = index;
        File file = playlist.get(currentTrackIndex);

        // Biến đường dẫn file thành URL chuẩn cho JavaFX
        Media media = new Media(file.toURI().toString());
        mediaPlayer = new MediaPlayer(media);

        // Tự động chuyển bài khi hết nhạc!
        mediaPlayer.setOnEndOfMedia(this::next);

        // Báo cáo cho giao diện biết bài hát đã sẵn sàng (để lấy độ dài)
        mediaPlayer.setOnReady(() -> {
            if (onTrackChange != null)
                onTrackChange.run();
        });

        // Cảm biến nhịp đập: Liên tục báo cáo thời gian đang phát
        mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            if (onProgressUpdate != null)
                onProgressUpdate.run();
        });

        mediaPlayer.setVolume(0.5); // Mặc định âm lượng 50%
    }

    public void play() {
        if (mediaPlayer != null) {
            mediaPlayer.play();
            isPlaying = true;
        } else if (!playlist.isEmpty()) {
            prepareTrack(0);
            mediaPlayer.play();
            isPlaying = true;
        }
    }

    public void pause() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            isPlaying = false;
        }
    }

    public void togglePlayPause() {
        if (isPlaying)
            pause();
        else
            play();
    }

    public void next() {
        if (playlist.isEmpty())
            return;
        int nextIndex = (currentTrackIndex + 1) % playlist.size();
        prepareTrack(nextIndex);
        if (isPlaying)
            play();
    }

    public void previous() {
        if (playlist.isEmpty())
            return;
        int prevIndex = (currentTrackIndex - 1 + playlist.size()) % playlist.size();
        prepareTrack(prevIndex);
        if (isPlaying)
            play();
    }

    public String getCurrentTrackName() {
        if (playlist.isEmpty())
            return "Chưa có nhạc";
        return playlist.get(currentTrackIndex).getName();
    }

    // 👇 CÁC VŨ KHÍ BÍ MẬT DÀNH CHO THANH TRƯỢT GIAO DIỆN (UI) 👇

    public double getCurrentTimeSeconds() {
        if (mediaPlayer == null)
            return 0;
        return mediaPlayer.getCurrentTime().toSeconds();
    }

    public double getTotalDurationSeconds() {
        if (mediaPlayer == null || mediaPlayer.getMedia().getDuration().isUnknown())
            return 100;
        return mediaPlayer.getMedia().getDuration().toSeconds();
    }

    public void seek(double seconds) {
        if (mediaPlayer != null) {
            mediaPlayer.seek(Duration.seconds(seconds));
        }
    }

    public void setVolume(double volume) {
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(volume);
        }
    }

    public void setOnProgressUpdate(Runnable onProgressUpdate) {
        this.onProgressUpdate = onProgressUpdate;
    }

    public void setOnTrackChange(Runnable onTrackChange) {
        this.onTrackChange = onTrackChange;
    }
}