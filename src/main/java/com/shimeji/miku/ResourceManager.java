package com.shimeji.miku;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.net.URL;

/**
 * Trình quản lý tài nguyên tối ưu (Resource Manager).
 * Áp dụng kỹ thuật: Pre-scaling (Thu nhỏ ảnh ngay khi đọc) để tiết kiệm 95%
 * RAM.
 */
public class ResourceManager {

    // =================================================================
    // 1. CẤU HÌNH ĐƯỜNG DẪN & SỐ LƯỢNG FRAME
    // =================================================================
    private static final String BASE_DIR = "/images/";

    public static final int TOTAL_IDLE_FRAMES = 240;
    public static final int TOTAL_WALK_FRAMES = 24;
    public static final int TOTAL_DRAG_FRAMES = 12;
    public static final int TOTAL_FALL_FRAMES = 24;

    // CHUẨN OOP: Định nghĩa hằng số chiều cao nhân vật để thu nhỏ ảnh
    private static final int TARGET_HEIGHT = 150;

    // =================================================================
    // 2. KHO LƯU TRỮ TRONG BỘ NHỚ (Memory Cache)
    // =================================================================
    private static BufferedImage imgPaused;
    private static BufferedImage[] imgIdle;
    private static BufferedImage[] imgWalk;
    private static BufferedImage[] imgDrag;
    private static BufferedImage[] imgFall;

    // Không cho phép khởi tạo object này (Utility Class)
    private ResourceManager() {
    }

    // =================================================================
    // 3. LOGIC TẢI VÀ THU NHỎ ẢNH (Load & Scale)
    // =================================================================
    public static void loadImages() {
        System.out.println("Bắt đầu nạp và thu nhỏ tài nguyên (Tối ưu RAM)...");

        // 1. Tải ảnh Menu (Phao cứu sinh)
        imgPaused = loadImageAndScale("img1.png");

        // 2. Tải 240 frame chờ (Idle)
        imgIdle = new BufferedImage[TOTAL_IDLE_FRAMES];
        for (int i = 0; i < TOTAL_IDLE_FRAMES; i++) {
            imgIdle[i] = loadFrame("miku_idle/idle_frame_%04d.png", i + 1);
        }

        // 3. Tải 24 frame đi bộ (Walk)
        imgWalk = new BufferedImage[TOTAL_WALK_FRAMES];
        for (int i = 0; i < TOTAL_WALK_FRAMES; i++) {
            imgWalk[i] = loadFrame("miku_walk/miku_frame_%02d.png", i + 1);
        }

        // 4. Tải 12 frame bị kéo thả (Drag)
        imgDrag = new BufferedImage[TOTAL_DRAG_FRAMES];
        for (int i = 0; i < TOTAL_DRAG_FRAMES; i++) {
            imgDrag[i] = loadFrame("miku_drag/miku_fly_frame_%04d.png", i + 67);
        }

        // 5. Tải 24 frame rơi tự do (Fall)
        imgFall = new BufferedImage[TOTAL_FALL_FRAMES];
        for (int i = 0; i < TOTAL_FALL_FRAMES; i++) {
            imgFall[i] = loadFrame("miku_fall/miku_fly_frame_%04d.png", i + 91);
        }

        System.out.println("Nạp tài nguyên hoàn tất! RAM đã được tối ưu.");

        // Gọi Garbage Collector dọn dẹp các mảng byte rác sinh ra trong lúc resize ảnh
        System.gc();
    }

    private static BufferedImage loadFrame(String formatString, int index) {
        String relativePath = String.format(formatString, index);
        BufferedImage frame = loadImageAndScale(relativePath);
        return (frame != null) ? frame : imgPaused;
    }

    /**
     * Hàm cốt lõi chống Memory Leak: Đọc file gốc, nén nó nhỏ lại bằng
     * TARGET_HEIGHT,
     * lưu bản nén vào RAM và quăng bản gốc đi.
     */
    private static BufferedImage loadImageAndScale(String relativePath) {
        String fullPath = BASE_DIR + relativePath;

        try {
            URL url = ResourceManager.class.getResource(fullPath);
            if (url == null) {
                System.err.println("CẢNH BÁO: Không tìm thấy file " + fullPath);
                return null;
            }

            // 1. Đọc ảnh gốc nguyên bản vào RAM
            BufferedImage originalImage = ImageIO.read(url);
            if (originalImage == null)
                return null;

            // 2. Tính toán tỷ lệ để thu nhỏ chiều cao về TARGET_HEIGHT (150px)
            int origWidth = originalImage.getWidth();
            int origHeight = originalImage.getHeight();

            // Nếu ảnh gốc đã nhỏ hơn hoặc bằng 150px thì giữ nguyên, không cần thu nhỏ
            if (origHeight <= TARGET_HEIGHT) {
                return originalImage;
            }

            double ratio = (double) origWidth / origHeight;
            int targetWidth = (int) (TARGET_HEIGHT * ratio);

            // 3. Tạo một bức ảnh nhỏ (Thumbnail) trên RAM (Rất nhẹ)
            BufferedImage scaledImage = new BufferedImage(targetWidth, TARGET_HEIGHT, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = scaledImage.createGraphics();

            // Cấu hình chất lượng thu nhỏ siêu mượt (Hardware Acceleration)
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 4. Vẽ ép ảnh gốc to vào khung ảnh nhỏ
            g2d.drawImage(originalImage, 0, 0, targetWidth, TARGET_HEIGHT, null);
            g2d.dispose();

            // 5. Giải phóng ảnh khổng lồ nguyên bản khỏi bộ nhớ! (Quan trọng nhất)
            originalImage.flush();

            return scaledImage;

        } catch (Exception e) {
            System.err.println("LỖI: Không thể đọc file " + fullPath);
            return null;
        }
    }

    // =================================================================
    // 4. GETTER ĐỂ LẤY DỮ LIỆU TỪ VIEW (Encapsulation)
    // =================================================================
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
}