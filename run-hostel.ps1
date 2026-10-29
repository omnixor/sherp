$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Windows.Forms
[System.Windows.Forms.Application]::EnableVisualStyles()
$projectRoot = $PSScriptRoot
$mysqlRoot = "C:\Program Files\MySQL"
$mysqlInstallation = Get-ChildItem $mysqlRoot -Directory -Filter "MySQL Server *" -ErrorAction SilentlyContinue |
    Sort-Object Name -Descending |
    Select-Object -First 1
if ($null -eq $mysqlInstallation) {
    throw "MySQL Server 8 is not installed. Install MySQL Server 8 and run this shortcut again."
}

$mysqlBin = Join-Path $mysqlInstallation.FullName "bin"
$mysqld = Join-Path $mysqlBin "mysqld.exe"
$mysql = Join-Path $mysqlBin "mysql.exe"
if (!(Test-Path $mysqld) -or !(Test-Path $mysql)) {
    throw "The MySQL server or client executable is missing from $mysqlBin."
}

$javaCommand = Get-Command javac.exe -ErrorAction SilentlyContinue
if ($null -eq $javaCommand) {
    throw "JDK 17 or newer was not found on PATH. Install a JDK and try again."
}
$javaBin = Split-Path $javaCommand.Source -Parent
$javaw = Join-Path $javaBin "javaw.exe"
$connector = Join-Path $projectRoot "lib\mysql-connector-j-9.4.0.jar"
if (!(Test-Path $javaw) -or !(Test-Path $connector)) {
    throw "Java or MySQL Connector/J is missing. Check the project prerequisites."
}

$stateRoot = Join-Path $env:LOCALAPPDATA "SmartHostel"
if ($env:HOSTEL_LAUNCHER_TEST_DIR) {
    $stateRoot = $env:HOSTEL_LAUNCHER_TEST_DIR
}
$dataDirectory = Join-Path $stateRoot "mysql-data"
$settingsPath = Join-Path $stateRoot "local-settings.json"
$logDirectory = Join-Path $stateRoot "logs"
$port = 33307
$databaseUrl = "jdbc:mysql://127.0.0.1:$port/smart_hostel"

function Protect-LocalSecret([string]$Value) {
    $secure = ConvertTo-SecureString -String $Value -AsPlainText -Force
    return ConvertFrom-SecureString -SecureString $secure
}

function Unprotect-LocalSecret([string]$Value) {
    $secure = ConvertTo-SecureString -String $Value
    $pointer = [IntPtr]::Zero
    try {
        $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    } finally {
        if ($pointer -ne [IntPtr]::Zero) {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
        }
        $secure.Dispose()
    }
}

function New-RandomHex([int]$ByteCount) {
    $bytes = New-Object byte[] $ByteCount
    $generator = [Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $generator.GetBytes($bytes)
        return [BitConverter]::ToString($bytes).Replace("-", "").ToLowerInvariant()
    } finally {
        $generator.Dispose()
    }
}

