package com.adbmanager.view.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;

public class ThemedComboBoxUI extends BasicComboBoxUI {

    private final AppTheme theme;
    private JButton arrowButtonRef;
    private PropertyChangeListener comboStateListener;

    public ThemedComboBoxUI(AppTheme theme) {
        this.theme = theme;
    }

    @Override
    protected JButton createArrowButton() {
        JButton button = new JButton(new ArrowIcon(theme.textSecondary()));
        // Use centralized button styling via ButtonStyler instead of BasicButtonUI
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBackground(resolveSurfaceColor());
        button.setForeground(theme.textSecondary());
        button.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, resolveBorderColor()));
        button.setFocusPainted(false);
        button.setFocusable(false);
        button.setPreferredSize(new java.awt.Dimension(32, 32));
        arrowButtonRef = button;
        return button;
    }

    @Override
    protected void installListeners() {
        super.installListeners();
        comboStateListener = event -> {
            String propertyName = event.getPropertyName();
            if ("enabled".equals(propertyName)
                    || "editable".equals(propertyName)
                    || "renderer".equals(propertyName)) {
                updateStateColors();
            }
        };
        comboBox.addPropertyChangeListener(comboStateListener);
        updateStateColors();
    }

    @Override
    protected void uninstallListeners() {
        if (comboStateListener != null && comboBox != null) {
            comboBox.removePropertyChangeListener(comboStateListener);
            comboStateListener = null;
        }
        super.uninstallListeners();
    }

    @Override
    public void paintCurrentValueBackground(Graphics graphics, Rectangle bounds, boolean hasFocus) {
        graphics.setColor(resolveSurfaceColor());
        graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    @Override
    protected ComboPopup createPopup() {
        BasicComboPopup popup = new BasicComboPopup(comboBox) {
            @Override
            protected JScrollPane createScroller() {
                JScrollPane scroller = super.createScroller();
                scroller.getViewport().setBackground(theme.surface());
                scroller.setBorder(BorderFactory.createLineBorder(theme.border(), 1));
                if (scroller.getVerticalScrollBar() != null) {
                    scroller.getVerticalScrollBar().setUI(new ThemedScrollBarUI(theme));
                    scroller.getVerticalScrollBar().setBackground(theme.surface());
                }
                if (scroller.getHorizontalScrollBar() != null) {
                    scroller.getHorizontalScrollBar().setUI(new ThemedScrollBarUI(theme));
                    scroller.getHorizontalScrollBar().setBackground(theme.surface());
                }
                return scroller;
            }
        };

        popup.setBorder(BorderFactory.createLineBorder(theme.border(), 1));
        popup.getList().setBackground(theme.surface());
        popup.getList().setForeground(theme.textPrimary());
        popup.getList().setSelectionBackground(theme.selectionBackground());
        popup.getList().setSelectionForeground(theme.selectionForeground());
        return popup;
    }

    public static void apply(JComponent comboBox, AppTheme theme) {
        comboBox.setBackground(theme.secondarySurface());
        comboBox.setForeground(theme.textPrimary());
        comboBox.setOpaque(true);
        if (comboBox instanceof JComboBox<?> combo) {
            styleEditableEditor(combo, theme);
        }
    }

    public static void styleEditableEditor(JComboBox<?> comboBox, AppTheme theme) {
        if (!comboBox.isEditable() || !(comboBox.getEditor().getEditorComponent() instanceof JTextField editorField)) {
            return;
        }

        boolean enabled = comboBox.isEnabled();
        editorField.setOpaque(true);
        editorField.setEnabled(true);
        editorField.setEditable(enabled);
        editorField.setFocusable(enabled);
        editorField.setBackground(enabled ? theme.secondarySurface() : theme.surface());
        editorField.setForeground(enabled ? theme.textPrimary() : theme.textSecondary());
        editorField.setDisabledTextColor(theme.textSecondary());
        editorField.setCaretColor(enabled ? theme.textPrimary() : theme.textSecondary());
        editorField.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        editorField.setSelectionColor(theme.selectionBackground());
        editorField.setSelectedTextColor(theme.selectionForeground());
    }

    public static void applyRendererColors(
            DefaultListCellRenderer renderer,
            JList<?> list,
            AppTheme theme,
            boolean isSelected,
            int index) {
        boolean enabled = true;
        if (list instanceof JComponent component) {
            Object property = component.getClientProperty("combo.enabled");
            if (property instanceof Boolean comboEnabled) {
                enabled = comboEnabled;
            } else {
                enabled = list.isEnabled();
            }
        } else if (list != null) {
            enabled = list.isEnabled();
        }
        renderer.setOpaque(true);
        if (index == -1) {
            renderer.setBackground(enabled ? theme.secondarySurface() : theme.surface());
            renderer.setForeground(enabled ? theme.textPrimary() : theme.textSecondary());
            return;
        }

        renderer.setBackground(isSelected ? theme.selectionBackground() : theme.surface());
        renderer.setForeground(isSelected
                ? theme.selectionForeground()
                : (enabled ? theme.textPrimary() : theme.textSecondary()));
    }

    private void updateStateColors() {
        if (comboBox == null) {
            return;
        }

        comboBox.setBackground(resolveSurfaceColor());
        comboBox.setForeground(comboBox.isEnabled() ? theme.textPrimary() : theme.textSecondary());
        if (listBox instanceof JComponent component) {
            component.putClientProperty("combo.enabled", comboBox.isEnabled());
        }
        if (arrowButtonRef != null) {
            arrowButtonRef.setBackground(resolveSurfaceColor());
            arrowButtonRef.setForeground(theme.textSecondary());
            arrowButtonRef.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, resolveBorderColor()));
            arrowButtonRef.setIcon(new ArrowIcon(theme.textSecondary()));
        }
        styleEditableEditor(comboBox, theme);
        comboBox.repaint();
    }

    private Color resolveSurfaceColor() {
        return comboBox != null && comboBox.isEnabled() ? theme.secondarySurface() : theme.surface();
    }

    private Color resolveBorderColor() {
        return comboBox != null && comboBox.isEnabled() ? theme.border() : theme.disabledBorder();
    }

    private static final class ArrowIcon implements Icon {

        private final Color color;

        private ArrowIcon(Color color) {
            this.color = color;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g2d = (Graphics2D) graphics.create();
            g2d.translate(x, y);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(color);

            Polygon polygon = new Polygon(
                    new int[] { 1, 7, 13 },
                    new int[] { 3, 9, 3 },
                    3);
            g2d.fillPolygon(polygon);
            g2d.dispose();
        }

        @Override
        public int getIconWidth() {
            return 14;
        }

        @Override
        public int getIconHeight() {
            return 10;
        }
    }
}
