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
    public static final int TOTAL_DELETE_FRAMES = 236; 
    public static final int TOTAL_CLIMB_FRAMES = 32; 

    private static final int TARGET_HEIGHT = 150;

    // Giữ nguyên mức phóng to như bạn đang dùng
    private static final int CLIMB_TARGET_HEIGHT = 175; 
    private static final int CLIMB_PAUSE_TARGET_HEIGHT = 150; 

    private static BufferedImage imgPaused;
    private static BufferedImage imgClimbPause;

    private static BufferedImage[] imgIdle = new BufferedImage[TOTAL_IDLE_FRAMES];
    private static BufferedImage[] imgWalk = new BufferedImage[TOTAL_WALK_FRAMES];
    private static BufferedImage[] imgDrag = new BufferedImage[TOTAL_DRAG_FRAMES];
    private static BufferedImage[] imgFall = new BufferedImage[TOTAL_FALL_FRAMES];
    private static BufferedImage[] imgThrow = new BufferedImage[TOTAL_THROW_FRAMES];
    private static BufferedImage[] imgDelete = new BufferedImage[TOTAL_DELETE_FRAMES];
    private static BufferedImage[] imgClimb = new BufferedImage[TOTAL_CLIMB_FRAMES]; 

    private static BufferedImage imgLeek;

    private ResourceManager() {
    }

    public static void loadImages() {
        ImageIO.setUseCache(false);
        int pausedCharacterHeight = 135; 
        int shiftX = 0; 
        int shiftY = -5; 

        BufferedImage tempImg = loadImageAndScale("miku_idle_pause.png", pausedCharacterHeight);
        BufferedImage referenceFrame = getIdleImage(0);
        
        if (tempImg != null && referenceFrame != null) {
            imgPaused = new BufferedImage(referenceFrame.getWidth(), TARGET_HEIGHT, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = imgPaused.createGraphics();
            
            int drawX = (referenceFrame.getWidth() - tempImg.getWidth()) / 2 + shiftX;
            int drawY = (TARGET_HEIGHT - tempImg.getHeight()) + shiftY;
            
            g2d.drawImage(tempImg, drawX, drawY, null);
            g2d.dispose();
            
            if (tempImg != imgPaused) {
                tempImg.flush();
            }
        }

        imgLeek = loadFrame("leek.png", -1, -1);

        BufferedImage rawClimbPause = loadFrame("miku_climb_pause.png", -1, CLIMB_PAUSE_TARGET_HEIGHT);
        BufferedImage referenceClimb = getClimbImage(0); 
        
        if (rawClimbPause != null && referenceClimb != null) {
            imgClimbPause = new BufferedImage(referenceClimb.getWidth(), referenceClimb.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = imgClimbPause.createGraphics();
            
            // Giữ đúng tham số bạn thấy hợp lý
            int PAUSE_SHIFT_X = -50; 
            int PAUSE_SHIFT_Y = 0;
            
            g2d.drawImage(rawClimbPause, PAUSE_SHIFT_X, PAUSE_SHIFT_Y, null);
            g2d.dispose();
            
            if (rawClimbPause != imgClimbPause) {
                rawClimbPause.flush();
            }
        }
    }

    public static BufferedImage getClimbImage(int index) {
        if (imgClimb[index] == null) {
            imgClimb[index] = loadFrame("miku_climb/miku_climb_%04d.png", index + 1, CLIMB_TARGET_HEIGHT);
        }
        return imgClimb[index];
    }

    public static BufferedImage getClimbPauseImage() {
        return imgClimbPause;
    }

    public static BufferedImage getThrowImage(int index) {
        if (imgThrow[index] == null) {
            BufferedImage raw = loadFrame("miku_throw/miku_throw_%04d.png", index + 1, TARGET_HEIGHT);
            imgThrow[index] = flipImageHorizontal(raw);
        }
        return imgThrow[index];
    }

    public static BufferedImage getDeleteImage(int index) {
        if (imgDelete[index] == null) {
            BufferedImage raw = loadFrame("miku_delete/miku_delete_%04d.png", index + 1, TARGET_HEIGHT);
            imgDelete[index] = flipImageHorizontal(raw); 
        }
        return imgDelete[index];
    }

    public static BufferedImage getIdleImage(int index) {
        if (imgIdle[index] == null) {
            imgIdle[index] = loadFrame("miku_idle/miku_idle_%04d.png", index + 1, TARGET_HEIGHT);
        }
        return imgIdle[index];
    }

    public static BufferedImage getWalkImage(int index) {
        if (imgWalk[index] == null) {
            imgWalk[index] = loadFrame("miku_walk/miku_walk_%04d.png", index + 1, TARGET_HEIGHT);
        }
        return imgWalk[index];
    }

    public static BufferedImage getDragImage(int index) {
        if (imgDrag[index] == null) {
            imgDrag[index] = loadFrame("miku_drag/miku_drag_%04d.png", index + 1, TARGET_HEIGHT);
        }
        return imgDrag[index];
    }

    public static BufferedImage getFallImage(int index) {
        if (imgFall[index] == null) {
            imgFall[index] = loadFrame("miku_fall/miku_fall_%04d.png", index + 1, TARGET_HEIGHT);
        }
        return imgFall[index];
    }

    public static BufferedImage getPausedImage() {
        return imgPaused;
    }

    public static BufferedImage getLeekImage() {
        return imgLeek;
    }

    private static BufferedImage flipImageHorizontal(BufferedImage img) {
        if (img == null) return null;
        int w = img.getWidth();
        int h = img.getHeight();
        BufferedImage flipped = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = flipped.createGraphics();
        g.drawImage(img, w, 0, -w, h, null);
        g.dispose();
        
        img.flush(); 
        return flipped;
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
            if (url == null) return null;

            BufferedImage originalImage = ImageIO.read(url);
            if (originalImage == null) return null;

            if (targetHeight == -1) {
                BufferedImage cropped = autoCropAndFree(originalImage);
                if (cropped != originalImage) {
                    originalImage.flush();
                }
                return cropped;
            }

            int origWidth = originalImage.getWidth();
            int origHeight = originalImage.getHeight();
            if (origHeight == targetHeight) return originalImage;

            double ratio = (double) origWidth / origHeight;
            int targetWidth = (int) (targetHeight * ratio);

            BufferedImage scaledImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = scaledImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
            g2d.dispose();
            originalImage.flush();

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
                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
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
}