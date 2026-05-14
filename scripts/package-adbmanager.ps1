param(
    [string] $ProjectDir = (Get-Location).Path,
    [string[]] $Target = @("current"),
    [string[]] $Arch = @("current"),
    [ValidateSet("installer", "image", "all")]
    [string] $Package = "installer",
    [switch] $StartMenu,
    [switch] $DesktopShortcut,
    [switch] $NoWixDownload,
    [string] $OutputDir = "",
    [string] $AppName = "ADB Manager",
    [string] $MainClass = "com.adbmanager.App",
    [string] $Vendor = "artgon06"
)

$ErrorActionPreference = "Stop"

function Resolve-ProjectDir {
    param([string] $PathValue)
    $resolved = Resolve-Path -LiteralPath $PathValue
    $root = $resolved.ProviderPath
    if (-not (Test-Path -LiteralPath (Join-Path $root "src"))) {
        throw "La carpeta indicada no contiene 'src'. Arrastra o indica la carpeta raiz del proyecto."
    }
    if (-not (Test-Path -LiteralPath (Join-Path $root "src\com\adbmanager\App.java"))) {
        throw "No se ha encontrado src\com\adbmanager\App.java. La carpeta no parece ser ADB Manager."
    }
    return $root
}

function Get-Tool {
    param([string] $Name)
    $command = Get-Command $Name -ErrorAction SilentlyContinue
    if ($null -eq $command) {
        throw "No se encontro '$Name'. Instala un JDK 17 o superior y asegurate de que esta en PATH."
    }
    return $command.Source
}

function Invoke-Step {
    param(
        [string] $FilePath,
        [string[]] $Arguments,
        [string] $WorkingDirectory
    )
    Write-Host "> $FilePath $($Arguments -join ' ')" -ForegroundColor DarkGray
    Push-Location $WorkingDirectory
    try {
        & $FilePath @Arguments
        $exitCode = $LASTEXITCODE
    } finally {
        Pop-Location
    }
    if ($exitCode -ne 0) {
        throw "El comando fallo con codigo $exitCode`: $FilePath"
    }
}

function Get-AppVersion {
    param([string] $Root)
    $messagesFile = Join-Path $Root "src\com\adbmanager\view\i18n\messages_es.properties"
    if (Test-Path -LiteralPath $messagesFile) {
        $line = Get-Content -LiteralPath $messagesFile | Where-Object { $_ -match '^app\.version=' } | Select-Object -First 1
        if ($line) {
            return ($line -replace '^app\.version=', '').Trim()
        }
    }
    return "0.0.0"
}

function Get-CurrentTarget {
    if ($IsWindows -or $env:OS -eq "Windows_NT") {
        return "windows"
    }
    if ($IsMacOS) {
        return "macos"
    }
    return "linux"
}

function Get-CurrentArch {
    $machine = [System.Runtime.InteropServices.RuntimeInformation]::OSArchitecture.ToString().ToLowerInvariant()
    if ($machine -in @("x64", "amd64")) {
        return "x64"
    }
    if ($machine -in @("arm64", "aarch64")) {
        return "arm64"
    }
    return $machine
}

function Expand-Targets {
    param([string[]] $Values)
    $normalized = @()
    foreach ($value in $Values) {
        switch ($value.ToLowerInvariant()) {
            "current" { $normalized += Get-CurrentTarget }
            "all" { $normalized += @("windows", "linux", "macos") }
            "win" { $normalized += "windows" }
            "windows" { $normalized += "windows" }
            "linux" { $normalized += "linux" }
            "mac" { $normalized += "macos" }
            "macos" { $normalized += "macos" }
            default { throw "Target no valido: $value. Usa current, windows, linux, macos o all." }
        }
    }
    return $normalized | Select-Object -Unique
}

function Expand-Architectures {
    param([string[]] $Values)
    $normalized = @()
    foreach ($value in $Values) {
        switch ($value.ToLowerInvariant()) {
            "current" { $normalized += Get-CurrentArch }
            "all" { $normalized += @("x64", "arm64") }
            "amd64" { $normalized += "x64" }
            "x86_64" { $normalized += "x64" }
            "x64" { $normalized += "x64" }
            "aarch64" { $normalized += "arm64" }
            "arm64" { $normalized += "arm64" }
            default { throw "Arquitectura no valida: $value. Usa current, x64, arm64 o all." }
        }
    }
    return $normalized | Select-Object -Unique
}

