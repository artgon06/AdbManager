package com.adbmanager.view;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import com.adbmanager.logic.model.DeviceType;

public final class Messages {

    public enum Language {
        ENGLISH("en", "settings.language.english"),
        SPANISH("es", "settings.language.spanish");

        private final Locale locale;
        private final String displayKey;

        Language(String languageCode, String displayKey) {
            this.locale = Locale.forLanguageTag(languageCode);
            this.displayKey = displayKey;
        }

        public Locale locale() {
            return locale;
        }

        public String displayKey() {
            return displayKey;
        }

        @Override
        public String toString() {
            return Messages.text(displayKey);
        }
    }

    private static final String BUNDLE_BASE_NAME = "com.adbmanager.view.i18n.messages";

    public static final String ADB_DEVICES_LIST = "List of devices attached";

    public static final String STATUS_CONNECTED = "device";
    public static final String STATUS_CONNECTING = "connecting";
    public static final String STATUS_UNAUTHORIZED = "unauthorized";
    public static final String STATUS_OFFLINE = "offline";
    public static final String STATUS_RECOVERY = "recovery";

    private static Language currentLanguage = defaultLanguage();
    private static ResourceBundle bundle = loadBundle(currentLanguage);

    private Messages() {
    }

    public static Language getLanguage() {
        return currentLanguage;
    }

    public static void setLanguage(Language language) {
        currentLanguage = language == null ? defaultLanguage() : language;
        bundle = loadBundle(currentLanguage);
    }

    public static Language defaultLanguage() {
        return Locale.getDefault().getLanguage().equalsIgnoreCase("es")
                ? Language.SPANISH
                : Language.ENGLISH;
    }

    public static String text(String key) {
        return bundle.containsKey(key) ? bundle.getString(key) : "!" + key + "!";
    }

    public static String format(String key, Object... args) {
        return MessageFormat.format(text(key), args);
    }

    public static String appName() {
        return text("app.name");
    }

    public static String version() {
        return text("app.version");
    }

    public static String repositoryUrl() {
        return text("app.repository.url");
    }

    public static String appTitle() {
        return format("app.title", appName(), version());
    }

    public static String stateLabel(String state) {
        return switch (state) {
            case STATUS_CONNECTED -> text("state.connected");
            case STATUS_CONNECTING -> text("state.connecting");
            case STATUS_UNAUTHORIZED -> text("state.unauthorized");
            case STATUS_OFFLINE -> text("state.offline");
            case STATUS_RECOVERY -> text("state.recovery");
            default -> text("state.unknown");
        };
    }

    public static String deviceTypeLabel(DeviceType deviceType) {
        return text(deviceType == null ? DeviceType.UNKNOWN.messageKey() : deviceType.messageKey());
    }

    private static ResourceBundle loadBundle(Language language) {
        try {
            return ResourceBundle.getBundle(BUNDLE_BASE_NAME, language.locale());
        } catch (MissingResourceException primaryError) {
            ResourceBundle fallbackBundle = loadBundleFromFilesystem(language);
            if (fallbackBundle != null) {
                return fallbackBundle;
            }
            throw primaryError;
        }
    }

    private static ResourceBundle loadBundleFromFilesystem(Language language) {
        String languageCode = language == null ? defaultLanguage().locale().getLanguage() : language.locale().getLanguage();
        String fileName = "messages_" + languageCode + ".properties";
        Path[] candidates = new Path[] {
                Path.of("src", "com", "adbmanager", "view", "i18n", fileName),
                Path.of("bin", "com", "adbmanager", "view", "i18n", fileName),
                Path.of("com", "adbmanager", "view", "i18n", fileName)
        };

        for (Path candidate : candidates) {
            if (!Files.isRegularFile(candidate)) {
                continue;
            }

            try (InputStream inputStream = Files.newInputStream(candidate)) {
                return new PropertyResourceBundle(inputStream);
            } catch (IOException ignored) {
            }
        }

        return null;
    }
}
