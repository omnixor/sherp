# Smart Hostel Management System

S3 OOP academic prototype covering the first 50% of the project.

## Team Modules

- Jeevan Geo Vadakken: login, admin dashboard, JDBC connection, exception handling
- Jasna Fathima V U: `Student` class and student CRUD module
- Jeeva Sajeesh: `Room` class and room allocation module
- Jeril M Reji: `Complaint` class and complaint status module

## Setup

The application currently runs in **dummy database mode** by default. It starts with sample admin, student, and room records and does not require MySQL for a demonstration. The dummy data is kept in memory and resets when the application closes.

For the JDBC/MySQL version, MySQL Server 8.4 and Connector/J are installed for this workspace. Start the local server when needed by running this from the project folder:

```powershell
& "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe" --datadir="$PWD\mysql-data" --port=3306 --console
```

For the current dummy-data demonstration, compile and run without MySQL:

```powershell
javac -d out src\Main.java src\model\*.java src\database\*.java src\dao\*.java src\ui\*.java
java -cp "out" Main
```

To prepare the retained JDBC implementation, create the database by running `database.sql`:

```powershell
Get-Content .\database.sql | & "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" -u root
```

Then, if needed, edit the username and password in `src/database/DBConnection.java` and compile from the project folder:

```text
javac -cp "lib/mysql-connector-j-<version>.jar" -d out src\Main.java src\model\*.java src\database\*.java src\dao\*.java src\ui\*.java
```

Run the JDBC version with:

```text
java -cp "out;lib/mysql-connector-j-<version>.jar" Main
```

Default admin login: `admin` / `admin123`

Student registration: click `Register as Student` on the login screen and enter the student details. After registration, select `Student` and enter the new student ID. Student login is intentionally basic and has no password in this prototype.

## Current 50% Scope

The prototype includes admin login, student CRUD, room add/view/allocate/vacate, complaint submission and viewing, and admin complaint status updates. It uses Swing, JDBC, MySQL, and simple DAO interfaces.

The second half remains intentionally open for complete integration, extensive testing, bug fixing, UI refinement, additional validation, final documentation, and presentation preparation.

## What We Can Explain in the 50% Review

- Classes and objects: model objects are created by the UI and DAO classes.
- Encapsulation: model data is private and accessed through getters and setters.
- Inheritance: `Student` and `Admin` extend the common `User` class.
- Abstraction: `User` is abstract and requires role-specific behavior.
- Polymorphism: `getRoleDescription()` is overridden by each user type.
- Interfaces: DAO interfaces define the database operations used by their implementations.
- Exception handling: invalid input and `SQLException` are handled in the UI and DAO layers.
- JDBC: `DBConnection` and prepared statements connect the Java application to MySQL.
