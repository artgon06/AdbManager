package com.adbmanager.view.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

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
import javax.swing.JScrollPane;
import javax.swing.JTextField;
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
    private final ScrollableContentPanel content = new ScrollableContentPanel();
    private final JScrollPane scrollPane = new JScrollPane(content);
    private final JPanel aboutPanel = new JPanel();
    private final JPanel appearancePanel = new JPanel();
    private final JPanel behaviorPanel = new JPanel();
    private final JPanel adbPanel = new JPanel();

    private final JLabel appNameValue = new JLabel();
    private final JLabel versionBadge = new JLabel();
    private final WrappingTextArea aboutSummaryLabel = new WrappingTextArea();
    private final JLabel creditsTitleLabel = new JLabel();
    private final WrappingTextArea scrcpyCreditLabel = new WrappingTextArea();
    private final WrappingTextArea deviceCatalogCreditLabel = new WrappingTextArea();
    private final JButton repositoryButton = new JButton();
    private final JButton scrcpyRepositoryButton = new JButton();
    private final JButton deviceCatalogButton = new JButton();

    private final JLabel themeLabel = new JLabel();
    private final JToggleButton lightThemeButton = new JToggleButton();
    private final JToggleButton darkThemeButton = new JToggleButton();
    private final JLabel languageLabel = new JLabel();
    private final JComboBox<Language> languageCombo = new JComboBox<>(Language.values());
    private final LanguageRenderer languageRenderer = new LanguageRenderer();

    private final JCheckBox autoRefreshOnFocusCheckBox = new JCheckBox();
    private final JCheckBox useCustomAdbPathCheckBox = new JCheckBox();
    private final JLabel adbPathLabel = new JLabel();
    private final JTextField adbPathField = new JTextField();
    private final JButton adbPathBrowseButton = new JButton();
    private final WrappingTextArea adbHintLabel = new WrappingTextArea();
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

    public void setScrcpyRepositoryAction(ActionListener actionListener) {
        scrcpyRepositoryButton.addActionListener(actionListener);
    }

    public void setDeviceCatalogAction(ActionListener actionListener) {
        deviceCatalogButton.addActionListener(actionListener);
    }

    public void setAutoRefreshOnFocusChangeAction(ActionListener actionListener) {
        autoRefreshOnFocusCheckBox.addActionListener(actionListener);
    }

    public void setUseCustomAdbPathChangeAction(ActionListener actionListener) {
        useCustomAdbPathCheckBox.addActionListener(actionListener);
    }

    public void setCustomAdbPathBrowseAction(ActionListener actionListener) {
        adbPathBrowseButton.addActionListener(actionListener);
    }

    public void setCustomAdbPathCommitAction(ActionListener actionListener) {
        adbPathField.addActionListener(actionListener);
        adbPathField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent event) {
                actionListener.actionPerformed(
                        new ActionEvent(adbPathField, ActionEvent.ACTION_PERFORMED, "adb-path-commit"));
            }
        });
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

    public boolean isUseCustomAdbPathSelected() {
        return useCustomAdbPathCheckBox.isSelected();
    }

    public void setUseCustomAdbPathSelected(boolean selected) {
        useCustomAdbPathCheckBox.setSelected(selected);
        updateAdbPathState();
    }

    public String getCustomAdbPath() {
        return adbPathField.getText().trim();
    }

    public void setCustomAdbPath(String path) {
        adbPathField.setText(path == null ? "" : path);
    }

    public void refreshTexts() {
        titleLabel.setText(Messages.text("settings.title"));
        subtitleLabel.setText(Messages.text("settings.subtitle"));
        appNameValue.setText(Messages.appName());
        versionBadge.setText(Messages.version());
        aboutSummaryLabel.setText(Messages.text("settings.about.summary"));
        creditsTitleLabel.setText(Messages.text("settings.about.credits"));
        scrcpyCreditLabel.setText(Messages.text("settings.about.scrcpy"));
        deviceCatalogCreditLabel.setText(Messages.text("settings.about.deviceCatalog"));
        repositoryButton.setText(Messages.text("settings.repository.open"));
        scrcpyRepositoryButton.setText(Messages.text("settings.about.scrcpy.link"));
        deviceCatalogButton.setText(Messages.text("settings.about.deviceCatalog.link"));
        themeLabel.setText(Messages.text("settings.theme"));
        lightThemeButton.setText(Messages.text("settings.theme.light"));
        darkThemeButton.setText(Messages.text("settings.theme.dark"));
        languageLabel.setText(Messages.text("settings.language"));
        autoRefreshOnFocusCheckBox.setText(Messages.text("settings.behavior.autoRefreshFocus"));
        useCustomAdbPathCheckBox.setText(Messages.text("settings.adb.useCustom"));
        adbPathLabel.setText(Messages.text("settings.adb.path"));
        adbPathBrowseButton.setText(Messages.text("settings.adb.browse"));
        adbHintLabel.setText(Messages.text("settings.adb.hint"));
        languageCombo.repaint();
    }

    public void applyTheme(AppTheme theme) {
        this.theme = theme;
        setBackground(theme.background());
        content.setBackground(theme.background());
        scrollPane.setBackground(theme.background());
        scrollPane.getViewport().setBackground(theme.background());
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        if (scrollPane.getVerticalScrollBar() != null) {
            scrollPane.getVerticalScrollBar().setUI(new ThemedScrollBarUI(theme));
            scrollPane.getVerticalScrollBar().setUnitIncrement(24);
            scrollPane.getVerticalScrollBar().setBlockIncrement(96);
        }
        if (scrollPane.getHorizontalScrollBar() != null) {
            scrollPane.getHorizontalScrollBar().setUI(new ThemedScrollBarUI(theme));
            scrollPane.getHorizontalScrollBar().setUnitIncrement(24);
        }

        titleLabel.setForeground(theme.textPrimary());
        subtitleLabel.setForeground(theme.textSecondary());

        applySectionTheme(aboutPanel, Messages.text("settings.about.title"), theme);
        applySectionTheme(appearancePanel, Messages.text("settings.appearance.title"), theme);
        applySectionTheme(behaviorPanel, Messages.text("settings.behavior.title"), theme);
        applySectionTheme(adbPanel, Messages.text("settings.adb.title"), theme);

        appNameValue.setForeground(theme.textPrimary());
        appNameValue.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        aboutSummaryLabel.applyTheme(theme, new Font(Font.SANS_SERIF, Font.PLAIN, 14), theme.textSecondary());
        creditsTitleLabel.setForeground(theme.textPrimary());
        creditsTitleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        scrcpyCreditLabel.applyTheme(theme, new Font(Font.SANS_SERIF, Font.PLAIN, 13), theme.textSecondary());
        deviceCatalogCreditLabel.applyTheme(theme, new Font(Font.SANS_SERIF, Font.PLAIN, 13), theme.textSecondary());

        versionBadge.setOpaque(true);
        versionBadge.setBackground(ThemeUtils.blend(theme.background(), theme.secondarySurface(), 0.88d));
        versionBadge.setForeground(theme.actionBackground());
        versionBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.border(), 1),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));
        versionBadge.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));

        styleLinkButton(theme);
        styleLinkButton(scrcpyRepositoryButton, theme);
        styleLinkButton(deviceCatalogButton, theme);
        styleSectionLabel(themeLabel, theme);
        styleSectionLabel(languageLabel, theme);
        styleSectionLabel(adbPathLabel, theme);
        styleThemeButton(lightThemeButton, theme);
        styleThemeButton(darkThemeButton, theme);
        styleLanguageCombo(theme);
        styleSecondaryButton(adbPathBrowseButton, theme);

        adbPathField.setBackground(theme.surface());
        adbPathField.setForeground(theme.textPrimary());
        adbPathField.setCaretColor(theme.actionBackground());
        adbPathField.setSelectionColor(theme.selectionBackground());
        adbPathField.setSelectedTextColor(theme.selectionForeground());
        adbPathField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.border(), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        adbPathField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        adbHintLabel.applyTheme(theme, new Font(Font.SANS_SERIF, Font.PLAIN, 13), theme.textSecondary());

        autoRefreshOnFocusCheckBox.setOpaque(true);
        autoRefreshOnFocusCheckBox.setBackground(theme.background());
        autoRefreshOnFocusCheckBox.setForeground(theme.textPrimary());
        autoRefreshOnFocusCheckBox.setFocusPainted(false);
        autoRefreshOnFocusCheckBox.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));

        useCustomAdbPathCheckBox.setOpaque(true);
        useCustomAdbPathCheckBox.setBackground(theme.background());
        useCustomAdbPathCheckBox.setForeground(theme.textPrimary());
        useCustomAdbPathCheckBox.setFocusPainted(false);
        useCustomAdbPathCheckBox.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
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

        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(22, 0, 0, 0));

        buildAboutPanel();
        buildAppearancePanel();
        buildBehaviorPanel();
        buildAdbPanel();

        content.add(aboutPanel);
        content.add(Box.createVerticalStrut(18));
        content.add(appearancePanel);
        content.add(Box.createVerticalStrut(18));
        content.add(behaviorPanel);
        content.add(Box.createVerticalStrut(18));
        content.add(adbPanel);
        content.add(Box.createVerticalGlue());

        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void buildAboutPanel() {
        aboutPanel.setLayout(new BoxLayout(aboutPanel, BoxLayout.Y_AXIS));
        aboutPanel.setAlignmentX(LEFT_ALIGNMENT);
        aboutPanel.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JPanel brandRow = new JPanel();
        brandRow.setOpaque(false);
        brandRow.setLayout(new BoxLayout(brandRow, BoxLayout.X_AXIS));
        brandRow.add(appNameValue);
        brandRow.add(Box.createHorizontalStrut(14));
        brandRow.add(versionBadge);
        brandRow.add(Box.createHorizontalGlue());

        repositoryButton.setUI(new BasicButtonUI());
        repositoryButton.setFocusPainted(false);
        repositoryButton.setRolloverEnabled(true);
        repositoryButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        repositoryButton.getModel().addChangeListener(event -> styleLinkButton(theme));
        scrcpyRepositoryButton.setUI(new BasicButtonUI());
        scrcpyRepositoryButton.setFocusPainted(false);
        scrcpyRepositoryButton.setRolloverEnabled(true);
        scrcpyRepositoryButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        scrcpyRepositoryButton.getModel().addChangeListener(event -> styleLinkButton(scrcpyRepositoryButton, theme));
        deviceCatalogButton.setUI(new BasicButtonUI());
        deviceCatalogButton.setFocusPainted(false);
        deviceCatalogButton.setRolloverEnabled(true);
        deviceCatalogButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        deviceCatalogButton.getModel().addChangeListener(event -> styleLinkButton(deviceCatalogButton, theme));

        JPanel linksPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        linksPanel.setOpaque(false);
        linksPanel.setAlignmentX(LEFT_ALIGNMENT);
        linksPanel.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 46));
        linksPanel.add(repositoryButton);
        linksPanel.add(scrcpyRepositoryButton);
        linksPanel.add(deviceCatalogButton);

        brandRow.setAlignmentX(LEFT_ALIGNMENT);
        brandRow.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 46));
        aboutPanel.add(brandRow);
        aboutPanel.add(Box.createVerticalStrut(12));
        aboutSummaryLabel.setAlignmentX(LEFT_ALIGNMENT);
        aboutSummaryLabel.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        scrcpyCreditLabel.setAlignmentX(LEFT_ALIGNMENT);
        scrcpyCreditLabel.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        deviceCatalogCreditLabel.setAlignmentX(LEFT_ALIGNMENT);
        deviceCatalogCreditLabel.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        creditsTitleLabel.setAlignmentX(LEFT_ALIGNMENT);

        aboutPanel.add(aboutSummaryLabel);
        aboutPanel.add(Box.createVerticalStrut(18));
        aboutPanel.add(creditsTitleLabel);
        aboutPanel.add(Box.createVerticalStrut(10));
        aboutPanel.add(scrcpyCreditLabel);
        aboutPanel.add(Box.createVerticalStrut(8));
        aboutPanel.add(deviceCatalogCreditLabel);
        aboutPanel.add(Box.createVerticalStrut(14));
        aboutPanel.add(linksPanel);
    }

    private void buildAppearancePanel() {
        appearancePanel.setLayout(new BoxLayout(appearancePanel, BoxLayout.Y_AXIS));
        appearancePanel.setAlignmentX(LEFT_ALIGNMENT);
        appearancePanel.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        ButtonGroup themeGroup = new ButtonGroup();
        themeGroup.add(lightThemeButton);
        themeGroup.add(darkThemeButton);
        lightThemeButton.setUI(new BasicToggleButtonUI());
        darkThemeButton.setUI(new BasicToggleButtonUI());
        lightThemeButton.setFocusable(false);
        darkThemeButton.setFocusable(false);
        lightThemeButton.setRolloverEnabled(true);
        darkThemeButton.setRolloverEnabled(true);
        lightThemeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        darkThemeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lightThemeButton.getModel().addChangeListener(event -> styleThemeButton(lightThemeButton, theme));
        darkThemeButton.getModel().addChangeListener(event -> styleThemeButton(darkThemeButton, theme));
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
        behaviorPanel.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        autoRefreshOnFocusCheckBox.setSelected(true);
        behaviorPanel.add(autoRefreshOnFocusCheckBox);
    }

    private void buildAdbPanel() {
        adbPanel.setLayout(new BoxLayout(adbPanel, BoxLayout.Y_AXIS));
        adbPanel.setAlignmentX(LEFT_ALIGNMENT);
        adbPanel.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        adbPathField.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 40));
        adbPathField.setAlignmentX(LEFT_ALIGNMENT);
        adbPathBrowseButton.setUI(new BasicButtonUI());
        adbPathBrowseButton.setFocusPainted(false);
        adbPathBrowseButton.setRolloverEnabled(true);
        adbPathBrowseButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        adbPathBrowseButton.getModel().addChangeListener(event -> styleSecondaryButton(adbPathBrowseButton, theme));
        useCustomAdbPathCheckBox.addActionListener(event -> updateAdbPathState());

        JPanel pathRow = new JPanel();
        pathRow.setOpaque(false);
        pathRow.setLayout(new BoxLayout(pathRow, BoxLayout.X_AXIS));
        pathRow.setAlignmentX(LEFT_ALIGNMENT);
        pathRow.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 40));
        pathRow.add(adbPathField);
        pathRow.add(Box.createHorizontalStrut(10));
        pathRow.add(adbPathBrowseButton);

        useCustomAdbPathCheckBox.setAlignmentX(LEFT_ALIGNMENT);
        adbPathLabel.setAlignmentX(LEFT_ALIGNMENT);
        adbPanel.add(useCustomAdbPathCheckBox);
        adbPanel.add(Box.createVerticalStrut(12));
        adbPanel.add(adbPathLabel);
        adbPanel.add(Box.createVerticalStrut(8));
        adbPanel.add(pathRow);
        adbPanel.add(Box.createVerticalStrut(10));
        adbHintLabel.setAlignmentX(LEFT_ALIGNMENT);
        adbHintLabel.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        adbPanel.add(adbHintLabel);
        updateAdbPathState();
    }

    private void applySectionTheme(JPanel panel, String title, AppTheme theme) {
        panel.setBackground(theme.background());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(theme.border(), 1),
                        title,
                        TitledBorder.LEFT,
                        TitledBorder.TOP,
                        new Font(Font.SANS_SERIF, Font.BOLD, 18),
                        theme.textPrimary()),
                BorderFactory.createEmptyBorder(16, 12, 16, 12)));
    }

    private void styleSectionLabel(JLabel label, AppTheme theme) {
        label.setForeground(theme.textSecondary());
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
    }

    private void styleLinkButton(AppTheme theme) {
        styleLinkButton(repositoryButton, theme);
    }

    private void styleLinkButton(JButton button, AppTheme theme) {
        boolean hovered = button.isEnabled() && button.getModel().isRollover();
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        button.setBackground(hovered
                ? ThemeUtils.blend(theme.background(), theme.selectionBackground(), 0.22d)
                : theme.background());
        button.setForeground(theme.actionBackground());
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.border(), 1),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        button.setHorizontalAlignment(JButton.LEFT);
        button.setMargin(new Insets(0, 0, 0, 0));
    }

    private void styleSecondaryButton(JButton button, AppTheme theme) {
        boolean hovered = button.isEnabled() && button.getModel().isRollover();
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        button.setBackground(hovered
                ? ThemeUtils.blend(theme.background(), theme.selectionBackground(), 0.22d)
                : theme.secondarySurface());
        button.setForeground(button.isEnabled() ? theme.textPrimary() : theme.textSecondary());
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.border(), 1),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        button.setMargin(new Insets(0, 0, 0, 0));
    }

    private void styleThemeButton(JToggleButton button, AppTheme theme) {
        boolean selected = button.isSelected();
        boolean hovered = button.isEnabled() && button.getModel().isRollover();
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setFocusPainted(false);
        java.awt.Color background = selected
                ? ThemeUtils.blend(theme.background(), theme.secondarySurface(), 0.94d)
                : theme.background();
        if (hovered && !selected) {
            background = ThemeUtils.blend(background, theme.selectionBackground(), 0.22d);
        }
        button.setBackground(background);
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

    private void updateAdbPathState() {
        boolean enabled = useCustomAdbPathCheckBox.isSelected();
        adbPathLabel.setEnabled(enabled);
        adbPathField.setEnabled(enabled);
        adbPathBrowseButton.setEnabled(enabled);
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
            ThemedComboBoxUI.applyRendererColors(this, list, theme, isSelected, index);
            return this;
        }
    }
}
