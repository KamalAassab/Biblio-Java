@echo off
REM ===========================================================
REM  BiblioTech desktop - build if needed, then launch
REM  Usage: desktop\run.bat   (works from any directory)
REM ===========================================================
setlocal
set "HERE=%~dp0"
cd /d "%HERE%"

if not exist "out\GUI_Main.class" (
  call "%HERE%build.bat"
  if errorlevel 1 exit /b 1
)

if defined JAVA_HOME (
  set "JAVA=%JAVA_HOME%\bin\java.exe"
) else (
  set "JAVA=java"
)

"%JAVA%" -Dfile.encoding=UTF-8 -cp "out;lib\postgresql-42.7.4.jar;lib\flatlaf-3.5.4.jar" GUI_Main
endlocal
