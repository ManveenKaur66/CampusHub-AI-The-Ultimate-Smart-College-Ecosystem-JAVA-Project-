# CampusHub AI — Java CLI Project

CampusHub AI is a Java command-line application for students and faculty backed by MySQL.

## Project structure

```text
CampusHub/
├── Member_1/Java_Files/   # Authentication, users, student roster, MySQL store
├── Member_2/Java_Files/   # Campus directory and CSV utilities
├── Member_3/Java_Files/   # Assignments and notifications
├── Member_4/Java_Files/   # Main application, DB connection, events, Java demos
├── Database/
│   └── complete_database.sql
├── lib/
│   └── mysql-connector-j.jar
├── run.bat
└── README.md
```

## Requirements

- JDK 17 or newer (`javac` and `java` in PATH)
- MySQL Server 8.0 or newer running locally

## Quick Start

1. Make sure MySQL Server is running.
2. Open PowerShell or Command Prompt inside the `CampusHub` folder and run:

```powershell
.\run.bat
```

3. Enter your MySQL username (default `root`) and MySQL password when prompted.
   The application automatically creates/migrates the `campushub` database, tables, and the 73 approved student records if not already present.
