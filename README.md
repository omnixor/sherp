# Smart Hostel Management System

A Java desktop application for managing hostel students, rooms, allocations, and complaints. It uses Swing for the interface and MySQL for persistent storage.

Before running the app, download MySQL Connector/J 9.4.0 from the [official MySQL Connector/J downloads](https://dev.mysql.com/downloads/connector/j/) page and place the extracted `mysql-connector-j-9.4.0.jar` in `lib\`. The JAR is excluded from this repository.

## Choose how to run it

| Option | Data | Best for |
| --- | --- | --- |
| `run-hostel.ps1` | Saved on this PC | Normal use on Windows |
| `run-demo.ps1` | Temporary; cleared when the app closes | A quick demonstration |
| Manual setup | Saved in a MySQL database you configure | Development or an existing MySQL installation |

### Windows: persistent app (recommended)

You need JDK 17 or newer, MySQL Server 8, and the Connector/J file included at `lib\mysql-connector-j-9.4.0.jar`. Open PowerShell in the project folder and run:

```powershell
.\run-hostel.ps1
```

On first run, the launcher asks you to create an administrator account, then creates a local database and starts the app. On later runs it reuses that database. The data is stored under `%LOCALAPPDATA%\SmartHostel` and remains after the app closes or Windows restarts. Back up that folder before reinstalling Windows or moving to another PC.

The launcher runs MySQL only on this PC, on port `33307`. It does not use or modify the `mysql-data` folder in the project. Keep the project folder, MySQL installation, and your Windows user profile available for future launches.

### Quick demo (temporary data)

You need JDK 17 or newer and the included Connector/J file. Run:

```powershell
.\run-demo.ps1
```

The script asks you to choose a demo administrator username and password, then compiles and starts the application. Demo data is held in memory and is lost when the app exits. Do not use this mode for information you need to keep.

### Manual MySQL setup

Use this option if you want the application to connect to a MySQL database you manage. You need MySQL Server 8 and JDK 17 or newer.

1. Create the database and tables with an account that can create databases. Adjust the MySQL executable path if MySQL is installed elsewhere:

   ```powershell
   Get-Content .\database.sql | & "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" -u <setup-user> -p
   ```

2. Create a dedicated MySQL application account with access to `smart_hostel`, then set its connection details and the initial administrator credentials in PowerShell. Replace each placeholder with your own value; do not commit real credentials to source files.

   ```powershell
   $env:HOSTEL_DB_URL = "jdbc:mysql://localhost:3306/smart_hostel"
   $env:HOSTEL_DB_USER = "<application-user>"
   $env:HOSTEL_DB_PASSWORD = "<application-password>"
   $env:HOSTEL_ADMIN_USERNAME = "<initial-admin-name>"
   $env:HOSTEL_ADMIN_PASSWORD = "<unique-password-of-at-least-12-characters>"
   ```

3. Compile and start the app:

   ```powershell
   javac -cp "lib\mysql-connector-j-9.4.0.jar" -d out src\Main.java src\model\*.java src\security\*.java src\database\*.java src\dao\*.java src\ui\*.java
   java -cp "out;lib\mysql-connector-j-9.4.0.jar" Main
   ```

The app updates older schemas when it starts. If the database has no administrator, the first login attempt creates one using `HOSTEL_ADMIN_USERNAME` and `HOSTEL_ADMIN_PASSWORD`. Remove those two environment variables after the administrator is created. Existing student records are kept, but students without a password must have one set by an administrator before they can sign in.

## What the app can do

- Administrators can manage students and rooms, allocate or vacate rooms, review complaints, and view dashboard totals.
- Students can view their profile, room-allocation history, complaints, and notifications.
- Complaints have categories, status history, and resolution notes. Students are notified when a complaint changes.
- Student, room, and complaint lists support filtering and pagination.

Student registration requires a unique email address and a password of at least 12 characters. Room allocation and vacating update student records and allocation history together. Rooms with allocation history cannot be deleted, and students with complaint or allocation history cannot be deleted.

## Security notes

The app checks login, administrator/student roles, and student access to their own records. Passwords are stored as PBKDF2-HMAC-SHA256 hashes. After five failed login attempts for the same account and role within 15 minutes, further attempts are rate-limited. Sessions expire after 30 minutes without keyboard or mouse activity.

The persistent app records login attempts, denied actions, logouts, and authorized data changes in an audit log. Administrators can view the latest 200 events. Audit entries do not contain passwords or password hashes. If an audit write fails after a data operation, that operation may already have succeeded; check the database before retrying it.

This is a desktop application, not a network server. Client-side access checks cannot stop someone who can modify the app or connect directly to MySQL. Use a dedicated database account with only the required privileges, and protect the computer and its credentials.

## Run the smoke tests

From PowerShell in the project folder, compile the app and tests:

```powershell
javac --release 17 -cp "lib\mysql-connector-j-9.4.0.jar" -d out src\Main.java src\model\*.java src\security\*.java src\database\*.java src\dao\*.java src\ui\*.java tests\WorkflowSmokeTest.java tests\AuthorizationSmokeTest.java
```

Run the workflow test:

```powershell
java -cp "out;lib\mysql-connector-j-9.4.0.jar" WorkflowSmokeTest
```

Run the authorization test in demo mode with test administrator credentials:

```powershell
$env:HOSTEL_DEMO_MODE = "true"
$env:HOSTEL_DEMO_ADMIN_USERNAME = "<demo-admin-name>"
$env:HOSTEL_DEMO_ADMIN_PASSWORD = "<demo-password-of-at-least-12-characters>"
java -cp "out;lib\mysql-connector-j-9.4.0.jar" AuthorizationSmokeTest
```

The smoke tests use in-memory demo data. They do not verify a live MySQL connection.

## Current limitations

- The app runs as a single desktop process; it is not a networked web application.
- Filtering and pagination happen in the desktop client, not in database queries.
- Room building, floor, and facility details are not implemented.