function Get-AdministratorSetup {
    $form = New-Object System.Windows.Forms.Form
    $form.Text = "Smart Hostel - First-time Setup"
    $form.StartPosition = "CenterScreen"
    $form.FormBorderStyle = "FixedDialog"
    $form.ClientSize = New-Object System.Drawing.Size(440, 220)
    $form.MaximizeBox = $false
    $form.MinimizeBox = $false
    $form.TopMost = $true

    $description = New-Object System.Windows.Forms.Label
    $description.Text = "Create the administrator account for this installation."
    $description.SetBounds(18, 15, 400, 25)

    $usernameLabel = New-Object System.Windows.Forms.Label
    $usernameLabel.Text = "Administrator username"
    $usernameLabel.SetBounds(18, 52, 170, 24)
    $username = New-Object System.Windows.Forms.TextBox
    $username.SetBounds(195, 49, 220, 26)
    $username.MaxLength = 50

    $passwordLabel = New-Object System.Windows.Forms.Label
    $passwordLabel.Text = "Password (at least 12 characters)"
    $passwordLabel.SetBounds(18, 91, 185, 24)
    $password = New-Object System.Windows.Forms.TextBox
    $password.SetBounds(195, 88, 220, 26)
    $password.UseSystemPasswordChar = $true

    $confirmLabel = New-Object System.Windows.Forms.Label
    $confirmLabel.Text = "Confirm password"
    $confirmLabel.SetBounds(18, 130, 170, 24)
    $confirm = New-Object System.Windows.Forms.TextBox
    $confirm.SetBounds(195, 127, 220, 26)
    $confirm.UseSystemPasswordChar = $true

    $ok = New-Object System.Windows.Forms.Button
    $ok.Text = "Continue"
    $ok.SetBounds(230, 173, 90, 30)
    $ok.DialogResult = [System.Windows.Forms.DialogResult]::OK
    $cancel = New-Object System.Windows.Forms.Button
    $cancel.Text = "Cancel"
    $cancel.SetBounds(325, 173, 90, 30)
    $cancel.DialogResult = [System.Windows.Forms.DialogResult]::Cancel

    $form.Controls.AddRange(@($description, $usernameLabel, $username,
        $passwordLabel, $password, $confirmLabel, $confirm, $ok, $cancel))
    $form.AcceptButton = $ok
    $form.CancelButton = $cancel
    try {
        while ($true) {
            if ($form.ShowDialog() -ne [System.Windows.Forms.DialogResult]::OK) {
                throw "First-time setup was cancelled."
            }
            if ([string]::IsNullOrWhiteSpace($username.Text)) {
                [System.Windows.Forms.MessageBox]::Show("Enter an administrator username.",
                    "Smart Hostel Setup", "OK", "Warning") | Out-Null
                continue
            }
            if ($password.Text.Length -lt 12) {
                [System.Windows.Forms.MessageBox]::Show("Use a password with at least 12 characters.",
                    "Smart Hostel Setup", "OK", "Warning") | Out-Null
                continue
            }
            if ($password.Text -cne $confirm.Text) {
                [System.Windows.Forms.MessageBox]::Show("The passwords do not match.",
                    "Smart Hostel Setup", "OK", "Warning") | Out-Null
                continue
            }
            return @{
                Username = $username.Text.Trim()
                Password = $password.Text
            }
        }
    } finally {
        $form.Dispose()
    }
}

function Invoke-MySql([string]$Sql, [string]$Username, [string]$Password) {
    $start = New-Object System.Diagnostics.ProcessStartInfo
    $start.FileName = $mysql
    $start.Arguments = "--protocol=TCP --host=127.0.0.1 --port=$port --user=$Username --batch --skip-column-names"
    if ([string]::IsNullOrEmpty($Password)) {
        $start.Arguments += " --skip-password"
    }
    $start.UseShellExecute = $false
    $start.RedirectStandardInput = $true
    $start.RedirectStandardOutput = $true
    $start.RedirectStandardError = $true
    if (![string]::IsNullOrEmpty($Password)) {
        $start.EnvironmentVariables["MYSQL_PWD"] = $Password
    }
    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $start
    if (!$process.Start()) {
        throw "Could not start the MySQL client."
    }
    $process.StandardInput.Write($Sql)
    $process.StandardInput.Close()
    $stdout = $process.StandardOutput.ReadToEnd()
    $stderr = $process.StandardError.ReadToEnd()
    $process.WaitForExit()
    if ($process.ExitCode -ne 0) {
        throw "MySQL setup failed: $stderr"
    }
    return $stdout
}

function Test-LocalDatabasePort {
    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $pending = $client.BeginConnect("127.0.0.1", $port, $null, $null)
        if (!$pending.AsyncWaitHandle.WaitOne(500, $false)) {
            return $false
        }
        $client.EndConnect($pending)
        return $true
    } catch {
        return $false
    } finally {
        $client.Close()
    }
}

