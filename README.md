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
- Modo CLI disponible para mantener compatibilidad con el flujo anterior.

## Requisitos

- Java 21 o superior.
- `adb` instalado y disponible en el `PATH`.
- Depuracion USB habilitada en el dispositivo Android.

## Ejecutar

Compilar:

```powershell
$sources = Get-ChildItem -Recurse src -Filter *.java | ForEach-Object { $_.FullName }
javac -d bin $sources
```

Lanzar interfaz grafica:

```powershell
java -cp bin com.adbmanager.App
```

Lanzar modo consola:

```powershell
java -cp bin com.adbmanager.App --cli
```

## Estructura

- `src/com/adbmanager/logic`: modelo y acceso a `adb`.
- `src/com/adbmanager/view`: mensajes y salida de consola.
- `src/com/adbmanager/view/swing`: interfaz grafica Swing.
- `src/com/adbmanager/control`: controladores de consola y de la UI.
