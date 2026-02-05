@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion

:: Использование: check-port.bat <порт> [таймаут_сек]
:: Возвращает: errorlevel 0 - порт открыт, 1 - не открыт

if "%~1"=="" (
  echo ❌ Использование: check-port.bat ^<порт^> [таймаут_сек]
  exit /b 1
)

set PORT=%~1
set TIMEOUT=%~2
if "%TIMEOUT%"=="" set TIMEOUT=30

echo 🔍 Проверка порта %PORT% (таймаут: %TIMEOUT% сек)...

for /l %%i in (1,1,%TIMEOUT%) do (
  :: Проверяем порт через PowerShell Test-NetConnection
  powershell -Command "Test-NetConnection -ComputerName localhost -Port %PORT% -WarningAction SilentlyContinue -InformationLevel Quiet"
  
  if !errorlevel! equ 0 (
    echo ✅ Порт %PORT% открыт (через %%i сек)
    exit /b 0
  )
  
  if %%i lss %TIMEOUT% (
    timeout /t 1 /nobreak >nul
  )
)

echo ❌ Порт %PORT% не открылся за %TIMEOUT% секунд
exit /b 1