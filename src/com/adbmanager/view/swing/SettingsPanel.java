package com.adbmanager.view.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicToggleButtonUI;

import com.adbmanager.view.Messages;
import com.adbmanager.view.Messages.Language;

public class SettingsPanel extends JPanel {

    private final JLabel titleLabel = new JLabel();
    private final JLabel subtitleLabel = new JLabel();
    private final JPanel aboutPanel = new JPanel();
    private final JPanel appearancePanel = new JPanel();
    private final JPanel behaviorPanel = new JPanel();

    private final JLabel appNameValue = new JLabel();
    private final JLabel versionBadge = new JLabel();
    private final JButton repositoryButton = new JButton();

    private final JLabel themeLabel = new JLabel();
    private final JToggleButton lightThemeButton = new JToggleButton();
    private final JToggleButton darkThemeButton = new JToggleButton();
    private final JLabel languageLabel = new JLabel();
    private final JComboBox<Language> languageCombo = new JComboBox<>(Language.values());
    private final LanguageRenderer languageRenderer = new LanguageRenderer();

    private final JCheckBox autoRefreshOnFocusCheckBox = new JCheckBox();
    private AppTheme theme = AppTheme.LIGHT;

    public SettingsPanel() {
        buildPanel();
        refreshTexts();
        applyTheme(AppTheme.LIGHT);
    }

    public void setThemeChangeAction(ActionListener actionListener) {
        lightThemeButton.addActionListener(actionListener);
        darkThemeButton.addActionListener(actionListener);
    }

    public void setLanguageChangeAction(ActionListener actionListener) {
        languageCombo.addActionListener(actionListener);
    }

    public void setRepositoryAction(ActionListener actionListener) {
        repositoryButton.addActionListener(actionListener);
    }

    public void setAutoRefreshOnFocusChangeAction(ActionListener actionListener) {
        autoRefreshOnFocusCheckBox.addActionListener(actionListener);
    }

    public AppTheme getSelectedTheme() {
        return darkThemeButton.isSelected() ? AppTheme.DARK : AppTheme.LIGHT;
    }

    public void setSelectedTheme(AppTheme selectedTheme) {
        if (selectedTheme == AppTheme.DARK) {
            darkThemeButton.setSelected(true);
        } else {
            lightThemeButton.setSelected(true);
        }
        applyTheme(theme);
    }

    public Language getSelectedLanguage() {
        Object selectedItem = languageCombo.getSelectedItem();
        return selectedItem instanceof Language language ? language : Messages.getLanguage();
    }

    public void setSelectedLanguage(Language language) {
        languageCombo.setSelectedItem(language);
    }

    public boolean isAutoRefreshOnFocusSelected() {
        return autoRefreshOnFocusCheckBox.isSelected();
    }

    public void setAutoRefreshOnFocusSelected(boolean selected) {
        autoRefreshOnFocusCheckBox.setSelected(selected);
    }

    public void refreshTexts() {
        titleLabel.setText(Messages.text("settings.title"));
        subtitleLabel.setText(Messages.text("settings.subtitle"));
        appNameValue.setText(Messages.appName());
        versionBadge.setText(Messages.version());
        repositoryButton.setText(Messages.text("settings.repository.open"));
        themeLabel.setText(Messages.text("settings.theme"));
        lightThemeButton.setText(Messages.text("settings.theme.light"));
        darkThemeButton.setText(Messages.text("settings.theme.dark"));
        languageLabel.setText(Messages.text("settings.language"));
        autoRefreshOnFocusCheckBox.setText(Messages.text("settings.behavior.autoRefreshFocus"));
        languageCombo.repaint();
    }

