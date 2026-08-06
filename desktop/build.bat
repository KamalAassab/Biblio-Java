@echo off
REM ===========================================================
REM  BiblioTech desktop - compile only (no launch)
REM
REM  To just use the app, double-click START-DESKTOP-APP.bat in
REM  the project root instead - it builds and launches in one go.
REM
REM  Usage: desktop\build.bat   (works from any directory)
REM  ASCII only: cmd.exe misreads non-ASCII characters under most codepages.
REM ===========================================================
setlocal
set "HERE=%~dp0"
cd /d "%HERE%"

call "%HERE%find-jdk.bat"
if errorlevel 1 exit /b 1

set "CP=lib\postgresql-42.7.4.jar;lib\flatlaf-3.5.4.jar"

if not exist out mkdir out

REM src\Interfaces\ holds the original coursework stubs, superseded by the
REM concrete classes in src\ and excluded from the build.
dir /s /b src\*.java | findstr /v /i "\\Interfaces\\" > "%TEMP%\biblio-sources.txt"

echo Compiling...
"%JDK_BIN%\javac.exe" -encoding UTF-8 -nowarn -cp "%CP%" -d out "@%TEMP%\biblio-sources.txt"
set "RESULT=%ERRORLEVEL%"
del "%TEMP%\biblio-sources.txt" >nul 2>&1

if %RESULT% neq 0 (
  echo Build FAILED.
  exit /b %RESULT%
)
echo Build OK  --^>  desktop\out
endlocal
