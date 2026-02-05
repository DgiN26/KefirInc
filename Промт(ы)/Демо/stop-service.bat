@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion

if "%~1"=="" (
  echo ❌ Использование: stop-service.bat ^<имя_сервиса^>
  exit /b 1
)

set SERVICE_NAME=%~1
set PIDS_DIR=%~dp0..\pids
set LOGS_DIR=%~dp0..\logs

echo 🔍 Остановка сервиса: %SERVICE_NAME%

set PID_FILE=%PIDS_DIR%\%SERVICE_NAME%.pid

if not exist "%PID_FILE%" (
  echo ⚠️  PID файл не найден: %PID_FILE%
  echo 🔍 Попытка найти процесс по порту...
  
  :: Определяем порт по имени
  if "%SERVICE_NAME%"=="ApiGateway" set PORT=8080
  if "%SERVICE_NAME%"=="Auth" set PORT=8097
  if "%SERVICE_NAME%"=="User" set PORT=8081
  if "%SERVICE_NAME%"=="Sklad" set PORT=8082
  if "%SERVICE_NAME%"=="Delivery" set PORT=8088
  if "%SERVICE_NAME%"=="Collector" set PORT=8086
  if "%SERVICE_NAME%"=="Backet" set PORT=8083
  if "%SERVICE_NAME%"=="Office" set PORT=8085
  if "%SERVICE_NAME%"=="TransactionSaga" set PORT=8090
  if "%SERVICE_NAME%"=="KefirFrontend" set PORT=3000
  
  if defined PORT (
    call :kill_by_port %PORT%
  ) else (
    echo ❌ Не могу определить порт для сервиса %SERVICE_NAME%
  )
  
  exit /b 1
)

:: Читаем PID из файла
set /p PID=<"%PID_FILE%"
echo 📝 PID из файла: !PID!

:: Останавливаем процесс
echo 🛑 Останавливаю процесс !PID!...
taskkill /PID !PID! /F >nul 2>&1

if errorlevel 1 (
  echo ⚠️  Не удалось остановить процесс !PID!
  echo 🔍 Возможно процесс уже завершен
) else (
  echo ✅ Процесс !PID! остановлен
)

:: Удаляем PID файл
del "%PID_FILE%" >nul 2>&1
echo 📝 PID файл удален: %PID_FILE%

:: Запись в лог
echo [%date% %time%] Остановлен %SERVICE_NAME% (PID !PID!) >> "%LOGS_DIR%\%SERVICE_NAME%.log"

exit /b 0

:kill_by_port
echo 🔧 Очистка порта %~1...
for /f "tokens=5" %%p in ('netstat -ano ^| findstr :%~1') do (
  if not "%%p"=="0" (
    echo   Найден процесс на порту %~1: PID %%p
    taskkill /PID %%p /F >nul 2>&1
    echo   ✅ Процесс %%p остановлен
  )
)
goto :eof