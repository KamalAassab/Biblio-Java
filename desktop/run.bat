@echo off
REM ===========================================================
REM  BiblioTech desktop - compile, then launch
REM
REM  To just use the app, double-click START-DESKTOP-APP.bat in
REM  the project root - it calls this script and keeps the window
REM  open if anything goes wrong.
REM
REM  Compiling takes a second or two and guarantees you are running
REM  your latest edits, which matches how desktop\build.sh behaves.
REM
REM  Usage: desktop\run.bat   (works from any directory)
REM  ASCII only: cmd.exe misreads non-ASCII characters under most codepages.
REM ===========================================================
setlocal
set "HERE=%~dp0"
cd /d "%HERE%"

call "%HERE%build.bat"
if errorlevel 1 exit /b 1

call "%HERE%find-jdk.bat"
if errorlevel 1 exit /b 1

echo Starting BiblioTech...
"%JDK_BIN%\java.exe" -Dfile.encoding=UTF-8 -cp "out;lib\postgresql-42.7.4.jar;lib\flatlaf-3.5.4.jar" GUI_Main
exit /b %ERRORLEVEL%
