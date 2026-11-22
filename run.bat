@echo off
setlocal EnableExtensions
pushd "%~dp0"

echo ==============================================
echo         CAMPUSHUB AI - SETUP ^& LAUNCHER
echo ==============================================

where javac >nul 2>nul
if errorlevel 1 (
  echo [ERROR] JDK not found. Install JDK 17 or newer and add it to PATH.
  pause
  popd
  exit /b 1
)

if not exist "lib\mysql-connector-j.jar" (
  echo [ERROR] MySQL Connector/J not found at: %CD%\lib\mysql-connector-j.jar
  pause
  popd
  exit /b 1
)

if exist out rmdir /s /q out
mkdir out

echo Compiling CampusHub...
javac -encoding UTF-8 -cp "lib\mysql-connector-j.jar" -d out Member_1\Java_Files\*.java Member_2\Java_Files\*.java Member_3\Java_Files\*.java Member_4\Java_Files\*.java
if errorlevel 1 (
  echo.
  echo [ERROR] Compilation failed.
  pause
  popd
  exit /b 1
)

echo Compilation successful!
echo.
java -cp "out;lib\mysql-connector-j.jar" CampusHubApp
set "APP_EXIT=%ERRORLEVEL%"
echo.
pause
popd
exit /b %APP_EXIT%
