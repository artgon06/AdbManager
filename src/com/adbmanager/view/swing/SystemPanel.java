package com.adbmanager.view.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.basic.BasicButtonUI;

import com.adbmanager.logic.model.AndroidUser;
import com.adbmanager.logic.model.KeyboardInputMethod;
import com.adbmanager.logic.model.SystemState;
import com.adbmanager.view.Messages;

public class SystemPanel extends JPanel {

    private final JLabel titleLabel = new JLabel();
    private final JLabel subtitleLabel = new JLabel();
    private final ScrollableContentPanel content = new ScrollableContentPanel();
    private final JScrollPane scrollPane = new JScrollPane(content);

    private final JPanel usersPanel = new JPanel();
    private final JLabel currentUserLabel = new JLabel();
    private final JLabel currentUserValueLabel = new JLabel("-");
    private final JLabel usersListLabel = new JLabel();
    private final JTextArea usersArea = new JTextArea();
    private final JScrollPane usersScrollPane = new JScrollPane(usersArea);
    private final JLabel userSelectionLabel = new JLabel();
    private final JComboBox<AndroidUser> usersCombo = new JComboBox<>();
    private final JLabel newUserLabel = new JLabel();
    private final JTextField newUserField = new JTextField();
    private final JButton createUserButton = new JButton();
    private final JButton switchUserButton = new JButton();
    private final JButton deleteUserButton = new JButton();
    private final JButton refreshUsersButton = new JButton();

    private final JPanel localesPanel = new JPanel();
    private final JCheckBox showAllAppLanguagesCheck = new JCheckBox();
    private final WrappingTextArea appLanguagesHintLabel = new WrappingTextArea();
    private final JButton applyAppLanguagesButton = new JButton();

    private final JPanel gesturesPanel = new JPanel();
    private final JCheckBox gesturesCheck = new JCheckBox();
    private final WrappingTextArea gesturesHintLabel = new WrappingTextArea();
    private final JButton applyGesturesButton = new JButton();

    private final JPanel keyboardsPanel = new JPanel();
    private final JLabel currentKeyboardLabel = new JLabel();
    private final JLabel currentKeyboardValueLabel = new JLabel("-");
    private final JLabel keyboardSelectionLabel = new JLabel();
    private final JComboBox<KeyboardInputMethod> keyboardCombo = new JComboBox<>();
    private final WrappingTextArea keyboardHintLabel = new WrappingTextArea();
    private final JButton enableKeyboardButton = new JButton();
    private final JButton setKeyboardButton = new JButton();
    private final JButton refreshKeyboardsButton = new JButton();

    private final JLabel statusLabel = new JLabel(" ");
    private final UserRenderer userRenderer = new UserRenderer();
    private final KeyboardRenderer keyboardRenderer = new KeyboardRenderer();

    private AppTheme theme = AppTheme.LIGHT;
    private boolean deviceAvailable;
    private boolean busy;
    private SystemState currentState = SystemState.empty();

    public SystemPanel() {
        buildPanel();
        refreshTexts();
        applyTheme(AppTheme.LIGHT);
        clearState();
    }

    public void setRefreshUsersAction(ActionListener actionListener) {
        refreshUsersButton.addActionListener(actionListener);
    }

    public void setCreateUserAction(ActionListener actionListener) {
        createUserButton.addActionListener(actionListener);
    }

    public void setSwitchUserAction(ActionListener actionListener) {
        switchUserButton.addActionListener(actionListener);
    }

    public void setDeleteUserAction(ActionListener actionListener) {
        deleteUserButton.addActionListener(actionListener);
    }

    public void setApplyAppLanguagesAction(ActionListener actionListener) {
        applyAppLanguagesButton.addActionListener(actionListener);
    }

    public void setApplyGesturesAction(ActionListener actionListener) {
        applyGesturesButton.addActionListener(actionListener);
    }

    public void setRefreshKeyboardsAction(ActionListener actionListener) {
        refreshKeyboardsButton.addActionListener(actionListener);
    }

    public void setEnableKeyboardAction(ActionListener actionListener) {
        enableKeyboardButton.addActionListener(actionListener);
    }

    public void setSetKeyboardAction(ActionListener actionListener) {
        setKeyboardButton.addActionListener(actionListener);
    }

