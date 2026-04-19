package com.adbmanager.view.swing;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicToggleButtonUI;

import com.adbmanager.logic.model.AdbToolInfo;
import com.adbmanager.logic.model.WirelessPairingQrPayload;
import com.adbmanager.view.Messages;

public class WirelessConnectionDialog extends JDialog {

    private static final String ROOT_CONNECT = "connect";
    private static final String ROOT_PAIR = "pair";
    private static final String PAIR_CODE = "pair-code";
    private static final String PAIR_QR = "pair-qr";

    private final CardLayout rootLayout = new CardLayout();
    private final JPanel rootContent = new JPanel(rootLayout);
    private final CardLayout pairLayout = new CardLayout();
    private final JPanel pairContent = new JPanel(pairLayout);

    private final JLabel titleLabel = new JLabel();
    private final JLabel subtitleLabel = new JLabel();
    private final JLabel statusLabel = new JLabel();

    private final JPanel capabilityCard = new JPanel(new GridBagLayout());
    private final JLabel adbVersionTitleLabel = new JLabel();
    private final JLabel adbVersionValueLabel = new JLabel("-");
    private final JLabel adbLocationTitleLabel = new JLabel();
    private final WrappingTextArea adbLocationValueLabel = new WrappingTextArea("-");
    private final JLabel pairSupportTitleLabel = new JLabel();
    private final JLabel pairSupportValueLabel = new JLabel("-");
    private final JLabel qrSupportTitleLabel = new JLabel();
    private final JLabel qrSupportValueLabel = new JLabel("-");

    private final JPanel primaryTabsPanel = new JPanel(new GridLayout(1, 2, 10, 0));
    private final JToggleButton connectTabButton = new JToggleButton();
    private final JToggleButton pairTabButton = new JToggleButton();

    private final JPanel secondaryTabsPanel = new JPanel(new GridLayout(1, 2, 10, 0));
    private final JToggleButton pairCodeTabButton = new JToggleButton();
    private final JToggleButton pairQrTabButton = new JToggleButton();

    private final JPanel connectCard = new JPanel();
    private final JTextField connectHostField = new JTextField();
    private final JTextField connectPortField = new JTextField();
    private final WrappingTextArea connectNoteLabel = new WrappingTextArea();
    private final JButton connectButton = new JButton();

    private final JPanel pairCard = new JPanel();
    private final JPanel pairCodePanel = new JPanel();
    private final JTextField pairHostField = new JTextField();
    private final JTextField pairPortField = new JTextField();
    private final JTextField pairCodeField = new JTextField();
    private final WrappingTextArea pairCodeNoteLabel = new WrappingTextArea();
    private final JButton pairButton = new JButton();

    private final JPanel pairQrPanel = new JPanel(new BorderLayout(18, 0));
    private final WrappingTextArea qrInfoLabel = new WrappingTextArea();
    private final JTextField qrServiceField = new JTextField();
    private final JTextField qrPasswordField = new JTextField();
    private final JButton generateQrButton = new JButton();
    private final JButton pairQrButton = new JButton();
    private final JPanel qrPreviewOuterPanel = new JPanel(new BorderLayout());
    private final JLabel qrPreviewLabel = new JLabel();

    private final List<JPanel> surfacePanels = new ArrayList<>();
    private final List<JLabel> secondaryLabels = new ArrayList<>();
    private final List<JLabel> primaryLabels = new ArrayList<>();
    private final List<WrappingTextArea> wrappingTextAreas = new ArrayList<>();

    private AppTheme theme = AppTheme.LIGHT;
    private boolean pairSupported;
    private boolean qrSupported;

    public WirelessConnectionDialog(JFrame owner) {
        super(owner, false);
        buildDialog();
        refreshTexts();
        applyTheme(AppTheme.LIGHT);
    }

    public void setConnectAction(ActionListener listener) {
        connectButton.addActionListener(listener);
    }

    public void setPairCodeAction(ActionListener listener) {
        pairButton.addActionListener(listener);
    }

    public void setGenerateQrAction(ActionListener listener) {
        generateQrButton.addActionListener(listener);
    }

    public void setPairQrAction(ActionListener listener) {
        pairQrButton.addActionListener(listener);
    }

    public String getConnectHost() {
        return connectHostField.getText().trim();
    }

    public Integer getConnectPort() {
        return parseInteger(connectPortField.getText());
    }

