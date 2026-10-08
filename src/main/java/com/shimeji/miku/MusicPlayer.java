package com.shimeji.miku;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

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
    private double currentVolume = 0.5;
    private double lastKnownPosition = 0; // Sổ tay an toàn
    private double pendingSeek = -1;
    private boolean isFirstLoad = true; // THÊM CỜ NÀY: Nhận biết lần đầu mở app

    // 👉 THÊM 2 BIẾN NÀY DÀNH CHO TÍNH NĂNG TRỘN BÀI
    private boolean isShuffle = false;
    private List<Integer> history = new ArrayList<>();

    // Cảm biến để báo cáo thời gian thực cho Thanh trượt giao diện
    private Runnable onProgressUpdate;
    private Runnable onTrackChange;

    public MusicPlayer() {
        prefs = Preferences.userNodeForPackage(MusicPlayer.class);
        isShuffle = prefs.getBoolean("isShuffle", false); // 👉 Thêm dòng này
        playlist = new ArrayList<>();
        String savedPath = prefs.get("musicFolderPath", null);
        if (savedPath != null) {
            musicFolder = new File(savedPath);
            scanMusic();
            if (!playlist.isEmpty()) {
                String savedTrack = prefs.get("savedTrackName", "");
                int index = 0;
                for (int i = 0; i < playlist.size(); i++) {
                    if (playlist.get(i).getName().equals(savedTrack)) {
                        index = i;
                        break;
                    }
                }

                lastKnownPosition = prefs.getDouble("savedPosition", 0);
                prepareTrack(index);
            }
        }

        // Kịch bản thoát app
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!playlist.isEmpty()) {
                prefs.put("savedTrackName", playlist.get(currentTrackIndex).getName());
                prefs.putDouble("savedPosition", lastKnownPosition);

                // 👈 BÙA CHÚ CHỐNG SẬP NGUỒN: Bắt buộc Windows phải ghi dữ liệu ngay lập tức!
                try {
                    prefs.flush();
                } catch (Exception e) {
                }
            }
        }));
    }

    public void setMusicFolder(File folder) {
        if (folder != null && folder.exists() && folder.isDirectory()) {
            history.clear(); // 👉 Xóa sạch lịch sử cũ khi đổi thư mục mới
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
            mediaPlayer.dispose();
        }

        currentTrackIndex = index;
        File file = playlist.get(currentTrackIndex);
        pendingSeek = -1;

        Media media = new Media(file.toURI().toString());
        mediaPlayer = new MediaPlayer(media);

        // Tạm thời TẮT TIẾNG để làm ảo thuật lừa JavaFX
        mediaPlayer.setVolume(0);

        mediaPlayer.setOnEndOfMedia(this::next);

        mediaPlayer.setOnReady(() -> {
            if (onTrackChange != null)
                onTrackChange.run();

            // CHỈ phục hồi thời gian lúc Miku vừa thức dậy (Lần mở app đầu tiên)
            if (isFirstLoad) {
                double savedPos = prefs.getDouble("savedPosition", -1);
                if (savedPos >= 0) {
                    pendingSeek = savedPos;
                    lastKnownPosition = savedPos;
                    if (onProgressUpdate != null)
                        onProgressUpdate.run();

                    prefs.putDouble("savedPosition", -1);
                    try {
                        prefs.flush();
                    } catch (Exception e) {
                    }
                } else {
                    mediaPlayer.setVolume(currentVolume);
                }
                isFirstLoad = false; // Đánh dấu là đã thức dậy xong!
            } else {
                // Những lần chuyển bài sau đó: Dọn dẹp sạch sẽ bóng ma bài cũ
                pendingSeek = -1;
                lastKnownPosition = 0;
                mediaPlayer.setVolume(currentVolume); // Mở tiếng
                if (onProgressUpdate != null)
                    onProgressUpdate.run(); // Ép giao diện reset về 00:00 ngay lập tức
            }
        });

        // 👈 TUYỆT CHIÊU CUỐI: Đợi đồng hồ chạy qua 0.05 giây rồi mới tua để chống kẹt!
        mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            if (pendingSeek >= 0) {
                if (newTime.toSeconds() > 0.05) {
                    if (pendingSeek > 0) {
                        mediaPlayer.seek(javafx.util.Duration.seconds(pendingSeek));
                    }
                    pendingSeek = -1;
                    mediaPlayer.setVolume(currentVolume); // Mở lại tiếng!
                }
            } else {
                lastKnownPosition = newTime.toSeconds();
                if (onProgressUpdate != null)
                    onProgressUpdate.run();
            }
        });
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

    public void seek(double seconds) {
        if (mediaPlayer != null) {
            if (mediaPlayer.getStatus() == javafx.scene.media.MediaPlayer.Status.PLAYING) {
                mediaPlayer.seek(javafx.util.Duration.seconds(seconds));
                pendingSeek = -1;
            } else {
                pendingSeek = seconds;
                lastKnownPosition = seconds;
                if (onProgressUpdate != null)
                    onProgressUpdate.run();
            }
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

        // 👉 Bước 1: Lưu bài hiện tại vào lịch sử (Tối đa 50 bài để không nặng máy)
        history.add(currentTrackIndex);
        if (history.size() > 50)
            history.remove(0);

        // 👉 Bước 2: Chọn bài mới
        int nextIndex;
        if (isShuffle && playlist.size() > 1) {
            nextIndex = currentTrackIndex;
            // Bốc thăm ngẫu nhiên đến khi ra bài mới khác bài cũ
            while (nextIndex == currentTrackIndex) {
                nextIndex = (int) (Math.random() * playlist.size());
            }
        } else {
            // Chế độ bình thường: Tiến 1 bài
            nextIndex = (currentTrackIndex + 1) % playlist.size();
        }

        prepareTrack(nextIndex);
        if (isPlaying)
            play();
    }

    public void previous() {
        if (playlist.isEmpty())
            return;

        int prevIndex;
        if (!history.isEmpty()) {
            // 👉 Trích xuất bài cuối cùng trong Lịch sử ra để quay về đúng bài đó
            prevIndex = history.remove(history.size() - 1);
        } else {
            // 👉 Nếu Lịch sử trống thì đành lùi 1 bài theo danh sách
            prevIndex = (currentTrackIndex - 1 + playlist.size()) % playlist.size();
        }

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
        if (pendingSeek >= 0) {
            return pendingSeek;
        }
        if (mediaPlayer == null)
            return 0;
        return mediaPlayer.getCurrentTime().toSeconds();
    }

    public double getTotalDurationSeconds() {
        if (mediaPlayer == null || mediaPlayer.getMedia().getDuration().isUnknown())
            return 100;
        return mediaPlayer.getMedia().getDuration().toSeconds();
    }

    public void setVolume(double volume) {
        this.currentVolume = volume; // Giữ mức này để lúc chuyển bài nó lôi ra dùng lại
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(volume);
        }
    }

    public void setOnProgressUpdate(Runnable onProgressUpdate) {
        this.onProgressUpdate = onProgressUpdate;
        // Bắt giao diện cập nhật ngay lập tức sau khi vừa vẽ xong để không bị "mù" số
        // ảo
        if (this.onProgressUpdate != null) {
            this.onProgressUpdate.run();
        }
    }

    public void setOnTrackChange(Runnable onTrackChange) {
        this.onTrackChange = onTrackChange;
        // Tương tự, ép cập nhật luôn tên bài hát lúc vừa mở app
        if (this.onTrackChange != null) {
            this.onTrackChange.run();
        }
    }

    public void saveState() {
        if (!playlist.isEmpty()) {
            prefs.put("savedTrackName", playlist.get(currentTrackIndex).getName());
            prefs.putDouble("savedPosition", lastKnownPosition);
            try {
                prefs.flush();
            } catch (Exception e) {
            }
        }
    }

    public boolean isShuffle() {
        return isShuffle;
    }

    public void setShuffle(boolean shuffle) {
        this.isShuffle = shuffle;
        prefs.putBoolean("isShuffle", shuffle);
    }
}