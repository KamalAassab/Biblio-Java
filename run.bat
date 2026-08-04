@echo off
setlocal
set "ROOT=%~dp0"
"%ROOT%runtime\bin\java.exe" -cp "%ROOT%out;%ROOT%lib\postgresql-42.7.4.jar;%ROOT%lib\flatlaf-3.5.4.jar" GUI_Main
endlocal
