package com.shimeji.miku;

import javax.swing.Icon;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileSystemView;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.*;

public class DesktopWatcher {
    private MikuCharacter miku;
    private Thread watcherThread;
    private boolean isRunning = false;

    public DesktopWatcher(MikuCharacter miku) {
        this.miku = miku;
    }

    public void startWatching() {
        if (isRunning)
            return;
        isRunning = true;

        watcherThread = new Thread(() -> {
            try {
                Path desktopPath = resolveDesktopPath();
                WatchService watchService = FileSystems.getDefault().newWatchService();
                desktopPath.register(watchService, StandardWatchEventKinds.ENTRY_DELETE);

                System.out.println(">>> HeThong: Miku dang giam sat man hinh Desktop...");

                while (isRunning) {
                    WatchKey key = watchService.take();

                    for (WatchEvent<?> event : key.pollEvents()) {
                        if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                            Path deletedFile = (Path) event.context();
                            String fileName = deletedFile.toString();
                            String lowerName = fileName.toLowerCase();

                            // SỬA LỖI: Bỏ qua các file rác tàng hình của hệ điều hành Windows
                            // Tránh Miku tự nhiên ném hành vô cớ (Bắt ma)
                            if (lowerName.startsWith("~") || lowerName.endsWith(".tmp") ||
                                    lowerName.endsWith(".ini") || lowerName.endsWith(".crdownload")) {
                                continue;
                            }

                            BufferedImage fileIcon = getIconForDeletedFile(fileName);

                            System.out.println("----------------------------------------");
                            System.out.println("Miku phat hien ban vua xoa file: " + fileName);
                            System.out.println("----------------------------------------");

                            SwingUtilities.invokeLater(() -> {
                                miku.triggerDeleteAction(fileName, fileIcon);
                            });
                        }
                    }
                    key.reset();
                }
            } catch (Exception e) {
                System.out.println("Loi he thong theo doi: " + e.getMessage());
            }
        });

        watcherThread.setDaemon(true);
        watcherThread.start();
    }

    private BufferedImage getIconForDeletedFile(String fileName) {
        try {
            int dotIndex = fileName.lastIndexOf('.');
            String extension = (dotIndex > 0) ? fileName.substring(dotIndex) : ".unknown";

            File tempFile = File.createTempFile("miku_temp", extension);
            Icon icon = FileSystemView.getFileSystemView().getSystemIcon(tempFile);
            tempFile.delete();

            if (icon != null) {
                BufferedImage bImg = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(),
                        BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = bImg.createGraphics();
                icon.paintIcon(null, g, 0, 0);
                g.dispose();
                return bImg;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private Path resolveDesktopPath() {
        try {
            // Cách 1: Hỏi trực tiếp Windows Shell qua FileSystemView (chuẩn xác nhất)
            File homeDir = FileSystemView.getFileSystemView().getHomeDirectory();
            if (homeDir != null && homeDir.exists()) {
                return homeDir.toPath();
            }
        } catch (Exception ignored) {
        }

        // Cách 2: Dự phòng nếu máy dùng OneDrive Desktop
        Path oneDriveDesktop = Paths.get(System.getProperty("user.home"), "OneDrive", "Desktop");
        if (Files.exists(oneDriveDesktop)) {
            return oneDriveDesktop;
        }

        // Cách 3: Đường dẫn mặc định truyền thống
        return Paths.get(System.getProperty("user.home"), "Desktop");
    }
}