package com.adbmanager.logic;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.adbmanager.logic.model.ScrcpyLaunchRequest;
import com.adbmanager.logic.model.UserConfig;
import com.adbmanager.view.Messages;
import com.adbmanager.view.Messages.Language;
import com.adbmanager.view.swing.AppTheme;

public final class UserConfigService {

    private static final String THEME_KEY = "theme";
    private static final String LANGUAGE_KEY = "language";
    private static final String AUTO_REFRESH_KEY = "auto_refresh_on_focus";
    private static final String SCRCPY_TARGET_KEY = "scrcpy.launch_target";
    private static final String SCRCPY_FULLSCREEN_KEY = "scrcpy.fullscreen";
    private static final String SCRCPY_READ_ONLY_KEY = "scrcpy.read_only";
    private static final String SCRCPY_MAX_SIZE_KEY = "scrcpy.max_size";
    private static final String SCRCPY_MAX_FPS_KEY = "scrcpy.max_fps";
    private static final String SCRCPY_RECORD_ENABLED_KEY = "scrcpy.record_enabled";
    private static final String SCRCPY_RECORD_PATH_KEY = "scrcpy.record_path";
    private static final String SCRCPY_START_APP_ENABLED_KEY = "scrcpy.start_app_enabled";
    private static final String SCRCPY_START_APP_KEY = "scrcpy.start_app";
    private static final String SCRCPY_AUDIO_KEY = "scrcpy.audio_source";
    private static final String SCRCPY_VIRTUAL_WIDTH_KEY = "scrcpy.virtual_width";
    private static final String SCRCPY_VIRTUAL_HEIGHT_KEY = "scrcpy.virtual_height";
    private static final String SCRCPY_VIRTUAL_DPI_KEY = "scrcpy.virtual_dpi";
    private static final String SCRCPY_CAMERA_WIDTH_KEY = "scrcpy.camera_width";
    private static final String SCRCPY_CAMERA_HEIGHT_KEY = "scrcpy.camera_height";
    private static final String SCRCPY_CAMERA_ID_KEY = "scrcpy.camera_id";
    private static final String SCRCPY_KEYBOARD_KEY = "scrcpy.keyboard_mode";
    private static final String SCRCPY_MOUSE_KEY = "scrcpy.mouse_mode";

    public UserConfig load() throws IOException {
        UserConfig defaults = UserConfig.defaults(AppTheme.LIGHT, Messages.getLanguage());
        Path file = configFile();
        Files.createDirectories(file.getParent());

        if (!Files.isRegularFile(file)) {
            save(defaults);
            return defaults;
        }

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        Map<String, String> values = parse(lines);
        UserConfig loaded = new UserConfig(
                parseTheme(values.get(THEME_KEY), defaults.theme()),
                parseLanguage(values.get(LANGUAGE_KEY), defaults.language()),
                parseBoolean(values.get(AUTO_REFRESH_KEY), defaults.autoRefreshOnFocus()),
                new ScrcpyLaunchRequest(
                        parseEnum(values.get(SCRCPY_TARGET_KEY), ScrcpyLaunchRequest.LaunchTarget.class,
                                UserConfig.defaultScrcpyLaunchRequest().launchTarget()),
                        parseBoolean(values.get(SCRCPY_FULLSCREEN_KEY),
                                UserConfig.defaultScrcpyLaunchRequest().fullscreen()),
                        parseInteger(values.get(SCRCPY_MAX_SIZE_KEY)),
                        parseDouble(values.get(SCRCPY_MAX_FPS_KEY)),
                        parseBoolean(values.get(SCRCPY_RECORD_ENABLED_KEY),
                                UserConfig.defaultScrcpyLaunchRequest().recordEnabled()),
                        valueOrBlank(values.get(SCRCPY_RECORD_PATH_KEY)),
                        parseBoolean(values.get(SCRCPY_START_APP_ENABLED_KEY),
                                UserConfig.defaultScrcpyLaunchRequest().startAppEnabled()),
                        valueOrBlank(values.get(SCRCPY_START_APP_KEY)),
                        parseEnum(values.get(SCRCPY_AUDIO_KEY), ScrcpyLaunchRequest.AudioSource.class,
                                UserConfig.defaultScrcpyLaunchRequest().audioSource()),
                        parseInteger(values.get(SCRCPY_VIRTUAL_WIDTH_KEY)),
                        parseInteger(values.get(SCRCPY_VIRTUAL_HEIGHT_KEY)),
                        parseInteger(values.get(SCRCPY_VIRTUAL_DPI_KEY)),
                        parseInteger(values.get(SCRCPY_CAMERA_WIDTH_KEY)),
                        parseInteger(values.get(SCRCPY_CAMERA_HEIGHT_KEY)),
                        valueOrBlank(values.get(SCRCPY_CAMERA_ID_KEY)),
                        parseBoolean(values.get(SCRCPY_READ_ONLY_KEY),
                                UserConfig.defaultScrcpyLaunchRequest().readOnly()),
                        parseEnum(values.get(SCRCPY_KEYBOARD_KEY), ScrcpyLaunchRequest.InputMode.class,
                                UserConfig.defaultScrcpyLaunchRequest().keyboardMode()),
                        parseEnum(values.get(SCRCPY_MOUSE_KEY), ScrcpyLaunchRequest.InputMode.class,
                                UserConfig.defaultScrcpyLaunchRequest().mouseMode())));
        save(loaded);
        return loaded;
    }

