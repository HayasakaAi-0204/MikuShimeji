package com.shimeji.miku;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.prefs.Preferences;

public class Playlist {
    private final List<File> tracks = new ArrayList<>();
    private final List<Integer> history = Collections.synchronizedList(new ArrayList<>());
    private int currentIndex = 0;
    private File musicFolder;
    private boolean isShuffle = false;
    private boolean isRepeatOne = false;
    private final Preferences prefs;

    public Playlist(Preferences prefs) {
        this.prefs = prefs;
        this.isShuffle = prefs.getBoolean("isShuffle", false);
        this.isRepeatOne = prefs.getBoolean("isRepeatOne", false);

        String savedPath = prefs.get("musicFolderPath", null);
        if (savedPath != null) {
            musicFolder = new File(savedPath);
            scan();
            if (!tracks.isEmpty()) {
                String savedTrack = prefs.get("savedTrackName", "");
                for (int i = 0; i < tracks.size(); i++) {
                    if (tracks.get(i).getName().equals(savedTrack)) {
                        currentIndex = i;
                        break;
                    }
                }
            }
        }
    }

    public void scan() {
        tracks.clear();
        if (musicFolder != null && musicFolder.exists()) {
            File[] files = musicFolder.listFiles((dir, name) -> {
                String lower = name.toLowerCase();
                return lower.endsWith(".mp3") || lower.endsWith(".wav");
            });
            if (files != null) {
                for (File f : files) {
                    tracks.add(f);
                }
            }
        }
    }

    public void setFolder(File folder) {
        if (folder != null && folder.exists() && folder.isDirectory()) {
            history.clear();
            this.musicFolder = folder;
            prefs.put("musicFolderPath", folder.getAbsolutePath());
            scan();
            currentIndex = 0;
        }
    }

    public int nextIndex() {
        if (tracks.isEmpty())
            return 0;

        // Lưu bài hiện tại vào lịch sử (tối đa 50 bài)
        history.add(currentIndex);
        if (history.size() > 50) {
            history.remove(0);
        }

        if (isShuffle && tracks.size() > 1) {
            int next = currentIndex;
            while (next == currentIndex) {
                next = (int) (Math.random() * tracks.size());
            }
            currentIndex = next;
        } else {
            currentIndex = (currentIndex + 1) % tracks.size();
        }
        return currentIndex;
    }

    public int previousIndex() {
        if (tracks.isEmpty())
            return 0;

        if (!history.isEmpty()) {
            currentIndex = history.remove(history.size() - 1);
        } else {
            currentIndex = (currentIndex - 1 + tracks.size()) % tracks.size();
        }
        return currentIndex;
    }

    public File getCurrentTrack() {
        if (tracks.isEmpty() || currentIndex < 0 || currentIndex >= tracks.size()) {
            return null;
        }
        return tracks.get(currentIndex);
    }

    public String getCurrentTrackName() {
        File current = getCurrentTrack();
        return (current != null) ? current.getName() : "Chưa có nhạc";
    }

    public String getFolderName() {
        return (musicFolder != null) ? musicFolder.getName() : "Chưa chọn";
    }

    public boolean isEmpty() {
        return tracks.isEmpty();
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(int index) {
        if (index >= 0 && index < tracks.size()) {
            this.currentIndex = index;
        }
    }

    public boolean isShuffle() {
        return isShuffle;
    }

    public void setShuffle(boolean shuffle) {
        this.isShuffle = shuffle;
        prefs.putBoolean("isShuffle", shuffle);
    }

    public boolean isRepeatOne() {
        return isRepeatOne;
    }

    public void setRepeatOne(boolean repeatOne) {
        this.isRepeatOne = repeatOne;
        prefs.putBoolean("isRepeatOne", repeatOne);
    }
}