@echo off
setlocal

if defined MAVEN_HOME if exist "%MAVEN_HOME%\bin\mvn.cmd" (
  call "%MAVEN_HOME%\bin\mvn.cmd" %*
  exit /b %ERRORLEVEL%
)

where mvn.cmd >nul 2>nul
if %ERRORLEVEL% EQU 0 (
  call mvn.cmd %*
  exit /b %ERRORLEVEL%
)

set "IDEA_MAVEN=%ProgramFiles%\JetBrains\IntelliJ IDEA 2026.1.4\plugins\maven\lib\maven3\bin\mvn.cmd"
if exist "%IDEA_MAVEN%" (
  call "%IDEA_MAVEN%" %*
  exit /b %ERRORLEVEL%
)

echo Maven 3.9+ was not found. Install Maven, set MAVEN_HOME, or run through IntelliJ's Maven tool window.
exit /b 1
