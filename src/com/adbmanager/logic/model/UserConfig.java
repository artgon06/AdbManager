package com.adbmanager.logic.model;

import java.util.Objects;

import com.adbmanager.view.Messages.Language;
import com.adbmanager.view.swing.AppTheme;

public record UserConfig(
        AppTheme theme,
        Language language,
        boolean autoRefreshOnFocus,
        ScrcpyLaunchRequest scrcpyLaunchRequest) {

    public UserConfig {
        theme = Objects.requireNonNullElse(theme, AppTheme.LIGHT);
        language = Objects.requireNonNullElse(language, Language.ENGLISH);
        scrcpyLaunchRequest = Objects.requireNonNullElse(scrcpyLaunchRequest, defaultScrcpyLaunchRequest());
    }

    public static UserConfig defaults(AppTheme theme, Language language) {
        return new UserConfig(theme, language, true, defaultScrcpyLaunchRequest());
    }

    public static ScrcpyLaunchRequest defaultScrcpyLaunchRequest() {
        return new ScrcpyLaunchRequest(
                ScrcpyLaunchRequest.LaunchTarget.DEVICE_DISPLAY,
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
