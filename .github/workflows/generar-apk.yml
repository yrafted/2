@echo off
setlocal EnableDelayedExpansion
cd /d "%~dp0"

echo ============================================
echo   COLECTIVOS ROSARIO - GENERADOR DE APK
echo ============================================
echo.

if not exist local.properties (
  if exist "%LOCALAPPDATA%\Android\Sdk" (
    set "SDK=%LOCALAPPDATA%\Android\Sdk"
    set "SDK=!SDK:\=/!"
    > local.properties echo sdk.dir=!SDK!
  )
)

where java >nul 2>nul
if errorlevel 1 (
  echo No se encontro Java. Abri este proyecto con Android Studio,
  echo o instala Android Studio y volve a ejecutar este archivo.
  pause
  exit /b 1
)

call gradlew.bat assembleDebug
if errorlevel 1 (
  echo.
  echo No se pudo compilar. La forma mas simple es abrir la carpeta
  echo en Android Studio, dejar que instale SDK 34 y volver a ejecutar.
  pause
  exit /b 1
)

copy /Y "app\build\outputs\apk\debug\app-debug.apk" "ColectivosRosario.apk" >nul

echo.
echo LISTO: %CD%\ColectivosRosario.apk
echo Copialo al telefono e instalalo.
echo.
pause
