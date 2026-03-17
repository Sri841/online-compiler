# CodeForge — Online Compiler (No Docker Required)

Works with only IntelliJ + MySQL.
Java runs automatically. C/C++ needs MinGW (Windows) or gcc (Mac/Linux).

## ── SETUP IN 4 STEPS ──────────────────────────────────────

### Step 1: Run the database schema
Open MySQL (via IntelliJ or terminal) and run:
  database/schema.sql

### Step 2: Set your MySQL password
Open:  online-compiler/src/main/resources/application.properties
Change:  spring.datasource.password=YOUR_MYSQL_PASSWORD_HERE

### Step 3: Open the Spring Boot project in IntelliJ
  File → Open → select the "online-compiler" folder (has pom.xml)
  Wait for Maven to download dependencies
  Run OnlineCompilerApplication.java

### Step 4: Open frontend
  Open frontend/index.html in Chrome or Firefox

## ── WANT C / C++ SUPPORT? ──────────────────────────────────

### Windows — Install MinGW:
  1. Download: https://sourceforge.net/projects/mingw/
  2. Install, select: mingw32-gcc-g++
  3. Add C:\MinGW\bin to your Windows PATH
  4. Test: open Command Prompt → type: gcc --version
  5. Update application.properties:
       compiler.gcc.path=C:/MinGW/bin/gcc.exe
       compiler.gpp.path=C:/MinGW/bin/g++.exe

### Mac:
  xcode-select --install
  (this installs clang which works as gcc/g++)

### Linux (Ubuntu/Debian):
  sudo apt install gcc g++ -y

## ── PROJECT STRUCTURE ──────────────────────────────────────

  online-compiler/          ← OPEN THIS IN INTELLIJ
  ├── pom.xml
  └── src/main/java/com/compiler/
      ├── OnlineCompilerApplication.java   ← main class, run this
      ├── controller/
      │   └── CompilerController.java      ← REST API /api/run
      ├── service/
      │   └── CodeExecutionService.java    ← compiles & runs code
      ├── model/
      │   ├── Submission.java              ← database table
      │   ├── CodeRequest.java             ← incoming JSON
      │   └── CodeResponse.java            ← outgoing JSON
      ├── repository/
      │   └── SubmissionRepository.java    ← database queries
      └── config/
          └── WebConfig.java               ← CORS settings

  frontend/
  ├── index.html   ← open in browser
  ├── styles.css
  └── script.js

  database/
  └── schema.sql   ← run this in MySQL first

## ── API ENDPOINTS ──────────────────────────────────────────

  POST http://localhost:8080/api/run
  GET  http://localhost:8080/api/submissions
  GET  http://localhost:8080/api/health

## ── TROUBLESHOOTING ────────────────────────────────────────

  Problem: "Backend not reachable"
  Fix: Make sure OnlineCompilerApplication is running in IntelliJ

  Problem: "gcc not found" for C/C++
  Fix: Install MinGW and update compiler paths in application.properties

  Problem: MySQL connection error
  Fix: Check your password in application.properties

  Problem: Java code works but C/C++ gives error
  Fix: See "WANT C/C++ SUPPORT" section above

