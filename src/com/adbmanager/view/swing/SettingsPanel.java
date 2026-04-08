package com.adbmanager.view.swing;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import com.adbmanager.view.Messages;

public class SettingsPanel extends JPanel {

    private final JLabel titleLabel = new JLabel("Ajustes");
    private final JPanel aboutPanel = new JPanel();
    private final JPanel appearancePanel = new JPanel();
    private final JLabel appNameTitle = new JLabel("Aplicacion");
    private final JLabel appNameValue = new JLabel(Messages.APP_NAME);
    private final JLabel versionTitle = new JLabel("Version");
    private final JLabel versionValue = new JLabel(Messages.VERSION);
    private final JLabel repositoryTitle = new JLabel("Repositorio");
    private final JButton repositoryButton = new JButton(Messages.REPOSITORY_URL);
    private final JRadioButton lightThemeRadio = new JRadioButton("Claro");
    private final JRadioButton darkThemeRadio = new JRadioButton("Oscuro");

    public SettingsPanel() {
        buildPanel();
        applyTheme(AppTheme.LIGHT);
    }

    public void setThemeChangeAction(ActionListener actionListener) {
        lightThemeRadio.addActionListener(actionListener);
        darkThemeRadio.addActionListener(actionListener);
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

    public void applyTheme(AppTheme theme) {
        setBackground(theme.background());
        titleLabel.setForeground(theme.textPrimary());

        applySectionTheme(aboutPanel, "Acerca de", theme);
        applySectionTheme(appearancePanel, "Apariencia", theme);

        styleInfoLabel(appNameTitle, theme, true);
        styleInfoLabel(appNameValue, theme, false);
        styleInfoLabel(versionTitle, theme, true);
        styleInfoLabel(versionValue, theme, false);
        styleInfoLabel(repositoryTitle, theme, true);
        styleLinkButton(theme);
        styleRadio(lightThemeRadio, theme);
        styleRadio(darkThemeRadio, theme);
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

        appearancePanel.add(lightThemeRadio);
        appearancePanel.add(Box.createVerticalStrut(10));
        appearancePanel.add(darkThemeRadio);
    }

    private JPanel createAboutRow(JLabel keyLabel, java.awt.Component valueComponent) {
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
}
