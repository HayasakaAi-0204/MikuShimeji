package com.shimeji.miku;

import java.awt.image.BufferedImage;

public interface MikuState {
    void enter(MikuCharacter miku);

    void updatePhysics(MikuCharacter miku, int floorY);

    void updateAnimation(MikuCharacter miku);

    BufferedImage getCurrentImage();
}