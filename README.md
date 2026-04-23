# AdbManager

AdbManager es una aplicacion Java para trabajar con dispositivos Android mediante `adb`, con una interfaz Swing ligera y una estructura MVC.

## Funcionalidades

- Selector de dispositivos con estado, serial y modelo.
- Resumen del dispositivo seleccionado con fabricante, modelo, Android, SoC y RAM.
- Barra grafica de RAM usada frente al total del dispositivo.
- Captura de pantalla directa desde `adb`.
- Guardado de la ultima captura en la ruta que elijas.
- Cambio de tema claro y oscuro.
- Pantalla de ajustes con informacion de version y enlace al repositorio.

## Requisitos

- Java 21 o superior.
- Depuracion USB habilitada en el dispositivo Android.

`adb` y `scrcpy` pueden detectarse desde el sistema cuando ya estan instalados. Si faltan, la app descarga automaticamente los paquetes oficiales compatibles con Windows, macOS y Linux durante el primer uso.

## Ejecutar

Compilar:

```bash
find src -name "*.java" -print0 | xargs -0 javac -d bin
```

En PowerShell:

```powershell
$sources = Get-ChildItem -Recurse src -Filter *.java | ForEach-Object { $_.FullName }
javac -d bin $sources
```

Lanzar interfaz grafica:

```powershell
java -cp bin com.adbmanager.App
```

## Estructura

- `src/com/adbmanager/logic`: modelo y acceso a `adb`.
- `src/com/adbmanager/view`: mensajes y recursos de la interfaz.
- `src/com/adbmanager/view/swing`: interfaz grafica Swing.
- `src/com/adbmanager/control`: controladores de la UI.
