@echo off
setlocal

set "PROJECT_DIR=%~1"
if "%PROJECT_DIR%"=="" set "PROJECT_DIR=%CD%"

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\package-adbmanager.ps1" -ProjectDir "%PROJECT_DIR%" -Target current -Arch current -Package installer -StartMenu -DesktopShortcut

echo.
echo Pulsa una tecla para cerrar...
pause > nul
