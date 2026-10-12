package com.shimeji.miku;

import java.util.ResourceBundle;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

public class ModeMenu extends JMenu {

    public ModeMenu(MikuCharacter miku, ResourceBundle lang, JPopupMenu swingPopup) {
        super(lang.getString("menu.mode"));

        JCheckBoxMenuItem casualItem = new JCheckBoxMenuItem(lang.getString("menu.casual"), true);
        JCheckBoxMenuItem gamingItem = new JCheckBoxMenuItem(lang.getString("menu.gaming"));
        JCheckBoxMenuItem workingItem = new JCheckBoxMenuItem(lang.getString("menu.working"));

        casualItem.addActionListener(e -> {
            gamingItem.setSelected(false);
            workingItem.setSelected(false);
            casualItem.setSelected(true);
            miku.setAppMode(MikuCharacter.AppMode.CASUAL);
            SwingUtilities.invokeLater(swingPopup::repaint);
        });

        gamingItem.addActionListener(e -> {
            casualItem.setSelected(false);
            workingItem.setSelected(false);
            gamingItem.setSelected(true);
            miku.setAppMode(MikuCharacter.AppMode.GAMING);
            SwingUtilities.invokeLater(swingPopup::repaint);
        });

        workingItem.addActionListener(e -> {
            casualItem.setSelected(false);
            gamingItem.setSelected(false);
            workingItem.setSelected(true);
            miku.setAppMode(MikuCharacter.AppMode.WORKING);
            SwingUtilities.invokeLater(swingPopup::repaint);
        });

        MikuMenu.keepMenuOpen(casualItem, gamingItem, workingItem);

        add(casualItem);
        add(gamingItem);
        add(workingItem);
    }
}