    public void save(UserConfig config) throws IOException {
        UserConfig safeConfig = config == null ? UserConfig.defaults(AppTheme.LIGHT, Messages.getLanguage()) : config;
        Path file = configFile();
        Files.createDirectories(file.getParent());
        StringBuilder builder = new StringBuilder();
        builder.append("# ADB Manager user configuration").append(System.lineSeparator());
        builder.append("# Edita los valores con la app cerrada. Formato: clave=valor").append(System.lineSeparator());
        builder.append(System.lineSeparator());
        builder.append("# Apariencia").append(System.lineSeparator());
        builder.append(THEME_KEY).append("=").append(safeConfig.theme().name().toLowerCase(Locale.ROOT)).append(System.lineSeparator());
        builder.append(LANGUAGE_KEY).append("=").append(languageValue(safeConfig.language())).append(System.lineSeparator());
        builder.append(System.lineSeparator());
        builder.append("# Comportamiento").append(System.lineSeparator());
        builder.append(AUTO_REFRESH_KEY).append("=").append(safeConfig.autoRefreshOnFocus()).append(System.lineSeparator());
        builder.append(System.lineSeparator());
        builder.append("# scrcpy").append(System.lineSeparator());
        builder.append("# launch_target: device_display | virtual_display | camera").append(System.lineSeparator());
        builder.append(SCRCPY_TARGET_KEY).append("=").append(enumValue(safeConfig.scrcpyLaunchRequest().launchTarget())).append(System.lineSeparator());
        builder.append(SCRCPY_FULLSCREEN_KEY).append("=").append(safeConfig.scrcpyLaunchRequest().fullscreen()).append(System.lineSeparator());
        builder.append(SCRCPY_READ_ONLY_KEY).append("=").append(safeConfig.scrcpyLaunchRequest().readOnly()).append(System.lineSeparator());
        builder.append(SCRCPY_MAX_SIZE_KEY).append("=").append(integerValue(safeConfig.scrcpyLaunchRequest().maxSize())).append(System.lineSeparator());
        builder.append(SCRCPY_MAX_FPS_KEY).append("=").append(doubleValue(safeConfig.scrcpyLaunchRequest().maxFps())).append(System.lineSeparator());
        builder.append(SCRCPY_RECORD_ENABLED_KEY).append("=").append(safeConfig.scrcpyLaunchRequest().recordEnabled()).append(System.lineSeparator());
        builder.append(SCRCPY_RECORD_PATH_KEY).append("=").append(textValue(safeConfig.scrcpyLaunchRequest().recordPath())).append(System.lineSeparator());
        builder.append(SCRCPY_START_APP_ENABLED_KEY).append("=").append(safeConfig.scrcpyLaunchRequest().startAppEnabled()).append(System.lineSeparator());
        builder.append(SCRCPY_START_APP_KEY).append("=").append(textValue(safeConfig.scrcpyLaunchRequest().startApp())).append(System.lineSeparator());
        builder.append("# audio_source: default | none | output | mic").append(System.lineSeparator());
        builder.append(SCRCPY_AUDIO_KEY).append("=").append(enumValue(safeConfig.scrcpyLaunchRequest().audioSource())).append(System.lineSeparator());
        builder.append(SCRCPY_VIRTUAL_WIDTH_KEY).append("=").append(integerValue(safeConfig.scrcpyLaunchRequest().virtualDisplayWidth())).append(System.lineSeparator());
        builder.append(SCRCPY_VIRTUAL_HEIGHT_KEY).append("=").append(integerValue(safeConfig.scrcpyLaunchRequest().virtualDisplayHeight())).append(System.lineSeparator());
        builder.append(SCRCPY_VIRTUAL_DPI_KEY).append("=").append(integerValue(safeConfig.scrcpyLaunchRequest().virtualDisplayDpi())).append(System.lineSeparator());
        builder.append(SCRCPY_CAMERA_WIDTH_KEY).append("=").append(integerValue(safeConfig.scrcpyLaunchRequest().cameraWidth())).append(System.lineSeparator());
        builder.append(SCRCPY_CAMERA_HEIGHT_KEY).append("=").append(integerValue(safeConfig.scrcpyLaunchRequest().cameraHeight())).append(System.lineSeparator());
        builder.append(SCRCPY_CAMERA_ID_KEY).append("=").append(textValue(safeConfig.scrcpyLaunchRequest().cameraId())).append(System.lineSeparator());
        builder.append("# keyboard_mode / mouse_mode: default | sdk | uhid | aoa | disabled").append(System.lineSeparator());
        builder.append(SCRCPY_KEYBOARD_KEY).append("=").append(enumValue(safeConfig.scrcpyLaunchRequest().keyboardMode())).append(System.lineSeparator());
        builder.append(SCRCPY_MOUSE_KEY).append("=").append(enumValue(safeConfig.scrcpyLaunchRequest().mouseMode())).append(System.lineSeparator());
        Files.writeString(file, builder.toString(), StandardCharsets.UTF_8);
    }