    public String getPairHost() {
        return pairHostField.getText().trim();
    }

    public Integer getPairPort() {
        return parseInteger(pairPortField.getText());
    }

    public String getPairCode() {
        return pairCodeField.getText().trim();
    }

    public void suggestConnectEndpoint(String host, Integer port) {
        if ((connectHostField.getText() == null || connectHostField.getText().isBlank()) && host != null && !host.isBlank()) {
            connectHostField.setText(host.trim());
        }
        if ((connectPortField.getText() == null || connectPortField.getText().isBlank()) && port != null && port > 0) {
            connectPortField.setText(String.valueOf(port));
        }
    }

    public void setConnectEndpoint(String host, Integer port) {
        if (host != null && !host.isBlank()) {
            connectHostField.setText(host.trim());
        }
        if (port != null && port > 0) {
            connectPortField.setText(String.valueOf(port));
        }
    }

    public void suggestPairEndpoint(String host, Integer port) {
        if ((pairHostField.getText() == null || pairHostField.getText().isBlank()) && host != null && !host.isBlank()) {
            pairHostField.setText(host.trim());
        }
        if ((pairPortField.getText() == null || pairPortField.getText().isBlank()) && port != null && port > 0) {
            pairPortField.setText(String.valueOf(port));
        }
    }

    public void setToolInfo(AdbToolInfo toolInfo) {
        adbVersionValueLabel.setText(toolInfo == null ? "-" : toolInfo.version());
        String location = toolInfo == null ? "-" : toolInfo.installedAs();
        adbLocationValueLabel.setText(location);
        adbLocationValueLabel.setToolTipText(location);

        pairSupported = toolInfo != null && toolInfo.supportsPair();
        qrSupported = toolInfo != null && toolInfo.supportsQrPairing();

        pairSupportValueLabel.setText(pairSupported
                ? Messages.text("wireless.support.available")
                : Messages.text("wireless.support.unavailable"));
        qrSupportValueLabel.setText(qrSupported
                ? Messages.text("wireless.support.available")
                : Messages.text("wireless.support.unavailable"));

        if (!pairSupported) {
            pairCodeTabButton.setSelected(true);
            pairLayout.show(pairContent, PAIR_CODE);
        }
        updateActionAvailability(false);
        applyTheme(theme);
        if (isVisible()) {
            SwingUtilities.invokeLater(() -> {
                pack();
                setSize(new Dimension(Math.max(860, getWidth()), Math.max(680, getHeight())));
                revalidate();
                repaint();
            });
        }
    }

    public void setBusy(boolean busy) {
        connectHostField.setEnabled(!busy);
        connectPortField.setEnabled(!busy);
        pairHostField.setEnabled(!busy);
        pairPortField.setEnabled(!busy);
        pairCodeField.setEnabled(!busy);
        connectTabButton.setEnabled(!busy);
        pairTabButton.setEnabled(!busy);
        updateActionAvailability(busy);
    }

    public void showStatus(String message, boolean error) {
        String normalized = message == null ? "" : message.trim();
        statusLabel.putClientProperty("error", error);
        statusLabel.setText(normalized);
        statusLabel.setVisible(!normalized.isBlank());
        applyTheme(theme);
    }

    public void setQrPayload(WirelessPairingQrPayload payload, BufferedImage image) {
        qrServiceField.setText(payload == null ? "" : payload.serviceName());
        qrPasswordField.setText(payload == null ? "" : payload.password());
        qrPreviewLabel.setIcon(image == null ? null : new ImageIcon(image));
        qrPreviewLabel.setText(image == null ? Messages.text("wireless.qr.empty") : "");
        updateActionAvailability(false);
        applyTheme(theme);
    }

    public void clearQrPayload() {
        setQrPayload(null, null);
    }

    public void resetSessionFields() {
        connectHostField.setText("");
        connectPortField.setText("");
        pairHostField.setText("");
        pairPortField.setText("");
        pairCodeField.setText("");
    }

