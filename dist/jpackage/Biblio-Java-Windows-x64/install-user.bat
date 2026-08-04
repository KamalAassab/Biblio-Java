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

if exist "%CFGFILE%" goto :done_cfg

echo Enter your DATABASE_URL [postgresql://... or jdbc:postgresql://...]:
set /p DBURL=
> "%CFGFILE%" echo %DBURL%

:done_cfg
echo Installed to "%DEST%"
echo Database config: "%CFGFILE%"
echo Run: "%DEST%\Biblio-Java-Windows-x64.exe"
endlocal
