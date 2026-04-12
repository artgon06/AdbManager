package com.adbmanager.view.swing;

import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JTextArea;

public class WrappingTextArea extends JTextArea {

    public WrappingTextArea() {
        this("");
    }

    public WrappingTextArea(String text) {
        super(text);
        setEditable(false);
        setFocusable(false);
        setLineWrap(true);
        setWrapStyleWord(true);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder());
        setAlignmentX(LEFT_ALIGNMENT);
        setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
    }

    public void applyTheme(AppTheme theme, Font font, java.awt.Color foreground) {
        setBackground(theme.background());
        setForeground(foreground);
        setCaretColor(foreground);
        if (font != null) {
            setFont(font);
        }
    }

    @Override
    public Dimension getMaximumSize() {
        Dimension preferred = getPreferredSize();
        return new Dimension(Integer.MAX_VALUE, preferred.height);
    }

    @Override
    public Dimension getPreferredSize() {
        if (getParent() != null && getParent().getWidth() > 0) {
            int availableWidth = Math.max(120, getParent().getWidth());
            setSize(new Dimension(availableWidth, Short.MAX_VALUE));
        }
        return super.getPreferredSize();
    }
}