function Start-LocalDatabase([string]$ApplicationPassword) {
    if (Test-LocalDatabasePort) {
        try {
            Invoke-MySql "SELECT 1;" "smart_hostel_app" $ApplicationPassword | Out-Null
            return
        } catch {
            throw "Port $port is already in use by a different service. No database was changed."
        }
    }

    New-Item -ItemType Directory -Path $logDirectory -Force | Out-Null
    $stdoutLog = Join-Path $logDirectory "mysql-server.log"
    $stderrLog = Join-Path $logDirectory "mysql-server-error.log"
    $arguments = "--no-defaults --basedir=`"$($mysqlInstallation.FullName)`" --datadir=`"$dataDirectory`" --port=$port --bind-address=127.0.0.1 --mysqlx=0 --console"
    $server = Start-Process -FilePath $mysqld -ArgumentList $arguments `
        -WorkingDirectory $mysqlInstallation.FullName -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput $stdoutLog -RedirectStandardError $stderrLog
    $ready = $false
    for ($attempt = 0; $attempt -lt 60; $attempt++) {
        Start-Sleep -Seconds 1
        if ($server.HasExited) {
            $detail = Get-Content $stderrLog -ErrorAction SilentlyContinue | Select-Object -Last 20
            throw "The local MySQL server stopped during startup. $($detail -join [Environment]::NewLine)"
        }
        try {
            Invoke-MySql "SELECT 1;" "smart_hostel_app" $ApplicationPassword | Out-Null
            $ready = $true
            break
        } catch {
            Start-Sleep -Milliseconds 500
        }
    }
    if (!$ready) {
        throw "The local database did not become ready. See $stderrLog."
    }
}

try {
    if (!(Test-Path $settingsPath)) {
        if (Test-Path (Join-Path $dataDirectory "mysql")) {
            throw "An existing Smart Hostel database was found but its protected settings are missing. It was left untouched; do not reinitialize it."
        }

        if (!$env:HOSTEL_LAUNCHER_TEST_DIR) {
            [System.Windows.Forms.MessageBox]::Show(
                "The app will create a persistent local database for this Windows account. Your existing project database files will not be modified.",
                "Smart Hostel - First-time Setup", "OK", "Information") | Out-Null
        }
        if ($env:HOSTEL_LAUNCHER_TEST_DIR -and $env:HOSTEL_LAUNCHER_TEST_ADMIN_USERNAME `
                -and $env:HOSTEL_LAUNCHER_TEST_ADMIN_PASSWORD) {
            $adminUsername = $env:HOSTEL_LAUNCHER_TEST_ADMIN_USERNAME
            $adminPassword = $env:HOSTEL_LAUNCHER_TEST_ADMIN_PASSWORD
        } else {
            $credentials = Get-AdministratorSetup
            $adminUsername = $credentials.Username
            $adminPassword = $credentials.Password
        }
        if ([string]::IsNullOrWhiteSpace($adminUsername) -or $adminUsername.Length -gt 50) {
            throw "Administrator username must contain 1 to 50 characters."
        }
        if ($adminPassword.Length -lt 12) {
            throw "Administrator password must be at least 12 characters."
        }

        $applicationPassword = New-RandomHex 32
        $rootPassword = New-RandomHex 32
        New-Item -ItemType Directory -Path $dataDirectory -Force | Out-Null
        $initializeArguments = @("--no-defaults", "--initialize-insecure",
            "--basedir=$($mysqlInstallation.FullName)", "--datadir=$dataDirectory")
        & $mysqld @initializeArguments
        if ($LASTEXITCODE -ne 0) {
            throw "Could not initialize the new local database."
        }

        New-Item -ItemType Directory -Path $logDirectory -Force | Out-Null
        $stdoutLog = Join-Path $logDirectory "mysql-server.log"
        $stderrLog = Join-Path $logDirectory "mysql-server-error.log"
        $serverArguments = "--no-defaults --basedir=`"$($mysqlInstallation.FullName)`" --datadir=`"$dataDirectory`" --port=$port --bind-address=127.0.0.1 --mysqlx=0 --console"
        $server = Start-Process -FilePath $mysqld -ArgumentList $serverArguments `
            -WorkingDirectory $mysqlInstallation.FullName -WindowStyle Hidden -PassThru `
            -RedirectStandardOutput $stdoutLog -RedirectStandardError $stderrLog
        $ready = $false
        for ($attempt = 0; $attempt -lt 60; $attempt++) {
            Start-Sleep -Seconds 1
            if ($server.HasExited) {
                $detail = Get-Content $stderrLog -ErrorAction SilentlyContinue | Select-Object -Last 20
                throw "The local MySQL server stopped during initialization. $($detail -join [Environment]::NewLine)"
            }
            try {
                Invoke-MySql "SELECT 1;" "root" "" | Out-Null
                $ready = $true
                break
            } catch {
                Start-Sleep -Milliseconds 500
            }
        }
        if (!$ready) {
            throw "The new local database did not become ready. See $stderrLog."
        }

        $setupSql = "ALTER USER 'root'@'localhost' IDENTIFIED BY '$rootPassword';" +
            "CREATE DATABASE smart_hostel;" +
            "CREATE USER 'smart_hostel_app'@'localhost' IDENTIFIED BY '$applicationPassword';" +
            "GRANT ALL PRIVILEGES ON smart_hostel.* TO 'smart_hostel_app'@'localhost';"
        Invoke-MySql $setupSql "root" "" | Out-Null

        New-Item -ItemType Directory -Path $stateRoot -Force | Out-Null
        $settings = [ordered]@{
            applicationPassword = Protect-LocalSecret $applicationPassword
            adminUsername = Protect-LocalSecret $adminUsername
            adminPassword = Protect-LocalSecret $adminPassword
        }
        $settings | ConvertTo-Json | Set-Content -Path $settingsPath -Encoding ASCII
        $applicationPassword = $null
        $rootPassword = $null
        $adminPassword = $null
    } else {
        $settings = Get-Content $settingsPath -Raw | ConvertFrom-Json
    }

    $applicationPassword = Unprotect-LocalSecret $settings.applicationPassword
    Start-LocalDatabase $applicationPassword
    if (!(Test-Path (Join-Path $dataDirectory "mysql"))) {
        throw "The persistent database files are missing. No database was recreated."
    }
    $schemaSql = Get-Content (Join-Path $projectRoot "database.sql") -Raw
    $schemaSql = $schemaSql -replace "(?im)^\s*CREATE DATABASE IF NOT EXISTS smart_hostel;\s*", ""
    Invoke-MySql $schemaSql "smart_hostel_app" $applicationPassword | Out-Null

    $env:HOSTEL_DB_URL = $databaseUrl
    $env:HOSTEL_DB_USER = "smart_hostel_app"
    $env:HOSTEL_DB_PASSWORD = $applicationPassword
    $env:HOSTEL_ADMIN_USERNAME = Unprotect-LocalSecret $settings.adminUsername
    $env:HOSTEL_ADMIN_PASSWORD = Unprotect-LocalSecret $settings.adminPassword
    Remove-Item Env:HOSTEL_DEMO_MODE -ErrorAction SilentlyContinue

    $sourceFiles = @((Join-Path $projectRoot "src\Main.java"))
    foreach ($directory in @("model", "security", "database", "dao", "ui")) {
        $sourceFiles += Get-ChildItem (Join-Path $projectRoot "src\$directory") -Filter "*.java" |
            Select-Object -ExpandProperty FullName
    }
    New-Item -ItemType Directory -Path (Join-Path $projectRoot "out") -Force | Out-Null
    & $javaCommand.Source -cp $connector -d (Join-Path $projectRoot "out") $sourceFiles
    if ($LASTEXITCODE -ne 0) {
        throw "Application compilation failed. Review the compiler output."
    }

    Start-Process -FilePath $javaw -ArgumentList @(
        "-cp",
        "`"$(Join-Path $projectRoot 'out');$connector`"",
        "Main"
    ) -WorkingDirectory $projectRoot
} catch {
    [System.Windows.Forms.MessageBox]::Show(
        "Smart Hostel could not start:`r`n`r`n$($_.Exception.Message)",
        "Smart Hostel Startup Error", "OK", "Error") | Out-Null
    exit 1
}
