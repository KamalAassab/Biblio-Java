@echo off
setlocal
set "ROOT=%~dp0"
set "APPDIR=%TEMP%\BiblioJava-%RANDOM%"
mkdir "%APPDIR%" >nul 2>&1
tar -xf "%ROOT%Biblio-Java-Windows-x64.zip" -C "%APPDIR%"
if errorlevel 1 exit /b 1
call "%APPDIR%\Biblio-Java-Windows-x64\run.bat"
endlocal
