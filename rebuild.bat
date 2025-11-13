@echo off
echo ======================================
echo Stopping all Java processes...
echo ======================================
taskkill /F /IM java.exe >nul 2>&1
timeout /t 2 /nobreak >nul

echo.
echo ======================================
echo Cleaning and rebuilding project...
echo ======================================
cd /d "%~dp0"
call mvn clean package -DskipTests

echo.
echo ======================================
echo Build complete!
echo ======================================
echo.
echo You can now run:
echo   - Server: java -jar target\StudyConnect-1.0.0.jar server
echo   - Client: java -jar target\StudyConnect-1.0.0.jar client
echo.
pause
