@echo off
REM ===========================================================
REM  BiblioTech - START THE DESKTOP APP
REM
REM  Double-click this file. That is all.
REM
REM  It compiles the Java sources in desktop\src and launches the
REM  application. The window below stays open if something fails,
REM  so you can read the error.
REM
REM  ASCII only: cmd.exe misreads non-ASCII characters under most codepages.
REM ===========================================================
title BiblioTech - Desktop
cd /d "%~dp0"

echo.
echo   ============================================
echo     BiblioTech - Gestion de Bibliotheque
echo     Desktop application (Java 17 + Swing)
echo   ============================================
echo.

call "desktop\run.bat"
set "RESULT=%ERRORLEVEL%"

if %RESULT% neq 0 (
  echo.
  echo   --------------------------------------------
  echo     The application did not start.
  echo     Read the message above for the reason.
  echo   --------------------------------------------
  echo.
  echo   Most common cause: no database configured.
  echo   Copy .env.example to .env and fill in DATABASE_URL.
  echo.
  pause
  exit /b %RESULT%
)

exit /b 0
