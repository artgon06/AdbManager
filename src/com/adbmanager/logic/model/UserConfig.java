package com.adbmanager.logic.model;

import java.util.Objects;

import com.adbmanager.view.Messages.Language;
import com.adbmanager.view.swing.AppTheme;

public record UserConfig(
        AppTheme theme,
        Language language,
        boolean autoRefreshOnFocus,
        boolean useCustomAdbPath,
        String customAdbPath,
        ScrcpyLaunchRequest scrcpyLaunchRequest) {

    public UserConfig {
        theme = Objects.requireNonNullElse(theme, AppTheme.DARK);
        language = Objects.requireNonNullElse(language, Language.ENGLISH);
        customAdbPath = customAdbPath == null ? "" : customAdbPath.trim();
        scrcpyLaunchRequest = Objects.requireNonNullElse(scrcpyLaunchRequest, defaultScrcpyLaunchRequest());
    }

    public static UserConfig defaults(AppTheme theme, Language language) {
        return new UserConfig(theme, language, true, false, "", defaultScrcpyLaunchRequest());
    }

    public static ScrcpyLaunchRequest defaultScrcpyLaunchRequest() {
        return new ScrcpyLaunchRequest(
                ScrcpyLaunchRequest.LaunchTarget.DEVICE_DISPLAY,
                false,
                false,
                null,
                null,
                false,
                "",
                false,
                "",
                ScrcpyLaunchRequest.AudioSource.DEFAULT,
                null,
                null,
                null,
                null,
                null,
                "",
                false,
                ScrcpyLaunchRequest.InputMode.DEFAULT,
                ScrcpyLaunchRequest.InputMode.DEFAULT);
    }
}
