package com.shimeji.miku;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.net.URL;

public class ResourceManager {

    // Chuẩn OOP 1: Cấu hình hằng số thư mục và số lượng frame
    public static final int TOTAL_WALK_FRAMES = 24;
    private static final String WALK_DIR = "/images/miku_walk/";

    public static final int TOTAL_DRAG_FRAMES = 12;
    private static final String DRAG_DIR = "/images/miku_drag/";

    public static final int TOTAL_FALL_FRAMES = 24;
    private static final String FALL_DIR = "/images/miku_fall/";

    // THÊM MỚI 1: Cấu hình cho mảng hoạt ảnh chờ (Idle)
    public static final int TOTAL_IDLE_FRAMES = 240;
    private static final String IDLE_DIR = "/images/miku_idle/";

    // SỬA ĐỔI 1: Tách biệt rõ ràng 2 khái niệm (Semantic Naming)
    private static BufferedImage imgPaused;     // Ảnh tĩnh (img1.png) dùng khi mở menu
    private static BufferedImage[] imgIdle;     // Mảng 240 ảnh động dùng khi rảnh rỗi

    private static BufferedImage[] imgWalk;
    private static BufferedImage[] imgDrag;
    private static BufferedImage[] imgFall;

    public static void loadImages() {
        // 1. Tải ảnh tĩnh (img1.png) - Đổi tên biến thành imgPaused
        imgPaused = loadImageSafely("/images/img1.png");

        // THÊM MỚI 2: Tải 240 khung hình chờ (Idle)
        imgIdle = new BufferedImage[TOTAL_IDLE_FRAMES];
        for (int i = 0; i < TOTAL_IDLE_FRAMES; i++) {
            // Tên file của bạn là idle_frame_0001.png đến 0240.png
            String fileName = IDLE_DIR + String.format("idle_frame_%04d.png", i + 1);
            BufferedImage frame = loadImageSafely(fileName);
            // Dùng imgPaused làm phao cứu sinh (fallback) nếu lỗi load frame
            imgIdle[i] = (frame != null) ? frame : imgPaused; 
        }

        // 2. Tải khung hình bị nhấc lên (Drag/Fly)
        imgDrag = new BufferedImage[TOTAL_DRAG_FRAMES];
        for (int i = 0; i < TOTAL_DRAG_FRAMES; i++) {
            String fileName = DRAG_DIR + String.format("miku_fly_frame_%04d.png", i + 67);
            BufferedImage frame = loadImageSafely(fileName);
            imgDrag[i] = (frame != null) ? frame : imgPaused;
        }

        // 3. Tải khung hình đi bộ (Walk)
        imgWalk = new BufferedImage[TOTAL_WALK_FRAMES];
        for (int i = 0; i < TOTAL_WALK_FRAMES; i++) {
            String fileName = WALK_DIR + String.format("miku_frame_%02d.png", i + 1);
            BufferedImage frame = loadImageSafely(fileName);
            imgWalk[i] = (frame != null) ? frame : imgPaused;
        }

        // 4. Tải khung hình rơi (Fall)
        imgFall = new BufferedImage[TOTAL_FALL_FRAMES];
        for (int i = 0; i < TOTAL_FALL_FRAMES; i++) {
            String fileName = FALL_DIR + String.format("miku_fly_frame_%04d.png", i + 91);
            BufferedImage frame = loadImageSafely(fileName);
            imgFall[i] = (frame != null) ? frame : imgPaused;
        }
    }

    private static BufferedImage loadImageSafely(String path) {
        try {
            URL url = ResourceManager.class.getResource(path);
            if (url == null) {
                System.err.println("CANH BAO: Khong tim thay file " + path);
                return null;
            }
            return ImageIO.read(url);
        } catch (Exception e) {
            System.err.println("LOI: Khong the doc file " + path);
            return null;
        }
    }

    // SỬA ĐỔI 2: Cung cấp Getter cho ảnh Menu tĩnh
    public static BufferedImage getPausedImage() {
        return imgPaused;
    }

    // SỬA ĐỔI 3: Getter của Idle giờ trả về dạng Mảng (Array)
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