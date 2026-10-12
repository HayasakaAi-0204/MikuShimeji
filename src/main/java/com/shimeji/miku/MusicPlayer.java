package com.shimeji.miku;

import java.io.File;
import java.util.prefs.Preferences;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public class MusicPlayer {
    private final Playlist playlist;
    private final Preferences prefs;
    private MediaPlayer mediaPlayer;

    private volatile boolean isPlaying = false;
    private volatile double currentVolume = 0.5;
    private volatile double lastKnownPosition = 0;
    private volatile double pendingSeek = -1;
    private boolean isFirstLoad = true;

    private Runnable onProgressUpdate;
    private Runnable onTrackChange;

    public MusicPlayer() {
        this.prefs = Preferences.userNodeForPackage(MusicPlayer.class);
        this.playlist = new Playlist(prefs);

        if (!playlist.isEmpty()) {
            this.lastKnownPosition = prefs.getDouble("savedPosition", 0);
            prepareTrack(playlist.getCurrentIndex());
        }

        Runtime.getRuntime().addShutdownHook(new Thread(this::saveState));
    }

    public void setMusicFolder(File folder) {
        if (folder != null && folder.exists() && folder.isDirectory()) {
            playlist.setFolder(folder);
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
        return playlist.getFolderName();
    }

    public void scanMusic() {
        playlist.scan();
    }

    private void prepareTrack(int index) {
        if (playlist.isEmpty())
            return;

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }

        playlist.setCurrentIndex(index);
        File file = playlist.getCurrentTrack();
        if (file == null)
            return;

        pendingSeek = -1;
        Media media = new Media(file.toURI().toString());
        mediaPlayer = new MediaPlayer(media);

        mediaPlayer.setVolume(0); // Tạm tắt tiếng để tránh tiếng bụp khi seek
        mediaPlayer.setOnEndOfMedia(this::autoNext);

        mediaPlayer.setOnReady(() -> {
            if (onTrackChange != null)
                onTrackChange.run();

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
                    } catch (Exception ignored) {
                    }
                } else {
                    mediaPlayer.setVolume(currentVolume);
                }
                isFirstLoad = false;
            } else {
                pendingSeek = -1;
                lastKnownPosition = 0;
                mediaPlayer.setVolume(currentVolume);
                if (onProgressUpdate != null)
                    onProgressUpdate.run();
            }
        });

        mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            if (pendingSeek >= 0) {
                if (newTime.toSeconds() > 0.05) {
                    if (pendingSeek > 0) {
                        mediaPlayer.seek(javafx.util.Duration.seconds(pendingSeek));
                    }
                    pendingSeek = -1;
                    mediaPlayer.setVolume(currentVolume);
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

    public void seek(double seconds) {
        if (mediaPlayer != null) {
            if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
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

    public void next() {
        if (playlist.isEmpty())
            return;
        prepareTrack(playlist.nextIndex());
        if (isPlaying)
            play();
    }

    public void previous() {
        if (playlist.isEmpty())
            return;
        prepareTrack(playlist.previousIndex());
        if (isPlaying)
            play();
    }

    public void autoNext() {
        if (playlist.isEmpty())
            return;
        if (playlist.isRepeatOne()) {
            if (mediaPlayer != null) {
                mediaPlayer.seek(javafx.util.Duration.ZERO);
                mediaPlayer.play();
                isPlaying = true;
            }
        } else {
            next();
        }
    }

    public String getCurrentTrackName() {
        return playlist.getCurrentTrackName();
    }

    public double getCurrentTimeSeconds() {
        if (pendingSeek >= 0)
            return pendingSeek;
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
        this.currentVolume = volume;
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(volume);
        }
    }

    public void setOnProgressUpdate(Runnable onProgressUpdate) {
        this.onProgressUpdate = onProgressUpdate;
        if (this.onProgressUpdate != null)
            this.onProgressUpdate.run();
    }

    public void setOnTrackChange(Runnable onTrackChange) {
        this.onTrackChange = onTrackChange;
        if (this.onTrackChange != null)
            this.onTrackChange.run();
    }

    public void saveState() {
        if (!playlist.isEmpty()) {
            prefs.put("savedTrackName", playlist.getCurrentTrackName());
            prefs.putDouble("savedPosition", lastKnownPosition);
            try {
                prefs.flush();
            } catch (Exception ignored) {
            }
        }
    }

    public boolean isShuffle() {
        return playlist.isShuffle();
    }

    public void setShuffle(boolean shuffle) {
        playlist.setShuffle(shuffle);
    }

    public boolean isRepeatOne() {
        return playlist.isRepeatOne();
    }

    public void setRepeatOne(boolean repeatOne) {
        playlist.setRepeatOne(repeatOne);
    }

    public boolean isPlaying() {
        return isPlaying;
    }
}