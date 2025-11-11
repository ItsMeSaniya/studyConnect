@echo off
REM Run script for StudyConnect (Maven build)

echo ========================================
echo Running StudyConnect Application
echo ========================================
echo.

if not exist target\StudyConnect-1.0.0.jar (
    echo JAR file not found! Please build first using build.bat
    pause
    exit /b 1
)

REM Run StudyConnect P2P Application
java -cp "target\StudyConnect-1.0.0.jar;target\lib\*" main.StudyConnectMain
pause