    public void setDeviceAvailable(boolean deviceAvailable) {
        this.deviceAvailable = deviceAvailable;
        updateControlStates();
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
        updateControlStates();
    }

    public void setStatus(String message, boolean error) {
        String normalized = message == null ? "" : message.trim();
        statusLabel.putClientProperty("error", error);
        statusLabel.setText(normalized.isBlank() ? " " : normalized);
        styleStatusLabel();
    }

    public String getNewUserName() {
        return newUserField.getText().trim();
    }

    public Integer getSelectedUserId() {
        Object selectedItem = usersCombo.getSelectedItem();
        return selectedItem instanceof AndroidUser user ? user.id() : null;
    }

    public boolean isShowAllAppLanguagesSelected() {
        return showAllAppLanguagesCheck.isSelected();
    }

    public boolean isGesturesSelected() {
        return gesturesCheck.isSelected();
    }

    public boolean isGesturalNavigationSelected() {
        return isGesturesSelected();
    }

    public String getSelectedKeyboardId() {
        Object selectedItem = keyboardCombo.getSelectedItem();
        return selectedItem instanceof KeyboardInputMethod keyboard ? keyboard.id() : "";
    }

    public boolean isSelectedKeyboardEnabled() {
        Object selectedItem = keyboardCombo.getSelectedItem();
        return selectedItem instanceof KeyboardInputMethod keyboard && keyboard.enabled();
    }

    public void setSystemState(SystemState state) {
        currentState = state == null ? SystemState.empty() : state;
        applySystemState();
    }

    public void clearState() {
        currentState = SystemState.empty();
        applySystemState();
    }

    public void clearSystemState() {
        clearState();
    }

