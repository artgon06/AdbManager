package com.adbmanager.view.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import com.adbmanager.view.Messages;
import com.adbmanager.view.Messages.Language;

public class SettingsPanel extends JPanel {

    private final JLabel titleLabel = new JLabel();
    private final JPanel aboutPanel = new JPanel();
    private final JPanel appearancePanel = new JPanel();
    private final JLabel appNameTitle = new JLabel();
    private final JLabel appNameValue = new JLabel();
    private final JLabel versionTitle = new JLabel();
    private final JLabel versionValue = new JLabel();
    private final JLabel repositoryTitle = new JLabel();
    private final JButton repositoryButton = new JButton();
    private final JLabel themeLabel = new JLabel();
    private final JLabel languageLabel = new JLabel();
    private final JRadioButton lightThemeRadio = new JRadioButton();
    private final JRadioButton darkThemeRadio = new JRadioButton();
    private final JComboBox<Language> languageCombo = new JComboBox<>(Language.values());
    private final LanguageRenderer languageRenderer = new LanguageRenderer();

    public SettingsPanel() {
        buildPanel();
        refreshTexts();
        applyTheme(AppTheme.LIGHT);
    }

    public void setThemeChangeAction(ActionListener actionListener) {
        lightThemeRadio.addActionListener(actionListener);
        darkThemeRadio.addActionListener(actionListener);
    }

    public void setLanguageChangeAction(ActionListener actionListener) {
        languageCombo.addActionListener(actionListener);
    }

    public void setRepositoryAction(ActionListener actionListener) {
        repositoryButton.addActionListener(actionListener);
    }

    public AppTheme getSelectedTheme() {
        return darkThemeRadio.isSelected() ? AppTheme.DARK : AppTheme.LIGHT;
    }

    public void setSelectedTheme(AppTheme theme) {
        if (theme == AppTheme.DARK) {
            darkThemeRadio.setSelected(true);
        } else {
            lightThemeRadio.setSelected(true);
        }
    }

    public Language getSelectedLanguage() {
        Object selectedItem = languageCombo.getSelectedItem();
        return selectedItem instanceof Language language ? language : Messages.getLanguage();
    }

    public void setSelectedLanguage(Language language) {
        languageCombo.setSelectedItem(language);
    }

    public void refreshTexts() {
        titleLabel.setText(Messages.text("settings.title"));
        appNameTitle.setText(Messages.text("settings.application"));
        appNameValue.setText(Messages.appName());
        versionTitle.setText(Messages.text("settings.version"));
        versionValue.setText(Messages.version());
        repositoryTitle.setText(Messages.text("settings.repository"));
        repositoryButton.setText(Messages.repositoryUrl());
        themeLabel.setText(Messages.text("settings.theme"));
        lightThemeRadio.setText(Messages.text("settings.theme.light"));
        darkThemeRadio.setText(Messages.text("settings.theme.dark"));
        languageLabel.setText(Messages.text("settings.language"));
        languageCombo.repaint();
    }

    public void applyTheme(AppTheme theme) {
        setBackground(theme.background());
        titleLabel.setForeground(theme.textPrimary());

        applySectionTheme(aboutPanel, Messages.text("settings.about.title"), theme);
        applySectionTheme(appearancePanel, Messages.text("settings.appearance.title"), theme);

        styleInfoLabel(appNameTitle, theme, true);
        styleInfoLabel(appNameValue, theme, false);
        styleInfoLabel(versionTitle, theme, true);
        styleInfoLabel(versionValue, theme, false);
        styleInfoLabel(repositoryTitle, theme, true);
        styleInfoLabel(themeLabel, theme, true);
        styleInfoLabel(languageLabel, theme, true);
        styleLinkButton(theme);
        styleRadio(lightThemeRadio, theme);
        styleRadio(darkThemeRadio, theme);
        styleLanguageCombo(theme);
    }

    private void buildPanel() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(28, 28, 28, 28));

        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        add(titleLabel, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(22, 0, 0, 0));

        buildAboutPanel();
        buildAppearancePanel();

        content.add(aboutPanel);
        content.add(Box.createVerticalStrut(18));
        content.add(appearancePanel);
        content.add(Box.createVerticalGlue());

        add(content, BorderLayout.CENTER);
    }

    private void buildAboutPanel() {
        aboutPanel.setLayout(new BoxLayout(aboutPanel, BoxLayout.Y_AXIS));
        aboutPanel.setAlignmentX(LEFT_ALIGNMENT);

        aboutPanel.add(createAboutRow(appNameTitle, appNameValue));
        aboutPanel.add(Box.createVerticalStrut(12));
        aboutPanel.add(createAboutRow(versionTitle, versionValue));
        aboutPanel.add(Box.createVerticalStrut(12));
        aboutPanel.add(createAboutRow(repositoryTitle, repositoryButton));
    }

    private void buildAppearancePanel() {
        appearancePanel.setLayout(new BoxLayout(appearancePanel, BoxLayout.Y_AXIS));
        appearancePanel.setAlignmentX(LEFT_ALIGNMENT);

        ButtonGroup themeGroup = new ButtonGroup();
        lightThemeRadio.setActionCommand(AppTheme.LIGHT.name());
        darkThemeRadio.setActionCommand(AppTheme.DARK.name());
        themeGroup.add(lightThemeRadio);
        themeGroup.add(darkThemeRadio);
        lightThemeRadio.setSelected(true);

        JPanel themeOptionsPanel = new JPanel();
        themeOptionsPanel.setOpaque(false);
        themeOptionsPanel.setLayout(new BoxLayout(themeOptionsPanel, BoxLayout.Y_AXIS));
        themeOptionsPanel.add(lightThemeRadio);
        themeOptionsPanel.add(Box.createVerticalStrut(10));
        themeOptionsPanel.add(darkThemeRadio);

        languageCombo.setRenderer(languageRenderer);
        languageCombo.setFocusable(false);
        languageCombo.setMaximumSize(new java.awt.Dimension(220, 38));

        appearancePanel.add(createAboutRow(themeLabel, themeOptionsPanel));
        appearancePanel.add(Box.createVerticalStrut(18));
        appearancePanel.add(createAboutRow(languageLabel, languageCombo));
    }

    private JPanel createAboutRow(JLabel keyLabel, Component valueComponent) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.add(keyLabel, BorderLayout.WEST);
        row.add(valueComponent, BorderLayout.CENTER);
        return row;
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
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));
    }

    private void styleInfoLabel(JLabel label, AppTheme theme, boolean muted) {
        label.setForeground(muted ? theme.textSecondary() : theme.textPrimary());
        label.setFont(new Font(Font.SANS_SERIF, muted ? Font.BOLD : Font.PLAIN, 16));
    }

    private void styleLinkButton(AppTheme theme) {
        repositoryButton.setOpaque(false);
        repositoryButton.setContentAreaFilled(false);
        repositoryButton.setBorderPainted(false);
        repositoryButton.setFocusPainted(false);
        repositoryButton.setForeground(theme.actionBackground());
        repositoryButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        repositoryButton.setHorizontalAlignment(JButton.LEFT);
        repositoryButton.setMargin(new Insets(0, 0, 0, 0));
    }

    private void styleRadio(JRadioButton radioButton, AppTheme theme) {
        radioButton.setOpaque(true);
        radioButton.setBackground(theme.surface());
        radioButton.setForeground(theme.textPrimary());
        radioButton.setFocusPainted(false);
        radioButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        radioButton.setMargin(new Insets(6, 2, 6, 2));
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
