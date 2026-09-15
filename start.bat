@echo off
rem =====================================================
rem  408 exam training system - backend startup script
rem  Frontend is pre-built into backend/src/main/resources/static
rem  Requires: JDK 8+, Maven 3.x (offline deps cached in ~/.m2)
rem =====================================================
cd /d "%~dp0backend"

where mvn >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Maven not found in PATH.
    pause
    exit /b 1
)

where java >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Java not found in PATH.
    pause
    exit /b 1
)

echo [1/2] Building backend (offline)...
mvn -o -q package -DskipTests
if errorlevel 1 (
    echo [ERROR] Build failed.
    pause
    exit /b 1
)

echo [2/2] Starting exam408 on http://localhost:8081
start "exam408" java -jar target\exam408-1.0.0.jar
echo [OK] Application is starting, see its console window for logs.