    public void applyTheme(AppTheme theme) {
        this.theme = theme;
        setBackground(theme.background());

        titleLabel.setForeground(theme.textPrimary());
        subtitleLabel.setForeground(theme.textSecondary());

        applySectionTheme(aboutPanel, Messages.text("settings.about.title"), theme);
        applySectionTheme(appearancePanel, Messages.text("settings.appearance.title"), theme);
        applySectionTheme(behaviorPanel, Messages.text("settings.behavior.title"), theme);

        appNameValue.setForeground(theme.textPrimary());
        appNameValue.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));

        versionBadge.setOpaque(true);
        versionBadge.setBackground(theme.secondarySurface());
        versionBadge.setForeground(theme.actionBackground());
        versionBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.border(), 1),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));
        versionBadge.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));

        styleLinkButton(theme);
        styleSectionLabel(themeLabel, theme);
        styleSectionLabel(languageLabel, theme);
        styleThemeButton(lightThemeButton, theme);
        styleThemeButton(darkThemeButton, theme);
        styleLanguageCombo(theme);

        autoRefreshOnFocusCheckBox.setOpaque(true);
        autoRefreshOnFocusCheckBox.setBackground(theme.surface());
        autoRefreshOnFocusCheckBox.setForeground(theme.textPrimary());
        autoRefreshOnFocusCheckBox.setFocusPainted(false);
        autoRefreshOnFocusCheckBox.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
    }

    private void buildPanel() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(28, 28, 28, 28));

        JPanel headerPanel = new JPanel();
        headerPanel.setOpaque(false);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        subtitleLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(6));
        headerPanel.add(subtitleLabel);
        add(headerPanel, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(22, 0, 0, 0));

        buildAboutPanel();
        buildAppearancePanel();
        buildBehaviorPanel();

        content.add(aboutPanel);
        content.add(Box.createVerticalStrut(18));
        content.add(appearancePanel);
        content.add(Box.createVerticalStrut(18));
        content.add(behaviorPanel);
        content.add(Box.createVerticalGlue());

        add(content, BorderLayout.CENTER);
    }

    private void buildAboutPanel() {
        aboutPanel.setLayout(new BoxLayout(aboutPanel, BoxLayout.Y_AXIS));
        aboutPanel.setAlignmentX(LEFT_ALIGNMENT);

        JPanel brandRow = new JPanel();
        brandRow.setOpaque(false);
        brandRow.setLayout(new BoxLayout(brandRow, BoxLayout.X_AXIS));
        brandRow.add(appNameValue);
        brandRow.add(Box.createHorizontalStrut(14));
        brandRow.add(versionBadge);
        brandRow.add(Box.createHorizontalGlue());

        repositoryButton.setUI(new BasicButtonUI());
        repositoryButton.setFocusPainted(false);

        aboutPanel.add(brandRow);
        aboutPanel.add(Box.createVerticalStrut(18));
        aboutPanel.add(repositoryButton);
    }

    private void buildAppearancePanel() {
        appearancePanel.setLayout(new BoxLayout(appearancePanel, BoxLayout.Y_AXIS));
        appearancePanel.setAlignmentX(LEFT_ALIGNMENT);

        ButtonGroup themeGroup = new ButtonGroup();
        themeGroup.add(lightThemeButton);
        themeGroup.add(darkThemeButton);
        lightThemeButton.setUI(new BasicToggleButtonUI());
        darkThemeButton.setUI(new BasicToggleButtonUI());
        lightThemeButton.setFocusable(false);
        darkThemeButton.setFocusable(false);
        lightThemeButton.setSelected(true);

        JPanel themeButtonsPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        themeButtonsPanel.setOpaque(false);
        themeButtonsPanel.add(lightThemeButton);
        themeButtonsPanel.add(darkThemeButton);

        languageCombo.setRenderer(languageRenderer);
        languageCombo.setFocusable(false);
        languageCombo.setMaximumSize(new java.awt.Dimension(240, 40));

        appearancePanel.add(themeLabel);
        appearancePanel.add(Box.createVerticalStrut(10));
        appearancePanel.add(themeButtonsPanel);
        appearancePanel.add(Box.createVerticalStrut(18));
        appearancePanel.add(languageLabel);
        appearancePanel.add(Box.createVerticalStrut(10));
        appearancePanel.add(languageCombo);
    }

    private void buildBehaviorPanel() {
        behaviorPanel.setLayout(new BoxLayout(behaviorPanel, BoxLayout.Y_AXIS));
        behaviorPanel.setAlignmentX(LEFT_ALIGNMENT);
        autoRefreshOnFocusCheckBox.setSelected(true);
        behaviorPanel.add(autoRefreshOnFocusCheckBox);
    }

    private void applySectionTheme(JPanel panel, String title, AppTheme theme) {
        panel.setBackground(theme.surface());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(theme.border(), 1),
                        title,
                        TitledBorder.LEFT,
                        TitledBorder.TOP,
                        new Font(Font.SANS_SERIF, Font.BOLD, 18),
                        theme.textPrimary()),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)));
    }

    private void styleSectionLabel(JLabel label, AppTheme theme) {
        label.setForeground(theme.textSecondary());
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
    }

    private void styleLinkButton(AppTheme theme) {
        repositoryButton.setOpaque(true);
        repositoryButton.setContentAreaFilled(true);
        repositoryButton.setBorderPainted(true);
        repositoryButton.setBackground(theme.surface());
        repositoryButton.setForeground(theme.actionBackground());
        repositoryButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.border(), 1),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        repositoryButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        repositoryButton.setHorizontalAlignment(JButton.LEFT);
        repositoryButton.setMargin(new Insets(0, 0, 0, 0));
    }

    private void styleThemeButton(JToggleButton button, AppTheme theme) {
        boolean selected = button.isSelected();
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setFocusPainted(false);
        button.setBackground(selected ? theme.secondarySurface() : theme.surface());
        button.setForeground(selected ? theme.actionBackground() : theme.textPrimary());
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(selected ? theme.actionBackground() : theme.border(), 1),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
    }

    private void styleLanguageCombo(AppTheme theme) {
        languageRenderer.applyTheme(theme);
        ThemedComboBoxUI.apply(languageCombo, theme);
        languageCombo.setUI(new ThemedComboBoxUI(theme));
        languageCombo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.border(), 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        languageCombo.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
    }

    private static final class LanguageRenderer extends DefaultListCellRenderer {

        private AppTheme theme = AppTheme.LIGHT;

        public void applyTheme(AppTheme theme) {
            this.theme = theme;
        }

        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (index == -1) {
                setOpaque(true);
                setBackground(theme.secondarySurface());
                setForeground(theme.textPrimary());
            } else {
                setOpaque(true);
                setBackground(isSelected ? theme.selectionBackground() : theme.surface());
                setForeground(isSelected ? theme.selectionForeground() : theme.textPrimary());
            }
            return this;
        }
    }
}
