package com.shimeji.miku;

import javax.sound.sampled.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;

public class MusicPlayer {
    private File musicFolder;
    private List<File> playlist;
    private int currentIndex = 0;

    private boolean isPlaying = false;
    private boolean isPaused = false;
    private boolean stopRequested = false;
    private Thread playThread;
    private SourceDataLine line;

    private Preferences prefs;

    // Khởi tạo thư mục và quét nhạc
    public MusicPlayer() {
        playlist = new ArrayList<>();
        // Sử dụng Preferences để hệ thống tự động ghi nhớ thư mục bạn đã chọn
        prefs = Preferences.userNodeForPackage(MusicPlayer.class);

        // Đọc đường dẫn đã lưu, nếu chưa có thì lấy mặc định là thư mục Music của
        // Windows
        String savedPath = prefs.get("MusicFolderPath",
                new File(System.getProperty("user.home"), "Music").getAbsolutePath());
        musicFolder = new File(savedPath);

        scanMusic();
    }

    // 👉 HÀM MỚI ĐỂ ĐỔI THƯ MỤC NHẠC
    public void setMusicFolder(File newFolder) {
        if (newFolder != null && newFolder.isDirectory()) {
            this.musicFolder = newFolder;
            prefs.put("MusicFolderPath", newFolder.getAbsolutePath()); // Lưu lại cấu hình để Miku nhớ mãi mãi
            scanMusic();
            currentIndex = 0; // Đặt lại bài hát về bài đầu tiên
            if (isPlaying) {
                stop();
                play(); // Đang hát thì tự động ngắt và phát luôn nhạc ở thư mục mới
            }
        }
    }

    public void scanMusic() {
        playlist.clear();
        if (musicFolder.exists() && musicFolder.isDirectory()) {
            File[] files = musicFolder.listFiles((dir, name) -> {
                String lower = name.toLowerCase();
                return lower.endsWith(".mp3") || lower.endsWith(".wav");
            });
            if (files != null) {
                playlist.addAll(Arrays.asList(files));
            }
        }
    }

    public void play() {
        if (playlist.isEmpty())
            return;

        // 👉 SỬA BUG: Nếu đang ở cuối danh sách mà bấm Play thì quay vòng lại bài đầu
        // tiên
        if (currentIndex >= playlist.size()) {
            currentIndex = 0;
        }

        if (isPaused && line != null) {
            // ... (Phần code cũ) // Đang tạm dừng thì phát tiếp (giữ nguyên vị trí bài hát)
            isPaused = false;
            line.start();
            return;
        }

        if (isPlaying) {
            stop(); // Nếu đang hát bài khác thì dừng bài cũ
        }

        isPlaying = true;
        stopRequested = false;

        // Mở luồng chạy ngầm để Miku không bị đứng hình khi hát
        playThread = new Thread(() -> {
            while (isPlaying && currentIndex < playlist.size() && !stopRequested) {
                playFile(playlist.get(currentIndex));
                if (!stopRequested) {
                    currentIndex++; // Hát xong tự nhảy qua bài tiếp theo
                }
            }
            isPlaying = false;
        });
        playThread.setDaemon(true);
        playThread.start();
    }

    public void pause() {
        if (isPlaying && !isPaused && line != null) {
            isPaused = true;
            line.stop(); // Tạm khóa họng, ngừng đẩy dữ liệu ra loa
        }
    }

    public void stop() {
        stopRequested = true;
        isPaused = false;
        if (line != null) {
            line.stop();
            line.close();
        }
        if (playThread != null) {
            try {
                playThread.join(500);
            } catch (Exception e) {
            }
        }
        isPlaying = false;
    }

    public void next() {
        stop();
        currentIndex++;
        if (currentIndex >= playlist.size()) {
            currentIndex = 0; // Quay lại bài số 1 nếu đã hết danh sách
        }
        play();
    }

    public void prev() {
        stop();
        currentIndex--;
        if (currentIndex < 0) {
            currentIndex = playlist.size() - 1; // Nhảy ngược về bài cuối cùng
        }
        play();
    }

    public String getCurrentTrackName() {
        if (playlist.isEmpty())
            return "Thư mục Music trống!";
        if (currentIndex >= 0 && currentIndex < playlist.size()) {
            return playlist.get(currentIndex).getName();
        }
        return "Unknown";
    }

    private void playFile(File file) {
        try (AudioInputStream in = AudioSystem.getAudioInputStream(file)) {
            AudioFormat baseFormat = in.getFormat();

            // Ép giải mã MP3 sang định dạng chuẩn PCM (chưa nén) để loa có thể hiểu
            AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.getSampleRate(),
                    16,
                    baseFormat.getChannels(),
                    baseFormat.getChannels() * 2,
                    baseFormat.getSampleRate(),
                    false);

            try (AudioInputStream din = AudioSystem.getAudioInputStream(decodedFormat, in)) {
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, decodedFormat);
                line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(decodedFormat);
                line.start();

                byte[] data = new byte[4096];
                int bytesRead = 0;

                // Thuật toán Streaming: Vừa đọc ổ cứng vừa bắn ra loa liên tục từng đoạn 4KB
                while (!stopRequested && bytesRead != -1) {
                    if (isPaused) {
                        Thread.sleep(100); // Ngủ đông khi bị Pause để tiết kiệm CPU
                        continue;
                    }
                    bytesRead = din.read(data, 0, data.length);
                    if (bytesRead >= 0) {
                        line.write(data, 0, bytesRead);
                    }
                }

                if (stopRequested) {
                    line.flush(); // Nếu bị Next bài, xả bỏ bộ đệm ngay lập tức
                } else {
                    line.drain(); // Chờ phát cho hết nốt nhạc cuối cùng của bài hát
                }

                line.stop();
                line.close();
            }
        } catch (Exception e) {
            System.out.println("Lỗi phát nhạc bài " + file.getName() + ": " + e.getMessage());
        }
    }

    public void togglePlayPause() {
        if (isPlaying && !isPaused) {
            pause();
        } else {
            play();
        }
    }

    // 👉 HÀM LẤY TÊN THƯ MỤC ĐANG PHÁT
    public String getMusicFolderName() {
        if (musicFolder != null) {
            return musicFolder.getName(); // Chỉ lấy tên thư mục ngắn gọn cho đẹp (VD: Nhac)
        }
        return "Mặc định";
    }
}