    public void refreshTexts() {
        titleLabel.setText(Messages.text("system.title"));
        subtitleLabel.setText(Messages.text("system.subtitle"));
        currentUserLabel.setText(Messages.text("system.users.current"));
        usersListLabel.setText(Messages.text("system.users.list"));
        userSelectionLabel.setText(Messages.text("system.users.selection"));
        newUserLabel.setText(Messages.text("system.users.newName"));
        createUserButton.setText(Messages.text("system.users.create"));
        switchUserButton.setText(Messages.text("system.users.switch"));
        deleteUserButton.setText(Messages.text("system.users.delete"));
        refreshUsersButton.setText(Messages.text("system.users.refresh"));

        showAllAppLanguagesCheck.setText(Messages.text("system.locales.toggle"));
        appLanguagesHintLabel.setText(Messages.text("system.locales.hint"));
        applyAppLanguagesButton.setText(Messages.text("system.locales.apply"));

        gesturesCheck.setText(Messages.text("system.gestures.toggle"));
        gesturesHintLabel.setText(Messages.text("system.gestures.hint"));
        applyGesturesButton.setText(Messages.text("system.gestures.apply"));

        currentKeyboardLabel.setText(Messages.text("system.keyboards.current"));
        keyboardSelectionLabel.setText(Messages.text("system.keyboards.selection"));
        keyboardHintLabel.setText(Messages.text("system.keyboards.hint"));
        updateKeyboardToggleButtonText();
        setKeyboardButton.setText(Messages.text("system.keyboards.setDefault"));
        refreshKeyboardsButton.setText(Messages.text("system.keyboards.refresh"));

        usersPanel.setBorder(createSectionBorder(Messages.text("system.users.title")));
        localesPanel.setBorder(createSectionBorder(Messages.text("system.locales.title")));
        gesturesPanel.setBorder(createSectionBorder(Messages.text("system.gestures.title")));
        keyboardsPanel.setBorder(createSectionBorder(Messages.text("system.keyboards.title")));
        usersCombo.repaint();
        keyboardCombo.repaint();
        applySystemState();
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
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        subtitleLabel.setForeground(theme.textSecondary());
        subtitleLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));

        styleSection(usersPanel, Messages.text("system.users.title"));
        styleSection(localesPanel, Messages.text("system.locales.title"));
        styleSection(gesturesPanel, Messages.text("system.gestures.title"));
        styleSection(keyboardsPanel, Messages.text("system.keyboards.title"));

        for (JLabel label : List.of(
                currentUserLabel,
                usersListLabel,
                userSelectionLabel,
                newUserLabel,
                currentKeyboardLabel,
                keyboardSelectionLabel)) {
            label.setForeground(theme.textSecondary());
            label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        }

        for (JLabel valueLabel : List.of(currentUserValueLabel, currentKeyboardValueLabel)) {
            valueLabel.setForeground(theme.textPrimary());
            valueLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        }

        appLanguagesHintLabel.applyTheme(theme, new Font(Font.SANS_SERIF, Font.PLAIN, 13), theme.textSecondary());
        gesturesHintLabel.applyTheme(theme, new Font(Font.SANS_SERIF, Font.PLAIN, 13), theme.textSecondary());
        keyboardHintLabel.applyTheme(theme, new Font(Font.SANS_SERIF, Font.PLAIN, 13), theme.textSecondary());

        styleTextArea(usersArea);
        styleScrollPane(usersScrollPane);
        styleTextField(newUserField);
        styleComboBox(usersCombo, userRenderer);
        styleComboBox(keyboardCombo, keyboardRenderer);
        styleCheckBox(showAllAppLanguagesCheck);
        styleCheckBox(gesturesCheck);
        styleButton(createUserButton, false);
        styleButton(switchUserButton, false);
        styleButton(deleteUserButton, false);
        styleButton(refreshUsersButton, false);
        styleButton(applyAppLanguagesButton, true);
        styleButton(applyGesturesButton, true);
        styleButton(enableKeyboardButton, false);
        styleButton(setKeyboardButton, true);
        styleButton(refreshKeyboardsButton, false);
        styleStatusLabel();
        repaint();
    }

    private void buildPanel() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(18, 18, 18, 18));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);
        subtitleLabel.setAlignmentX(LEFT_ALIGNMENT);
        header.add(titleLabel);
        header.add(Box.createVerticalStrut(6));
        header.add(subtitleLabel);
        add(header, BorderLayout.NORTH);

        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(16, 0, 0, 0));

        buildUsersPanel();
        buildLocalesPanel();
        buildGesturesPanel();
        buildKeyboardsPanel();

        content.add(usersPanel);
        content.add(Box.createVerticalStrut(12));
        content.add(localesPanel);
        content.add(Box.createVerticalStrut(12));
        content.add(gesturesPanel);
        content.add(Box.createVerticalStrut(12));
        content.add(keyboardsPanel);
        content.add(Box.createVerticalGlue());

        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);

        statusLabel.setBorder(new EmptyBorder(12, 0, 0, 0));
        add(statusLabel, BorderLayout.SOUTH);
    }

    private void buildUsersPanel() {
        usersPanel.setLayout(new BoxLayout(usersPanel, BoxLayout.Y_AXIS));
        usersPanel.setAlignmentX(LEFT_ALIGNMENT);
        usersPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JPanel currentUserRow = createStackedFieldPanel(currentUserLabel, currentUserValueLabel);

        usersArea.setEditable(false);
        usersArea.setFocusable(false);
        usersArea.setLineWrap(true);
        usersArea.setWrapStyleWord(true);
        usersScrollPane.setPreferredSize(new Dimension(0, 110));
        usersScrollPane.setAlignmentX(LEFT_ALIGNMENT);

        usersCombo.setRenderer(userRenderer);
        usersCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        usersCombo.setFocusable(false);

        JPanel selectionPanel = new JPanel();
        selectionPanel.setOpaque(false);
        selectionPanel.setLayout(new BoxLayout(selectionPanel, BoxLayout.Y_AXIS));
        selectionPanel.setAlignmentX(LEFT_ALIGNMENT);
        selectionPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        selectionPanel.add(createStackedFieldPanel(userSelectionLabel, usersCombo));
        selectionPanel.add(Box.createVerticalStrut(10));
        selectionPanel.add(createStackedFieldPanel(newUserLabel, newUserField));

        JPanel actionsPanel = new JPanel(new GridLayout(1, 4, 10, 0));
        actionsPanel.setOpaque(false);
        actionsPanel.setAlignmentX(LEFT_ALIGNMENT);
        actionsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        configureButton(createUserButton);
        configureButton(switchUserButton);
        configureButton(deleteUserButton);
        configureButton(refreshUsersButton);
        actionsPanel.add(createUserButton);
        actionsPanel.add(switchUserButton);
        actionsPanel.add(deleteUserButton);
        actionsPanel.add(refreshUsersButton);

        usersPanel.add(currentUserRow);
        usersPanel.add(Box.createVerticalStrut(10));
        usersPanel.add(usersListLabel);
        usersPanel.add(Box.createVerticalStrut(8));
        usersPanel.add(usersScrollPane);
        usersPanel.add(Box.createVerticalStrut(12));
        usersPanel.add(selectionPanel);
        usersPanel.add(Box.createVerticalStrut(10));
        usersPanel.add(actionsPanel);
    }

    private void buildLocalesPanel() {
        localesPanel.setLayout(new BoxLayout(localesPanel, BoxLayout.Y_AXIS));
        localesPanel.setAlignmentX(LEFT_ALIGNMENT);
        localesPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        configureButton(applyAppLanguagesButton);
        applyAppLanguagesButton.setAlignmentX(LEFT_ALIGNMENT);
        localesPanel.add(showAllAppLanguagesCheck);
        localesPanel.add(Box.createVerticalStrut(10));
        localesPanel.add(appLanguagesHintLabel);
        localesPanel.add(Box.createVerticalStrut(10));
        localesPanel.add(applyAppLanguagesButton);
    }

    private void buildGesturesPanel() {
        gesturesPanel.setLayout(new BoxLayout(gesturesPanel, BoxLayout.Y_AXIS));
        gesturesPanel.setAlignmentX(LEFT_ALIGNMENT);
        gesturesPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        configureButton(applyGesturesButton);
        applyGesturesButton.setAlignmentX(LEFT_ALIGNMENT);
        gesturesPanel.add(gesturesCheck);
        gesturesPanel.add(Box.createVerticalStrut(10));
        gesturesPanel.add(gesturesHintLabel);
        gesturesPanel.add(Box.createVerticalStrut(10));
        gesturesPanel.add(applyGesturesButton);
    }

    private void buildKeyboardsPanel() {
        keyboardsPanel.setLayout(new BoxLayout(keyboardsPanel, BoxLayout.Y_AXIS));
        keyboardsPanel.setAlignmentX(LEFT_ALIGNMENT);
        keyboardsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JPanel currentKeyboardRow = createStackedFieldPanel(currentKeyboardLabel, currentKeyboardValueLabel);

        keyboardCombo.setRenderer(keyboardRenderer);
        keyboardCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        keyboardCombo.setFocusable(false);
        keyboardCombo.addActionListener(event -> updateKeyboardToggleButtonText());

        JPanel selectionPanel = createStackedFieldPanel(keyboardSelectionLabel, keyboardCombo);
        selectionPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));

        JPanel actionsPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        actionsPanel.setOpaque(false);
        actionsPanel.setAlignmentX(LEFT_ALIGNMENT);
        actionsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        configureButton(enableKeyboardButton);
        configureButton(setKeyboardButton);
        configureButton(refreshKeyboardsButton);
        actionsPanel.add(enableKeyboardButton);
        actionsPanel.add(setKeyboardButton);
        actionsPanel.add(refreshKeyboardsButton);

        keyboardsPanel.add(currentKeyboardRow);
        keyboardsPanel.add(Box.createVerticalStrut(10));
        keyboardsPanel.add(selectionPanel);
        keyboardsPanel.add(Box.createVerticalStrut(10));
        keyboardsPanel.add(keyboardHintLabel);
        keyboardsPanel.add(Box.createVerticalStrut(10));
        keyboardsPanel.add(actionsPanel);
    }

    private void applySystemState() {
        AndroidUser currentUser = currentState.currentUser();
        currentUserValueLabel.setText(currentUser == null ? "-" : formatUser(currentUser));

        DefaultComboBoxModel<AndroidUser> usersModel = new DefaultComboBoxModel<>();
        for (AndroidUser user : currentState.users()) {
            usersModel.addElement(user);
        }
        usersCombo.setModel(usersModel);
        if (currentUser != null) {
            usersCombo.setSelectedItem(currentUser);
        }

        if (currentState.users().isEmpty()) {
            usersArea.setText(Messages.text("system.users.empty"));
        } else {
            StringBuilder builder = new StringBuilder();
            for (AndroidUser user : currentState.users()) {
                if (builder.length() > 0) {
                    builder.append(System.lineSeparator());
                }
                builder.append(formatUser(user));
            }
            usersArea.setText(builder.toString());
        }
        usersArea.setCaretPosition(0);

        showAllAppLanguagesCheck.setSelected(Boolean.TRUE.equals(currentState.showAllAppLanguages()));
        gesturesCheck.setSelected(Boolean.TRUE.equals(currentState.gesturesEnabled()));

        KeyboardInputMethod currentKeyboard = currentState.selectedKeyboard();
        currentKeyboardValueLabel.setText(currentKeyboard == null ? "-" : formatKeyboard(currentKeyboard));

        DefaultComboBoxModel<KeyboardInputMethod> keyboardModel = new DefaultComboBoxModel<>();
        for (KeyboardInputMethod keyboard : currentState.keyboards()) {
            keyboardModel.addElement(keyboard);
        }
        keyboardCombo.setModel(keyboardModel);
        if (currentKeyboard != null) {
            keyboardCombo.setSelectedItem(currentKeyboard);
        }

        updateControlStates();
    }

    private void updateControlStates() {
        boolean enabled = deviceAvailable && !busy;
        newUserField.setEnabled(enabled);
        createUserButton.setEnabled(enabled);
        refreshUsersButton.setEnabled(enabled);
        showAllAppLanguagesCheck.setEnabled(enabled);
        applyAppLanguagesButton.setEnabled(enabled);
        gesturesCheck.setEnabled(enabled);
        applyGesturesButton.setEnabled(enabled);
        refreshKeyboardsButton.setEnabled(enabled);

        boolean hasUsers = enabled && usersCombo.getItemCount() > 0;
        usersCombo.setEnabled(hasUsers);
        switchUserButton.setEnabled(hasUsers);
        deleteUserButton.setEnabled(hasUsers);

        boolean hasKeyboards = enabled && keyboardCombo.getItemCount() > 0;
        keyboardCombo.setEnabled(hasKeyboards);
        enableKeyboardButton.setEnabled(hasKeyboards);
        setKeyboardButton.setEnabled(hasKeyboards);
        updateKeyboardToggleButtonText();

        applyTheme(theme);
    }

    private JPanel createStackedFieldPanel(JLabel label, Component field) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        label.setAlignmentX(LEFT_ALIGNMENT);
        if (field instanceof JComponent component) {
            component.setAlignmentX(LEFT_ALIGNMENT);
        }
        panel.add(label);
        panel.add(Box.createVerticalStrut(8));
        panel.add(field);
        return panel;
    }

    private void updateKeyboardToggleButtonText() {
        enableKeyboardButton.setText(Messages.text(
                isSelectedKeyboardEnabled() ? "system.keyboards.disable" : "system.keyboards.enable"));
    }

    private void styleSection(JPanel panel, String title) {
        panel.setBackground(theme.background());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(theme.border(), 1),
                        title,
                        TitledBorder.LEFT,
                        TitledBorder.TOP,
                        new Font(Font.SANS_SERIF, Font.BOLD, 18),
                        theme.textPrimary()),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));
    }

    private void styleTextArea(JTextArea textArea) {
        textArea.setForeground(theme.textPrimary());
        textArea.setDisabledTextColor(theme.textSecondary());
        textArea.setBackground(theme.secondarySurface());
        textArea.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        textArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
    }

    private void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.setBorder(BorderFactory.createLineBorder(theme.border(), 1));
        scrollPane.getViewport().setBackground(theme.secondarySurface());
        if (scrollPane.getVerticalScrollBar() != null) {
            scrollPane.getVerticalScrollBar().setUI(new ThemedScrollBarUI(theme));
            scrollPane.getVerticalScrollBar().setUnitIncrement(24);
            scrollPane.getVerticalScrollBar().setBlockIncrement(96);
        }
        if (scrollPane.getHorizontalScrollBar() != null) {
            scrollPane.getHorizontalScrollBar().setUI(new ThemedScrollBarUI(theme));
            scrollPane.getHorizontalScrollBar().setUnitIncrement(24);
        }
    }

    private void styleTextField(JTextField textField) {
        textField.setForeground(theme.textPrimary());
        textField.setCaretColor(theme.textPrimary());
        textField.setBackground(textField.isEnabled() ? theme.secondarySurface() : theme.surface());
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.border(), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        textField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        textField.setPreferredSize(new Dimension(0, 42));
        textField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
    }

    private void styleComboBox(JComboBox<?> comboBox, DefaultListCellRenderer renderer) {
        ThemedComboBoxUI.apply(comboBox, theme);
        comboBox.setUI(new ThemedComboBoxUI(theme));
        comboBox.setRenderer(renderer);
        comboBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.border(), 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        comboBox.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        comboBox.setBackground(theme.secondarySurface());
        comboBox.setForeground(theme.textPrimary());
    }

    private void styleCheckBox(JCheckBox checkBox) {
        checkBox.setOpaque(true);
        checkBox.setBackground(theme.background());
        checkBox.setForeground(checkBox.isEnabled() ? theme.textPrimary() : theme.textSecondary());
        checkBox.setFocusPainted(false);
        checkBox.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
    }

    private void configureButton(JButton button) {
        button.setUI(new BasicButtonUI());
        button.setFocusPainted(false);
        button.setFocusable(false);
        button.setRolloverEnabled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.getModel().addChangeListener(event -> styleButton(button, Boolean.TRUE.equals(button.getClientProperty("primary"))));
    }

    private void styleButton(JButton button, boolean primary) {
        button.putClientProperty("primary", primary);
        boolean enabled = button.isEnabled();
        boolean hovered = enabled && button.getModel().isRollover();
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));

        if (enabled) {
            java.awt.Color background = primary
                    ? theme.actionBackground()
                    : ThemeUtils.blend(theme.background(), theme.secondarySurface(), 0.84d);
            if (hovered) {
                background = ThemeUtils.blend(background, theme.selectionBackground(), primary ? 0.18d : 0.22d);
            }
            button.setBackground(background);
            button.setForeground(primary ? theme.actionForeground() : theme.textPrimary());
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(primary ? background : theme.border(), 1),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        } else {
            button.setBackground(theme.surface());
            button.setForeground(theme.textSecondary());
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(theme.disabledBorder(), 1),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        }
    }

    private void styleStatusLabel() {
        boolean error = Boolean.TRUE.equals(statusLabel.getClientProperty("error"));
        statusLabel.setForeground(error ? new java.awt.Color(214, 80, 80) : theme.actionBackground());
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
    }

    private String formatUser(AndroidUser user) {
        StringBuilder builder = new StringBuilder();
        builder.append(user.displayLabel());
        if (user.current()) {
            builder.append(" - ").append(Messages.text("system.users.currentMarker"));
        } else if (user.running()) {
            builder.append(" - ").append(Messages.text("system.users.runningMarker"));
        }
        return builder.toString();
    }

    private String formatKeyboard(KeyboardInputMethod keyboard) {
        StringBuilder builder = new StringBuilder();
        builder.append(keyboard.displayLabel());
        if (keyboard.selected()) {
            builder.append(" - ").append(Messages.text("system.keyboards.defaultMarker"));
        } else if (keyboard.enabled()) {
            builder.append(" - ").append(Messages.text("system.keyboards.enabledMarker"));
        }
        return builder.toString();
    }

    private TitledBorder createSectionBorder(String title) {
        return BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(theme.border(), 1),
                title,
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font(Font.SANS_SERIF, Font.BOLD, 18),
                theme.textPrimary());
    }

    private final class UserRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            setOpaque(true);
            setBackground(isSelected ? theme.selectionBackground() : (index == -1 ? theme.secondarySurface() : theme.surface()));
            setForeground(isSelected ? theme.selectionForeground() : theme.textPrimary());
            if (value instanceof AndroidUser user) {
                setText(formatUser(user));
            }
            return this;
        }
    }

    private final class KeyboardRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            setOpaque(true);
            setBackground(isSelected ? theme.selectionBackground() : (index == -1 ? theme.secondarySurface() : theme.surface()));
            setForeground(isSelected ? theme.selectionForeground() : theme.textPrimary());
            if (value instanceof KeyboardInputMethod keyboard) {
                setText(formatKeyboard(keyboard));
            }
            return this;
        }
    }
}
