package com.adbmanager.view;

public class Messages {

    public static final String ADB_DEVICES_LIST = "List of devices attached";

    public static final String STATUS_CONNECTED = "device";
    public static final String STATUS_CONNECTING = "connecting";
    public static final String STATUS_UNAUTHORIZED = "unauthorized";
    public static final String STATUS_OFFLINE = "offline";
    public static final String STATUS_RECOVERY = "recovery";

    public static final String CONNECTED = "Conectado";
    public static final String CONNECTING = "Conectando";
    public static final String UNAUTHORIZED = "No autorizado";
    public static final String OFFLINE = "Desconectado";
    public static final String RECOVERY = "Recovery";
    public static final String UNKNOWN = "Desconocido";
    public static final String CODENAME = "Nombre en clave";
    public static final String MODEL = "Modelo";
    public static final String ERROR_NOT_CONECTED = "El dispositivo debe estar conectado para mostrar mas informacion.";

    public static final String APP_NAME = "ADB Manager";
    public static final String VERSION = "0.2.0";
    public static final String REPOSITORY_URL = "https://github.com/artgon06/AdbManager";

    private Messages() {
    }

    public static String stateLabel(String state) {
        return switch (state) {
            case STATUS_CONNECTED -> CONNECTED;
            case STATUS_CONNECTING -> CONNECTING;
            case STATUS_UNAUTHORIZED -> UNAUTHORIZED;
            case STATUS_OFFLINE -> OFFLINE;
            case STATUS_RECOVERY -> RECOVERY;
            default -> UNKNOWN;
        };
    }
}
