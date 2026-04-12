package com.adbmanager.logic.model;

import java.util.List;
import java.util.Objects;

public record SystemState(
        List<AndroidUser> users,
        Boolean showAllAppLanguages,
        Boolean gesturesEnabled,
        List<KeyboardInputMethod> keyboards) {

    public SystemState {
        users = users == null ? List.of() : List.copyOf(users);
        keyboards = keyboards == null ? List.of() : List.copyOf(keyboards);
    }

    public static SystemState empty() {
        return new SystemState(List.of(), null, null, List.of());
    }

    public AndroidUser currentUser() {
        return users.stream()
                .filter(AndroidUser::current)
                .findFirst()
                .orElse(null);
    }

    public KeyboardInputMethod selectedKeyboard() {
        return keyboards.stream()
                .filter(KeyboardInputMethod::selected)
                .findFirst()
                .orElse(null);
    }

    public boolean hasUsers() {
        return !users.isEmpty();
    }

    public boolean hasKeyboards() {
        return !keyboards.isEmpty();
    }
}