    public void refreshTexts() {
        setTitle(Messages.text("wireless.title"));
        titleLabel.setText(Messages.text("wireless.title"));
        subtitleLabel.setText(Messages.text("wireless.subtitle"));

        adbVersionTitleLabel.setText(Messages.text("wireless.capability.version"));
        adbLocationTitleLabel.setText(Messages.text("wireless.capability.location"));
        pairSupportTitleLabel.setText(Messages.text("wireless.capability.pair"));
        qrSupportTitleLabel.setText(Messages.text("wireless.capability.qr"));

        connectTabButton.setText(Messages.text("wireless.tab.connect"));
        pairTabButton.setText(Messages.text("wireless.tab.pair"));
        pairCodeTabButton.setText(Messages.text("wireless.mode.code"));
        pairQrTabButton.setText(Messages.text("wireless.mode.qr"));

        connectButton.setText(Messages.text("wireless.connect.action"));
        pairButton.setText(Messages.text("wireless.code.action"));
        generateQrButton.setText(Messages.text("wireless.qr.generate"));
        pairQrButton.setText(Messages.text("wireless.qr.pair"));

        connectNoteLabel.setText(Messages.text("wireless.connect.note"));
        pairCodeNoteLabel.setText(Messages.text("wireless.code.note"));
        qrInfoLabel.setText(Messages.text("wireless.qr.note"));

        if (qrPreviewLabel.getIcon() == null) {
            qrPreviewLabel.setText(Messages.text("wireless.qr.empty"));
        }
    }

