$ErrorActionPreference = "Stop"
$projectRoot = $PSScriptRoot
$javaCommand = Get-Command java.exe -ErrorAction SilentlyContinue
if ($null -eq $javaCommand) {
    throw "Java was not found on PATH. Install JDK 17 or newer and try again."
}

$javaHome = Split-Path (Split-Path $javaCommand.Source -Parent) -Parent
$javaw = Join-Path $javaHome "bin\javaw.exe"
$javac = Join-Path $javaHome "bin\javac.exe"
$connector = Join-Path $projectRoot "lib\mysql-connector-j-9.4.0.jar"
if (!(Test-Path $javaw) -or !(Test-Path $javac)) {
    throw "A JDK installation with javaw.exe and javac.exe is required."
}
if (!(Test-Path $connector)) {
    throw "MySQL Connector/J is missing: $connector"
}

$adminUsername = Read-Host "Choose a demo administrator username"
if ([string]::IsNullOrWhiteSpace($adminUsername)) {
    throw "The demo administrator username cannot be empty."
}

$securePassword = Read-Host "Choose a demo administrator password (at least 12 characters)" -AsSecureString
$passwordPointer = [IntPtr]::Zero
try {
    $passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    $adminPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
} finally {
    if ($passwordPointer -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
    }
    $securePassword.Dispose()
}
if ($adminPassword.Length -lt 12) {
    throw "The demo administrator password must be at least 12 characters."
}

$env:HOSTEL_DEMO_MODE = "true"
$env:HOSTEL_DEMO_ADMIN_USERNAME = $adminUsername
$env:HOSTEL_DEMO_ADMIN_PASSWORD = $adminPassword

Push-Location $projectRoot
try {
    $sourceFiles = @((Join-Path $projectRoot "src\Main.java"))
    foreach ($directory in @("model", "security", "database", "dao", "ui")) {
        $sourceFiles += Get-ChildItem (Join-Path $projectRoot "src\$directory") -Filter "*.java" |
            Select-Object -ExpandProperty FullName
    }
    & $javac -cp $connector -d (Join-Path $projectRoot "out") $sourceFiles
    if ($LASTEXITCODE -ne 0) {
        throw "Compilation failed. Review the compiler errors above."
    }
    Start-Process -FilePath $javaw -ArgumentList @(
        "-cp",
        "`"out;$connector`"",
        "Main"
    ) -WorkingDirectory $projectRoot
} finally {
    Pop-Location
}
