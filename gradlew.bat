@echo off
setlocal
set "APP_HOME=%~dp0"
set "GRADLE_VERSION=8.14.3"
set "DIST_URL=https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip"
if "%GRADLE_USER_HOME%"=="" set "GRADLE_USER_HOME=%USERPROFILE%\.gradle"
set "CACHE_DIR=%GRADLE_USER_HOME%\wrapper\dists\gradle-%GRADLE_VERSION%"
set "DIST_DIR=%CACHE_DIR%\gradle-%GRADLE_VERSION%"
set "ZIP_FILE=%CACHE_DIR%\gradle-%GRADLE_VERSION%-bin.zip"

if exist "%DIST_DIR%\bin\gradle.bat" goto run
if not exist "%CACHE_DIR%" mkdir "%CACHE_DIR%"
if not exist "%ZIP_FILE%" (
  echo Downloading Gradle %GRADLE_VERSION%...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing '%DIST_URL%' -OutFile '%ZIP_FILE%'"
  if errorlevel 1 exit /b 1
)
powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Force '%ZIP_FILE%' '%CACHE_DIR%'"
if errorlevel 1 exit /b 1

:run
call "%DIST_DIR%\bin\gradle.bat" --no-daemon %*
exit /b %ERRORLEVEL%
