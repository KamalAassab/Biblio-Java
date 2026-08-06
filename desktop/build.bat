@echo off
REM ===========================================================
REM  BiblioTech desktop - compile
REM  Usage: desktop\build.bat   (works from any directory)
REM  Set JAVA_HOME to pick a specific JDK; otherwise javac on PATH is used.
REM  ASCII only: cmd.exe misreads non-ASCII characters under most codepages.
REM ===========================================================
setlocal
set "HERE=%~dp0"
cd /d "%HERE%"

if defined JAVA_HOME (
  set "JAVAC=%JAVA_HOME%\bin\javac.exe"
) else (
  set "JAVAC=javac"
)

set "CP=lib\postgresql-42.7.4.jar;lib\flatlaf-3.5.4.jar"

if not exist out mkdir out

REM Interfaces\ holds the original coursework stubs, superseded by the concrete
REM classes in src\ and excluded from the build.
dir /s /b src\*.java | findstr /v /i "\\Interfaces\\" > "%TEMP%\biblio-sources.txt"

echo Compiling...
"%JAVAC%" -encoding UTF-8 -nowarn -cp "%CP%" -d out "@%TEMP%\biblio-sources.txt"
set "RESULT=%ERRORLEVEL%"
del "%TEMP%\biblio-sources.txt" >nul 2>&1

if %RESULT% neq 0 (
  echo Build FAILED.
  exit /b %RESULT%
)
echo Build OK  --^>  desktop\out
endlocal