function Get-PackageTypes {
    param([string] $TargetName, [string] $Mode)
    if ($Mode -eq "image") {
        return @("app-image")
    }
    if ($Mode -eq "all") {
        switch ($TargetName) {
            "windows" { return @("app-image", "exe", "msi") }
            "linux" { return @("app-image", "deb", "rpm") }
            "macos" { return @("app-image", "dmg", "pkg") }
        }
    }
    switch ($TargetName) {
        "windows" { return @("msi") }
        "linux" { return @("deb") }
        "macos" { return @("dmg") }
    }
}

function Ensure-Wix {
    param([string] $Root, [switch] $SkipDownload)
    if (-not (Get-Command candle.exe -ErrorAction SilentlyContinue) -or -not (Get-Command light.exe -ErrorAction SilentlyContinue)) {
        if ($SkipDownload) {
            throw "WiX no esta en PATH. Instala WiX 3.x o ejecuta sin -NoWixDownload."
        }

        $wixDir = Join-Path $Root "tools\wix314"
        $candle = Join-Path $wixDir "candle.exe"
        $light = Join-Path $wixDir "light.exe"
        if (-not ((Test-Path -LiteralPath $candle) -and (Test-Path -LiteralPath $light))) {
            Write-Host "Descargando WiX 3.14 portable para generar instaladores Windows..." -ForegroundColor Cyan
            $zip = Join-Path $Root "tools\wix314-binaries.zip"
            New-Item -ItemType Directory -Force -Path (Join-Path $Root "tools") | Out-Null
            Invoke-WebRequest `
                -Uri "https://github.com/wixtoolset/wix3/releases/download/wix3141rtm/wix314-binaries.zip" `
                -OutFile $zip
            New-Item -ItemType Directory -Force -Path $wixDir | Out-Null
            Expand-Archive -LiteralPath $zip -DestinationPath $wixDir -Force
        }
        $env:PATH = "$wixDir;$env:PATH"
    }
}