    public void applyTheme(AppTheme theme) {
        this.theme = theme;
        getContentPane().setBackground(theme.background());
        rootContent.setBackground(theme.background());
        pairContent.setBackground(theme.background());
        pairCodePanel.setBackground(theme.background());
        pairQrPanel.setBackground(theme.background());
        primaryTabsPanel.setBackground(theme.background());
        secondaryTabsPanel.setBackground(theme.background());

        titleLabel.setForeground(theme.textPrimary());
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        subtitleLabel.setForeground(theme.textSecondary());
        subtitleLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));

        for (JPanel panel : surfacePanels) {
            panel.setOpaque(true);
            panel.setBackground(theme.background());
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(theme.border(), 1),
                    BorderFactory.createEmptyBorder(18, 18, 18, 18)));
        }

        capabilityCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.border(), 1),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        limitVerticalGrowth(capabilityCard);
        limitVerticalGrowth(primaryTabsPanel);
        limitVerticalGrowth(secondaryTabsPanel);

        for (JLabel label : secondaryLabels) {
            label.setForeground(theme.textSecondary());
            label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        }

        for (JLabel label : primaryLabels) {
            label.setForeground(theme.textPrimary());
            label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        }

        for (WrappingTextArea wrappingTextArea : wrappingTextAreas) {
            wrappingTextArea.applyTheme(theme, new Font(Font.SANS_SERIF, Font.PLAIN, 13), theme.textSecondary());
        }
        adbLocationValueLabel.applyTheme(theme, new Font(Font.SANS_SERIF, Font.PLAIN, 14), theme.textPrimary());

        stylePrimaryTabButton(connectTabButton);
        stylePrimaryTabButton(pairTabButton);
        styleSecondaryTabButton(pairCodeTabButton);
        styleSecondaryTabButton(pairQrTabButton);

        styleField(connectHostField, true);
        styleField(connectPortField, true);
        styleField(pairHostField, true);
        styleField(pairPortField, true);
        styleField(pairCodeField, true);
        styleField(qrServiceField, false);
        styleField(qrPasswordField, false);

        styleActionButton(connectButton, true);
        styleActionButton(pairButton, true);
        styleActionButton(generateQrButton, false);
        styleActionButton(pairQrButton, true);

        limitVerticalGrowth(connectCard);
        limitVerticalGrowth(pairCard);
        limitVerticalGrowth(pairContent);
        limitVerticalGrowth(rootContent);

        qrPreviewOuterPanel.setOpaque(true);
        qrPreviewOuterPanel.setBackground(theme.background());
        qrPreviewOuterPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.border(), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        qrPreviewLabel.setOpaque(true);
        qrPreviewLabel.setBackground(java.awt.Color.WHITE);
        qrPreviewLabel.setForeground(theme.placeholderForeground());
        qrPreviewLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        qrPreviewLabel.setHorizontalAlignment(SwingConstants.CENTER);
        qrPreviewLabel.setVerticalAlignment(SwingConstants.CENTER);
        qrPreviewLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.border(), 1),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)));

        statusLabel.setForeground(Boolean.TRUE.equals(statusLabel.getClientProperty("error"))
                ? new java.awt.Color(214, 80, 80)
                : theme.textSecondary());
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));

        revalidate();
        repaint();
    }

    public void open() {
        revalidate();
        pack();
        setSize(new Dimension(Math.max(860, getWidth()), Math.max(680, getHeight())));
        setLocationRelativeTo(getOwner());
        setVisible(true);
        SwingUtilities.invokeLater(() -> {
            revalidate();
            repaint();
        });
    }

    private void buildDialog() {
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setSize(new Dimension(860, 720));
        setMinimumSize(new Dimension(780, 680));

        getRootPane().registerKeyboardAction(
                event -> setVisible(false),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setOpaque(true);
        content.setBorder(new EmptyBorder(24, 24, 24, 24));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);

        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusLabel.setVisible(false);

        header.add(titleLabel);
        header.add(Box.createVerticalStrut(6));
        header.add(subtitleLabel);
        header.add(Box.createVerticalStrut(18));
        header.add(buildCapabilityCard());
        header.add(Box.createVerticalStrut(18));
        header.add(buildPrimaryTabs());

        body.add(buildRootContent(), BorderLayout.NORTH);

        content.add(header, BorderLayout.NORTH);
        content.add(body, BorderLayout.CENTER);
        content.add(statusLabel, BorderLayout.SOUTH);

        setContentPane(content);
    }

    private JPanel buildCapabilityCard() {
        surfacePanels.add(capabilityCard);
        capabilityCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        addCapabilityRow(0, adbVersionTitleLabel, adbVersionValueLabel);
        addCapabilityRow(1, adbLocationTitleLabel, adbLocationValueLabel);
        addCapabilityRow(2, pairSupportTitleLabel, pairSupportValueLabel);
        addCapabilityRow(3, qrSupportTitleLabel, qrSupportValueLabel);
        return capabilityCard;
    }

    private void addCapabilityRow(int row, JLabel title, JLabel value) {
        secondaryLabels.add(title);
        primaryLabels.add(value);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, row == 3 ? 0 : 10, 14);
        capabilityCard.add(title, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = row;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, row == 3 ? 0 : 10, 0);
        capabilityCard.add(value, constraints);
    }

    private void limitVerticalGrowth(JComponent component) {
        Dimension preferredSize = component.getPreferredSize();
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferredSize.height));
    }

    private void addCapabilityRow(int row, JLabel title, WrappingTextArea value) {
        secondaryLabels.add(title);
        wrappingTextAreas.add(value);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 10, 12);
        capabilityCard.add(title, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = row;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 10, 0);
        capabilityCard.add(value, constraints);
    }

    private JPanel buildPrimaryTabs() {
        primaryTabsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        ButtonGroup group = new ButtonGroup();
        group.add(connectTabButton);
        group.add(pairTabButton);

        configureTabButton(connectTabButton, ROOT_CONNECT, rootLayout, rootContent);
        configureTabButton(pairTabButton, ROOT_PAIR, rootLayout, rootContent);
        connectTabButton.setSelected(true);

        primaryTabsPanel.add(connectTabButton);
        primaryTabsPanel.add(pairTabButton);
        return primaryTabsPanel;
    }

    private JPanel buildRootContent() {
        rootContent.setAlignmentX(Component.LEFT_ALIGNMENT);
        rootContent.add(buildConnectCard(), ROOT_CONNECT);
        rootContent.add(buildPairCard(), ROOT_PAIR);
        return rootContent;
    }

    private JPanel buildConnectCard() {
        surfacePanels.add(connectCard);
        connectCard.setLayout(new BoxLayout(connectCard, BoxLayout.Y_AXIS));
        connectCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrappingTextAreas.add(connectNoteLabel);

        connectCard.add(buildFieldRow(Messages.text("wireless.connect.host"), connectHostField));
        connectCard.add(Box.createVerticalStrut(12));
        connectCard.add(buildFieldRow(Messages.text("wireless.connect.port"), connectPortField));
        connectCard.add(Box.createVerticalStrut(16));

        JPanel actionsPanel = new JPanel();
        actionsPanel.setOpaque(false);
        actionsPanel.setLayout(new BoxLayout(actionsPanel, BoxLayout.X_AXIS));
        actionsPanel.add(connectButton);
        actionsPanel.add(Box.createHorizontalGlue());

        connectCard.add(actionsPanel);
        connectCard.add(Box.createVerticalStrut(14));
        connectCard.add(connectNoteLabel);
        return connectCard;
    }

    private JPanel buildPairCard() {
        surfacePanels.add(pairCard);
        pairCard.setLayout(new BoxLayout(pairCard, BoxLayout.Y_AXIS));
        pairCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        pairCard.add(buildSecondaryTabs());
        pairCard.add(Box.createVerticalStrut(14));
        pairCard.add(buildPairContent());
        return pairCard;
    }

    private JPanel buildSecondaryTabs() {
        ButtonGroup group = new ButtonGroup();
        group.add(pairCodeTabButton);
        group.add(pairQrTabButton);

        configureTabButton(pairCodeTabButton, PAIR_CODE, pairLayout, pairContent);
        configureTabButton(pairQrTabButton, PAIR_QR, pairLayout, pairContent);
        pairCodeTabButton.setSelected(true);

        secondaryTabsPanel.add(pairCodeTabButton);
        secondaryTabsPanel.add(pairQrTabButton);
        return secondaryTabsPanel;
    }

    private JPanel buildPairContent() {
        pairContent.setOpaque(true);
        pairContent.add(buildPairCodePanel(), PAIR_CODE);
        pairContent.add(buildPairQrPanel(), PAIR_QR);
        return pairContent;
    }

    private JPanel buildPairCodePanel() {
        pairCodePanel.setOpaque(true);
        pairCodePanel.setLayout(new BoxLayout(pairCodePanel, BoxLayout.Y_AXIS));
        wrappingTextAreas.add(pairCodeNoteLabel);
        pairCodePanel.add(buildFieldRow(Messages.text("wireless.code.host"), pairHostField));
        pairCodePanel.add(Box.createVerticalStrut(12));
        pairCodePanel.add(buildFieldRow(Messages.text("wireless.code.pairPort"), pairPortField));
        pairCodePanel.add(Box.createVerticalStrut(12));
        pairCodePanel.add(buildFieldRow(Messages.text("wireless.code.pairCode"), pairCodeField));
        pairCodePanel.add(Box.createVerticalStrut(16));

        JPanel actionsPanel = new JPanel();
        actionsPanel.setOpaque(false);
        actionsPanel.setLayout(new BoxLayout(actionsPanel, BoxLayout.X_AXIS));
        actionsPanel.add(pairButton);
        actionsPanel.add(Box.createHorizontalGlue());

        pairCodePanel.add(actionsPanel);
        pairCodePanel.add(Box.createVerticalStrut(14));
        pairCodePanel.add(pairCodeNoteLabel);
        return pairCodePanel;
    }

    private JPanel buildPairQrPanel() {
        JPanel leftColumn = new JPanel();
        leftColumn.setOpaque(false);
        leftColumn.setLayout(new BoxLayout(leftColumn, BoxLayout.Y_AXIS));
        wrappingTextAreas.add(qrInfoLabel);
        leftColumn.add(qrInfoLabel);
        leftColumn.add(Box.createVerticalStrut(16));
        leftColumn.add(buildFieldRow(Messages.text("wireless.qr.service"), qrServiceField));
        leftColumn.add(Box.createVerticalStrut(12));
        leftColumn.add(buildFieldRow(Messages.text("wireless.qr.password"), qrPasswordField));
        leftColumn.add(Box.createVerticalStrut(16));

        JPanel actionsPanel = new JPanel();
        actionsPanel.setOpaque(false);
        actionsPanel.setLayout(new BoxLayout(actionsPanel, BoxLayout.X_AXIS));
        actionsPanel.add(generateQrButton);
        actionsPanel.add(Box.createHorizontalStrut(10));
        actionsPanel.add(pairQrButton);
        actionsPanel.add(Box.createHorizontalGlue());
        leftColumn.add(actionsPanel);
        leftColumn.add(Box.createVerticalGlue());

        qrPreviewOuterPanel.setPreferredSize(new Dimension(360, 360));
        qrPreviewLabel.setText(Messages.text("wireless.qr.empty"));
        qrPreviewOuterPanel.add(qrPreviewLabel, BorderLayout.CENTER);

        pairQrPanel.setOpaque(true);
        pairQrPanel.add(leftColumn, BorderLayout.CENTER);
        pairQrPanel.add(qrPreviewOuterPanel, BorderLayout.EAST);
        return pairQrPanel;
    }

    private JPanel buildFieldRow(String labelText, JTextField field) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);

        JLabel label = new JLabel(labelText);
        secondaryLabels.add(label);
        row.add(label, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        return row;
    }

    private void configureTabButton(
            JToggleButton button,
            String cardKey,
            CardLayout layout,
            JPanel targetPanel) {
        button.setUI(new BasicToggleButtonUI());
        button.setFocusable(false);
        button.setFocusPainted(false);
        button.setRolloverEnabled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(event -> {
            layout.show(targetPanel, cardKey);
            refreshTabStyles();
            targetPanel.revalidate();
            targetPanel.repaint();
        });
        button.getModel().addChangeListener(event -> refreshTabStyles());
    }

    private void stylePrimaryTabButton(JToggleButton button) {
        boolean selected = button.isSelected();
        boolean hovered = button.isEnabled() && button.getModel().isRollover();
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        java.awt.Color background = selected
                ? ThemeUtils.blend(theme.background(), theme.secondarySurface(), 0.92d)
                : theme.background();
        if (hovered && !selected) {
            background = ThemeUtils.blend(background, theme.selectionBackground(), 0.2d);
        }
        button.setBackground(background);
        button.setForeground(selected ? theme.actionBackground() : theme.textSecondary());
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(selected ? theme.actionBackground() : theme.border(), 1),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
    }

    private void styleSecondaryTabButton(JToggleButton button) {
        boolean selected = button.isSelected();
        boolean hovered = button.isEnabled() && button.getModel().isRollover();
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        java.awt.Color background = selected
                ? theme.selectionBackground()
                : ThemeUtils.blend(theme.background(), theme.secondarySurface(), 0.84d);
        if (hovered && !selected) {
            background = ThemeUtils.blend(background, theme.selectionBackground(), 0.18d);
        }
        button.setBackground(background);
        button.setForeground(selected ? theme.selectionForeground() : theme.textSecondary());
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(selected ? theme.actionBackground() : theme.border(), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
    }

    private void refreshTabStyles() {
        stylePrimaryTabButton(connectTabButton);
        stylePrimaryTabButton(pairTabButton);
        styleSecondaryTabButton(pairCodeTabButton);
        styleSecondaryTabButton(pairQrTabButton);
        primaryTabsPanel.repaint();
        secondaryTabsPanel.repaint();
    }

    private void styleField(JTextField field, boolean editable) {
        field.setEditable(editable);
        field.setOpaque(true);
        field.setBackground(editable ? theme.secondarySurface() : theme.surface());
        field.setForeground(theme.textPrimary());
        field.setCaretColor(theme.textPrimary());
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.border(), 1),
                BorderFactory.createEmptyBorder(9, 10, 9, 10)));
        field.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
    }

    private void styleActionButton(JButton button, boolean primary) {
        button.setUI(new BasicButtonUI());
        button.setFocusable(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setRolloverEnabled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        button.setMargin(new Insets(0, 0, 0, 0));
        if (!Boolean.TRUE.equals(button.getClientProperty("hoverListenerInstalled"))) {
            button.putClientProperty("hoverListenerInstalled", Boolean.TRUE);
            button.getModel().addChangeListener(event -> styleActionButton(button, primary));
        }

        if (button.isEnabled()) {
            boolean hovered = button.getModel().isRollover();
            java.awt.Color background = primary
                    ? theme.actionBackground()
                    : ThemeUtils.blend(theme.background(), theme.secondarySurface(), 0.84d);
            if (hovered) {
                background = ThemeUtils.blend(background, theme.selectionBackground(), primary ? 0.16d : 0.24d);
            }
            button.setBackground(background);
            button.setForeground(primary ? theme.actionForeground() : theme.textPrimary());
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(primary ? background : theme.border(), 1),
                    BorderFactory.createEmptyBorder(10, 14, 10, 14)));
            return;
        }

        button.setBackground(theme.secondarySurface());
        button.setForeground(theme.textSecondary());
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.disabledBorder(), 1),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
    }

    private void updateActionAvailability(boolean busy) {
        connectButton.setEnabled(!busy);
        pairButton.setEnabled(!busy && pairSupported);
        generateQrButton.setEnabled(!busy && qrSupported);
        pairQrButton.setEnabled(!busy
                && qrSupported
                && !qrServiceField.getText().isBlank()
                && !qrPasswordField.getText().isBlank());

        pairCodeTabButton.setEnabled(!busy && pairSupported);
        pairQrTabButton.setEnabled(!busy && qrSupported);

        if (!pairSupported) {
            pairCodeTabButton.setSelected(true);
            pairLayout.show(pairContent, PAIR_CODE);
        }
    }

    private Integer parseInteger(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

}
