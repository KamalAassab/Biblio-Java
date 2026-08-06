@echo off
REM ===========================================================
REM  BiblioTech - START THE WEB APP
REM
REM  Double-click this file, then open http://localhost:3000
REM
REM  It installs npm dependencies the first time, then starts the
REM  Next.js development server. Close this window to stop it.
REM
REM  ASCII only: cmd.exe misreads non-ASCII characters under most codepages.
REM ===========================================================
title BiblioTech - Web
cd /d "%~dp0"

echo.
echo   ============================================
echo     BiblioTech - Gestion de Bibliotheque
echo     Web application (Next.js + React)
echo   ============================================
echo.

REM ---- Locate Node -------------------------------------------
set "NODE_BIN="
for %%I in (npm.cmd) do if not "%%~$PATH:I"=="" set "NODE_BIN=%%~dp$PATH:I"

if not defined NODE_BIN (
  for /d %%D in ("%USERPROFILE%\nodejs\node-v*" "%ProgramFiles%\nodejs") do (
    if exist "%%~D\npm.cmd" set "NODE_BIN=%%~D\"
  )
)

if not defined NODE_BIN (
  echo   Could not find Node.js.
  echo   Install the LTS build from https://nodejs.org
  echo.
  pause
  exit /b 1
)

set "PATH=%NODE_BIN%;%PATH%"

REM ---- Configuration check -----------------------------------
if not exist "web\.env.local" (
  echo   Missing web\.env.local
  echo.
  echo   Copy web\.env.local.example to web\.env.local and fill in
  echo   DATABASE_URL and SESSION_SECRET before starting.
  echo.
  pause
  exit /b 1
)

REM ---- Dependencies ------------------------------------------
if not exist "web\node_modules" (
  echo   First run - installing dependencies. This takes a few minutes.
  echo.
  call npm --prefix web install
  if errorlevel 1 (
    echo.
    echo   npm install failed. Check your network connection and retry.
    echo.
    pause
    exit /b 1
  )
)

REM ---- Start -------------------------------------------------
echo.
echo   Starting the server, then opening your browser...
echo   Close this window to stop it.
echo.
start "" http://localhost:3000
call npm --prefix web run dev

pause
