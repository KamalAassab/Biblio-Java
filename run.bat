@echo off
setlocal
set "ROOT=%~dp0"
if exist "%ROOT%.env" for /f "usebackq tokens=1,* delims==" %%a in ("%ROOT%.env") do (
    if not "%%a"=="" if not "%%a"=="#" set "%%a=%%b"
)
"%ROOT%runtime\bin\java.exe" -cp "%ROOT%out;%ROOT%lib\postgresql-42.7.4.jar;%ROOT%lib\flatlaf-3.5.4.jar" GUI_Main
endlocal
