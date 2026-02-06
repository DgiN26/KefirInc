@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion

if "%~1"=="" (
  echo ❌ Использование: start-service.bat ^<имя_сервиса^>
  exit /b 1
)

set SERVICE_NAME=%~1
set BACKEND_DIR=%~dp0..\..\..\Backend
set KEFIR_FRONTEND_DIR=%~dp0..\..\..\kefir-react-app
set PIDS_DIR=%~dp0..\pids
set LOGS_DIR=%~dp0..\logs

echo 🔍 Поиск сервиса: %SERVICE_NAME%

:: Определяем порт по имени сервиса
if "%SERVICE_NAME%"=="ApiGateway" set PORT=8080
if "%SERVICE_NAME%"=="Auth" set PORT=8097
if "%SERVICE_NAME%"=="User" set PORT=8081
if "%SERVICE_NAME%"=="Sklad" set PORT=8082
if "%SERVICE_NAME%"=="Delivery" set PORT=8088
if "%SERVICE_NAME%"=="Collector" set PORT=8086
if "%SERVICE_NAME%"=="Backet" set PORT=8083
if "%SERVICE_NAME%"=="Office" set PORT=8085
if "%SERVICE_NAME%"=="TransactionSaga" set PORT=8090

:: Если это фронтенд
if "%SERVICE_NAME%"=="KefirFrontend" (
  echo 🚀 Запуск KefirFrontend (логистическая система)...
  
  if not exist "%KEFIR_FRONTEND_DIR%" (
    echo ❌ Папка не найдена: %KEFIR_FRONTEND_DIR%
    exit /b 1
  )
  
  cd /d "%KEFIR_FRONTEND_DIR%"
  start "KefirLogisticsSystem" /B cmd /c "npm start"
  echo !errorlevel! > "%PIDS_DIR%\KefirFrontend.pid"
  
  echo ✅ KefirFrontend запущен: http://localhost:3000
  exit /b 0
)

:: Если это бекенд сервис
if not defined PORT (
  echo ❌ Неизвестный сервис: %SERVICE_NAME%
  exit /b 1
)

echo 📍 Порт: %PORT%

if not exist "%BACKEND_DIR%\%SERVICE_NAME%\" (
  echo ❌ Папка не найдена: %BACKEND_DIR%\%SERVICE_NAME%
  exit /b 1
)

cd /d "%BACKEND_DIR%\%SERVICE_NAME%"

if not exist mvnw.cmd (
  echo ❌ mvnw.cmd не найден
  exit /b 1
)

echo 🚀 Запуск %SERVICE_NAME% на порту %PORT%...

:: Останавливаем если уже запущен
if exist "%PIDS_DIR%\%SERVICE_NAME%.pid" (
  set /p OLD_PID=<"%PIDS_DIR%\%SERVICE_NAME%.pid"
  echo ⚠️  Сервис уже запущен (PID: !OLD_PID!), останавливаю...
  taskkill /PID !OLD_PID! /F >nul 2>&1
  timeout /t 2 /nobreak >nul
)

:: Запускаем
start "%SERVICE_NAME%" /B cmd /c "mvnw.cmd spring-boot:run -Dserver.port=%PORT% -DskipTests"
set NEW_PID=!errorlevel!
echo !NEW_PID! > "%PIDS_DIR%\%SERVICE_NAME%.pid"

echo ✅ %SERVICE_NAME% запущен (PID: !NEW_PID!, порт: %PORT%)
echo 📝 PID сохранен в: %PIDS_DIR%\%SERVICE_NAME%.pid

:: Запись в лог
echo [%date% %time%] Запущен %SERVICE_NAME% (порт %PORT%, PID !NEW_PID!) >> "%LOGS_DIR%\%SERVICE_NAME%.log"

exit /b 0