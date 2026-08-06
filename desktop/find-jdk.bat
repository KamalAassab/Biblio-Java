@echo off
REM ===========================================================
REM  BiblioTech desktop - locate a JDK
REM
REM  Sets JDK_BIN to a folder containing java.exe and javac.exe.
REM  Called by build.bat and run.bat; not meant to be run directly.
REM
REM  Search order:
REM    1. JAVA_HOME
REM    2. javac.exe already on PATH
REM    3. Common install locations, including portable unzipped JDKs
REM       under %USERPROFILE%\Downloads and %USERPROFILE%\Apps
REM
REM  ASCII only: cmd.exe misreads non-ASCII characters under most codepages.
REM ===========================================================

set "JDK_BIN="

REM ---- 1. JAVA_HOME ------------------------------------------
if defined JAVA_HOME (
  if exist "%JAVA_HOME%\bin\javac.exe" (
    set "JDK_BIN=%JAVA_HOME%\bin"
    goto :found
  )
)

REM ---- 2. Already on PATH ------------------------------------
for %%I in (javac.exe) do (
  if not "%%~$PATH:I"=="" (
    set "JDK_BIN=%%~dp$PATH:I"
    goto :found
  )
)

REM ---- 3. Common install locations ---------------------------
REM Each entry is a parent folder that may hold one or more JDKs.
for %%R in (
  "%ProgramFiles%\Java"
  "%ProgramFiles%\Eclipse Adoptium"
  "%ProgramFiles%\Microsoft\jdk"
  "%ProgramFiles%\Amazon Corretto"
  "%ProgramFiles%\Zulu"
  "%LOCALAPPDATA%\Programs\Eclipse Adoptium"
  "%USERPROFILE%\Apps"
  "%USERPROFILE%\Downloads\Images\Apps"
  "%USERPROFILE%\Downloads"
) do (
  if exist "%%~R" (
    for /d %%D in ("%%~R\*jdk*") do (
      if exist "%%~D\bin\javac.exe" (
        set "JDK_BIN=%%~D\bin"
        goto :found
      )
    )
    REM Some distributions name the folder after the vendor, not "jdk".
    for /d %%D in ("%%~R\*") do (
      if exist "%%~D\bin\javac.exe" (
        set "JDK_BIN=%%~D\bin"
        goto :found
      )
    )
  )
)

REM ---- Not found ---------------------------------------------
echo.
echo   Could not find a Java Development Kit (JDK 17 or newer).
echo.
echo   Fix it in one of two ways:
echo.
echo     A. Install Temurin JDK 17 from https://adoptium.net
echo        and tick "Set JAVA_HOME" during setup.
echo.
echo     B. If you already have a JDK unzipped somewhere, point
echo        JAVA_HOME at it, for example:
echo.
echo          setx JAVA_HOME "C:\path\to\jdk-17"
echo.
echo        Then close this window and open a new one.
echo.
exit /b 1

:found
exit /b 0
