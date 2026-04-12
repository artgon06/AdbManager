package com.adbmanager.logic.client;

@FunctionalInterface
public interface AdbExecutableResolver {
    String resolveExecutable() throws Exception;
}