    public Path configFile() {
        return Path.of(System.getProperty("user.home"), ".adbmanager", "config.txt");
    }

    private Map<String, String> parse(List<String> lines) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String rawLine : lines) {
            if (rawLine == null) {
                continue;
            }
            String line = rawLine.trim();
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            int separatorIndex = line.indexOf('=');
            if (separatorIndex <= 0) {
                continue;
            }
            String key = line.substring(0, separatorIndex).trim();
            String value = line.substring(separatorIndex + 1).trim();
            values.put(key, value);
        }
        return values;
    }

    private AppTheme parseTheme(String value, AppTheme fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        for (AppTheme theme : AppTheme.values()) {
            if (theme.name().equalsIgnoreCase(value.trim())) {
                return theme;
            }
        }
        return fallback;
    }

    private Language parseLanguage(String value, Language fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (Language language : Language.values()) {
            if (language.name().equalsIgnoreCase(normalized)
                    || language.locale().getLanguage().equalsIgnoreCase(normalized)) {
                return language;
            }
        }
        return fallback;
    }

    private boolean parseBoolean(String value, boolean fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return Boolean.parseBoolean(value.trim());
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim().replace(',', '.'));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private <T extends Enum<T>> T parseEnum(String value, Class<T> enumClass, T fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        try {
            return Enum.valueOf(enumClass, normalized);
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    private String enumValue(Enum<?> value) {
        return value == null ? "" : value.name().toLowerCase(Locale.ROOT);
    }

    private String integerValue(Integer value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String doubleValue(Double value) {
        if (value == null) {
            return "";
        }
        double rounded = Math.rint(value);
        if (Math.abs(value - rounded) < 0.0001d) {
            return String.valueOf((long) rounded);
        }
        return String.format(Locale.US, "%.2f", value);
    }

    private String textValue(String value) {
        return value == null ? "" : value.trim();
    }

    private String languageValue(Language language) {
        return language == null ? "" : language.locale().getLanguage();
    }

    private String valueOrBlank(String value) {
        return value == null ? "" : value.trim();
    }
}