function New-CleanJar {
    param(
        [string] $Root,
        [string] $BuildRoot,
        [string] $JarPath,
        [string] $MainClassName
    )

    $srcDir = Join-Path $Root "src"
    $classesDir = Join-Path $BuildRoot "classes"
    New-Item -ItemType Directory -Force -Path $classesDir | Out-Null

    $javaFiles = Get-ChildItem -LiteralPath $srcDir -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
    if (-not $javaFiles -or $javaFiles.Count -eq 0) {
        throw "No se encontraron archivos .java en src."
    }

    $argFile = Join-Path $BuildRoot "javac-files.txt"
    $argFileContent = ($javaFiles | ForEach-Object {
        '"' + ($_.Replace('\', '/')) + '"'
    }) -join [Environment]::NewLine
    [System.IO.File]::WriteAllText($argFile, $argFileContent, [System.Text.Encoding]::ASCII)

    $javac = Get-Tool "javac"
    $jar = Get-Tool "jar"
    Invoke-Step $javac @("-encoding", "UTF-8", "-g:none", "-d", $classesDir, "@$argFile") $Root

    $inputDir = Split-Path -Parent $JarPath
    New-Item -ItemType Directory -Force -Path $inputDir | Out-Null
    if (Test-Path -LiteralPath $JarPath) {
        Remove-Item -LiteralPath $JarPath -Force
    }

    Invoke-Step $jar @(
        "--create",
        "--file", $JarPath,
        "--main-class", $MainClassName,
        "-C", $classesDir, ".",
        "-C", $srcDir, "com/adbmanager/view/i18n",
        "-C", $srcDir, "com/adbmanager/view/assets"
    ) $Root
}

function Resolve-PackageIcon {
    param(
        [string] $Root,
        [string] $TargetName
    )

    $assetsDir = Join-Path $Root "src\com\adbmanager\view\assets"
    switch ($TargetName) {
        "windows" {
            $icon = Join-Path $assetsDir "app-icon.ico"
            if (Test-Path -LiteralPath $icon) {
                return $icon
            }
        }
        "linux" {
            $icon = Join-Path $assetsDir "app-icon.png"
            if (Test-Path -LiteralPath $icon) {
                return $icon
            }
        }
        "macos" {
            $icon = Join-Path $assetsDir "app-icon.icns"
            if (Test-Path -LiteralPath $icon) {
                return $icon
            }
        }
    }
    return ""
}

function New-Package {
    param(
        [string] $Root,
        [string] $InputDir,
        [string] $Destination,
        [string] $PackageType,
        [string] $Name,
        [string] $Version,
        [string] $MainJar,
        [string] $MainClassName,
        [string] $VendorName,
        [string] $IconPath,
        [switch] $AddStartMenu,
        [switch] $AddDesktopShortcut
    )

    $jpackage = Get-Tool "jpackage"
    $tempDir = $null
    $arguments = @(
        "--type", $PackageType,
        "--name", $Name,
        "--input", $InputDir,
        "--main-jar", $MainJar,
        "--main-class", $MainClassName,
        "--dest", $Destination,
        "--app-version", $Version,
        "--vendor", $VendorName
    )

    if (-not [string]::IsNullOrWhiteSpace($IconPath) -and (Test-Path -LiteralPath $IconPath)) {
        $arguments += @("--icon", $IconPath)
    }

    if ((Get-CurrentTarget) -eq "windows" -and $PackageType -in @("msi", "exe")) {
        $tempDir = Join-Path $Destination "jpackage-temp"
        if (Test-Path -LiteralPath $tempDir) {
            Remove-Item -LiteralPath $tempDir -Recurse -Force
        }
        New-Item -ItemType Directory -Force -Path $tempDir | Out-Null
        $arguments += @("--temp", $tempDir)
    }

    if ((Get-CurrentTarget) -eq "windows") {
        if ($AddStartMenu) {
            $arguments += "--win-menu"
        }
        if ($AddDesktopShortcut) {
            $arguments += "--win-shortcut"
        }
    }

    try {
        Invoke-Step $jpackage $arguments $Root
    } catch {
        if ((Get-CurrentTarget) -eq "windows" -and $PackageType -eq "msi" -and $tempDir) {
            Write-Warning "jpackage fallo al validar el MSI con WiX. Intentando fallback con light.exe -sval..."
            Invoke-WixMsiFallback $tempDir $Destination $Name $Version
            if (Test-Path -LiteralPath $tempDir) {
                Remove-Item -LiteralPath $tempDir -Recurse -Force
            }
            return
        }
        throw
    }

    if ($tempDir -and (Test-Path -LiteralPath $tempDir)) {
        Remove-Item -LiteralPath $tempDir -Recurse -Force
    }
}

function Invoke-WixMsiFallback {
    param(
        [string] $TempDir,
        [string] $Destination,
        [string] $Name,
        [string] $Version
    )

    $light = Get-Command "light.exe" -ErrorAction SilentlyContinue
    if ($null -eq $light) {
        throw "No se encontro light.exe para aplicar el fallback de WiX."
    }

    $configDir = Join-Path $TempDir "config"
    $wixObjDir = Join-Path $TempDir "wixobj"
    $imageDir = Join-Path $TempDir "images\win-msi.image\$Name"
    $outputFile = Join-Path $Destination "$Name-$Version.msi"

    foreach ($required in @($configDir, $wixObjDir, $imageDir)) {
        if (-not (Test-Path -LiteralPath $required)) {
            throw "No se pudo aplicar el fallback de WiX porque falta: $required"
        }
    }

    $locFiles = Get-ChildItem -LiteralPath $configDir -Filter "MsiInstallerStrings_*.wxl" | Sort-Object Name
    $arguments = @(
        "-nologo",
        "-spdb",
        "-ext", "WixUtilExtension",
        "-out", $outputFile,
        "-b", $configDir,
        "-sval",
        "-sice:ICE27"
    )
    foreach ($locFile in $locFiles) {
        $arguments += @("-loc", $locFile.FullName)
    }
    $arguments += "-cultures:en-us"
    $arguments += @(
        (Join-Path $wixObjDir "main.wixobj"),
        (Join-Path $wixObjDir "bundle.wixobj"),
        (Join-Path $wixObjDir "ui.wixobj")
    )

    Invoke-Step $light.Source $arguments $imageDir
}

$projectRoot = Resolve-ProjectDir $ProjectDir
$currentTarget = Get-CurrentTarget
$currentArch = Get-CurrentArch
$targets = Expand-Targets $Target
$architectures = Expand-Architectures $Arch
$version = Get-AppVersion $projectRoot

if ([string]::IsNullOrWhiteSpace($OutputDir)) {
    $OutputDir = Join-Path $projectRoot "release"
}
$OutputDir = [System.IO.Path]::GetFullPath($OutputDir)

Write-Host "Proyecto: $projectRoot" -ForegroundColor Cyan
Write-Host "Host: $currentTarget/$currentArch" -ForegroundColor Cyan
Write-Host "Version: $version" -ForegroundColor Cyan

$unsupported = @()
foreach ($targetName in $targets) {
    foreach ($archName in $architectures) {
        if ($targetName -ne $currentTarget -or $archName -ne $currentArch) {
            $unsupported += "$targetName/$archName"
        }
    }
}
if ($unsupported.Count -gt 0) {
    Write-Warning "jpackage no hace cross-compilation. Se omitiran: $($unsupported -join ', '). Ejecuta este script en cada SO/arquitectura para generar esos binarios."
}

$buildRoot = Join-Path $projectRoot "package-build"
$inputDir = Join-Path $buildRoot "input"
$jarName = "AdbManager.jar"
$jarPath = Join-Path $inputDir $jarName

if (Test-Path -LiteralPath $buildRoot) {
    Remove-Item -LiteralPath $buildRoot -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $inputDir | Out-Null
New-CleanJar $projectRoot $buildRoot $jarPath $MainClass

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

$created = @()
foreach ($targetName in $targets) {
    foreach ($archName in $architectures) {
        if ($targetName -ne $currentTarget -or $archName -ne $currentArch) {
            continue
        }

        foreach ($packageType in (Get-PackageTypes $targetName $Package)) {
            if ($targetName -eq "windows" -and $packageType -in @("exe", "msi")) {
                Ensure-Wix $projectRoot -SkipDownload:$NoWixDownload
            }

            $dest = Join-Path $OutputDir "$targetName-$archName-$packageType"
            if (Test-Path -LiteralPath $dest) {
                Remove-Item -LiteralPath $dest -Recurse -Force
            }
            New-Item -ItemType Directory -Force -Path $dest | Out-Null

            Write-Host "Generando $packageType para $targetName/$archName..." -ForegroundColor Cyan
            $packageIcon = Resolve-PackageIcon $projectRoot $targetName
            New-Package `
                -Root $projectRoot `
                -InputDir $inputDir `
                -Destination $dest `
                -PackageType $packageType `
                -Name $AppName `
                -Version $version `
                -MainJar $jarName `
                -MainClassName $MainClass `
                -VendorName $Vendor `
                -IconPath $packageIcon `
                -AddStartMenu:$StartMenu `
                -AddDesktopShortcut:$DesktopShortcut

            $created += Get-ChildItem -LiteralPath $dest -Recurse | Where-Object {
                ((-not $_.PSIsContainer) `
                    -and ($_.FullName -notlike "*jpackage-temp*") `
                    -and ($_.Extension -in @(".exe", ".msi", ".dmg", ".pkg", ".deb", ".rpm")))
            }
            if ($packageType -eq "app-image") {
                $created += Get-ChildItem -LiteralPath $dest -Directory
            }
        }
    }
}

Write-Host ""
Write-Host "Build terminado." -ForegroundColor Green
if ($created.Count -gt 0) {
    Write-Host "Artefactos:" -ForegroundColor Green
    $created | ForEach-Object { Write-Host " - $($_.FullName)" }
} else {
    Write-Warning "No se genero ningun artefacto para este host. Revisa Target/Arch."
}
