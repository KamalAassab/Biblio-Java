@echo off
setlocal

set "SRC=%~dp0dist\jpackage\Biblio-Java-Windows-x64"
set "DEST=%LOCALAPPDATA%\Programs\Biblio-Java"
set "CFGDIR=%LOCALAPPDATA%\Biblio-Java"
set "CFGFILE=%CFGDIR%\database.url"

if not exist "%SRC%\Biblio-Java-Windows-x64.exe" (
  echo Missing build output: %SRC%
  exit /b 1
)

if not exist "%CFGDIR%" mkdir "%CFGDIR%"
if not exist "%DEST%" mkdir "%DEST%"

robocopy "%SRC%" "%DEST%" /MIR /NFL /NDL /NJH /NJS >nul
if %ERRORLEVEL% GEQ 8 exit /b %ERRORLEVEL%

if not exist "%CFGFILE%" (
  set /p DBURL=Enter your DATABASE_URL (postgresql://... or jdbc:postgresql://...): 
  > "%CFGFILE%" echo %DBURL%
)

echo Installed to "%DEST%"
echo Database config: "%CFGFILE%"
echo Run: "%DEST%\Biblio-Java-Windows-x64.exe"
endlocal
