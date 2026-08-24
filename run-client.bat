@echo off
setlocal
cd /d "%~dp0"

where java >nul 2>nul
if errorlevel 1 (
    echo Java was not found. Restart Windows, then try again.
    pause
    exit /b 1
)

call gradlew.bat runClient --no-configuration-cache
if errorlevel 1 (
    echo.
    echo The client did not start. Keep this window open so the error can be inspected.
    pause
)

endlocal
