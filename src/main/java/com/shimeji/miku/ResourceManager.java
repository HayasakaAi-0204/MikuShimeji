package com.shimeji.miku;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.net.URL;

public class ResourceManager {

    private static final String BASE_DIR = "/images/";

    public static final int TOTAL_IDLE_FRAMES = 240;
    public static final int TOTAL_WALK_FRAMES = 24;
    public static final int TOTAL_DRAG_FRAMES = 12;
    public static final int TOTAL_FALL_FRAMES = 24;
    public static final int TOTAL_THROW_FRAMES = 212;

    private static final int TARGET_HEIGHT = 150;

    private static BufferedImage imgPaused;
    private static BufferedImage[] imgIdle;
    private static BufferedImage[] imgWalk;
    private static BufferedImage[] imgDrag;
    private static BufferedImage[] imgFall;
    private static BufferedImage[] imgThrow;

    private static BufferedImage imgLeek;

    private ResourceManager() {
    }

    public static void loadImages() {
        System.out.println("Starting to load and minimize resources...");

        imgPaused = loadImageAndScale("img1.png", TARGET_HEIGHT);

        // Nạp từng bộ ảnh và ÉP DỌN RÁC (Batched GC) để tránh phình RAM
        imgIdle = new BufferedImage[TOTAL_IDLE_FRAMES];
        for (int i = 0; i < TOTAL_IDLE_FRAMES; i++) {
            imgIdle[i] = loadFrame("miku_idle/idle_frame_%04d.png", i + 1, TARGET_HEIGHT);
        }
        forceGarbageCollection(); // Dọn rác ngay sau khi nạp xong đợt 1

        imgWalk = new BufferedImage[TOTAL_WALK_FRAMES];
        for (int i = 0; i < TOTAL_WALK_FRAMES; i++) {
            imgWalk[i] = loadFrame("miku_walk/miku_frame_%02d.png", i + 1, TARGET_HEIGHT);
        }

        imgDrag = new BufferedImage[TOTAL_DRAG_FRAMES];
        for (int i = 0; i < TOTAL_DRAG_FRAMES; i++) {
            imgDrag[i] = loadFrame("miku_drag/miku_fly_frame_%04d.png", i + 67, TARGET_HEIGHT);
        }

        imgFall = new BufferedImage[TOTAL_FALL_FRAMES];
        for (int i = 0; i < TOTAL_FALL_FRAMES; i++) {
            imgFall[i] = loadFrame("miku_fall/miku_fly_frame_%04d.png", i + 91, TARGET_HEIGHT);
        }
        forceGarbageCollection(); // Dọn rác sau đợt 2

        imgThrow = new BufferedImage[TOTAL_THROW_FRAMES];
        for (int i = 0; i < TOTAL_THROW_FRAMES; i++) {
            imgThrow[i] = loadFrame("miku_throw/miku_throw_%04d.png", i + 1, TARGET_HEIGHT);
        }

        // Tải cọng hành và kích hoạt Auto-Crop
        imgLeek = loadFrame("throw_frame_leek_0192.png", -1, -1);

        System.out.println("Resource loading complete!");
        forceGarbageCollection(); // Dọn dẹp sạch sẽ lần cuối cùng
    }

    // Hàm cưỡng chế dọn rác
    private static void forceGarbageCollection() {
        System.gc();
        System.runFinalization();
    }

    private static BufferedImage loadFrame(String formatString, int index, int targetHeight) {
        String relativePath = (index == -1) ? formatString : String.format(formatString, index);
        BufferedImage frame = loadImageAndScale(relativePath, targetHeight);
        return (frame != null) ? frame : imgPaused;
    }

    private static BufferedImage loadImageAndScale(String relativePath, int targetHeight) {
        String fullPath = BASE_DIR + relativePath;

        try {
            URL url = ResourceManager.class.getResource(fullPath);
            if (url == null)
                return null;

            // 1. Đọc ảnh gốc vào RAM
            BufferedImage originalImage = ImageIO.read(url);
            if (originalImage == null)
                return null;

            // 2. Cắt cọng hành (Nếu có)
            if (targetHeight == -1) {
                BufferedImage cropped = autoCropAndFree(originalImage);
                // Nếu hàm autoCrop trả về một ảnh MỚI, ta phải dọn sạch ảnh GỐC cũ
                if (cropped != originalImage) {
                    originalImage.flush();
                    originalImage = null;
                }
                return cropped;
            }

            int origWidth = originalImage.getWidth();
            int origHeight = originalImage.getHeight();

            if (origHeight <= targetHeight) {
                return originalImage;
            }

            double ratio = (double) origWidth / origHeight;
            int targetWidth = (int) (targetHeight * ratio);

            // 3. Tạo ảnh thu nhỏ mới bằng hệ màu TỐI ƯU NHẤT CHO RAM
            BufferedImage scaledImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = scaledImage.createGraphics();

            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
            g2d.dispose(); // Giải phóng cọ vẽ ngay lập tức

            // 4. DỌN SẠCH ẢNH GỐC KHỔNG LỒ KHỎI RAM
            originalImage.flush();
            originalImage = null; // Cực kỳ quan trọng: Ép tham chiếu về null để GC dọn ngay

            return scaledImage;

        } catch (Exception e) {
            return null;
        }
    }

    private static BufferedImage autoCropAndFree(BufferedImage source) {
        int minX = source.getWidth(), minY = source.getHeight(), maxX = 0, maxY = 0;
        boolean found = false;

        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                if (((source.getRGB(x, y) >> 24) & 0xff) > 0) {
                    if (x < minX)
                        minX = x;
                    if (y < minY)
                        minY = y;
                    if (x > maxX)
                        maxX = x;
                    if (y > maxY)
                        maxY = y;
                    found = true;
                }
            }
        }

        if (found) {
            int w = maxX - minX + 1;
            int h = maxY - minY + 1;
            BufferedImage cropped = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = cropped.createGraphics();
            g.drawImage(source, 0, 0, w, h, minX, minY, maxX + 1, maxY + 1, null);
            g.dispose();

            return cropped;
        }
        return source;
    }

    public static BufferedImage getPausedImage() {
        return imgPaused;
    }

    public static BufferedImage[] getIdleImages() {
        return imgIdle;
    }

    public static BufferedImage[] getWalkImages() {
        return imgWalk;
    }

    public static BufferedImage[] getDragImages() {
        return imgDrag;
    }

    public static BufferedImage[] getFallImages() {
        return imgFall;
    }

    public static BufferedImage[] getThrowImages() {
        return imgThrow;
    }

    public static BufferedImage getLeekImage() {
        return imgLeek;
    }
}