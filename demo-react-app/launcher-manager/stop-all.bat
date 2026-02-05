@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion

echo ===========================================
echo 🛑 LAUNCHER MANAGER: ОСТАНОВКА СИСТЕМЫ
echo ===========================================
echo.

set PIDS_DIR=%~dp0..\pids
set LOGS_DIR=%~dp0..\logs
set STOP_TIME=%time%

echo 📁 Поиск PID файлов в: %PIDS_DIR%
echo.

set STOPPED_COUNT=0
set ERROR_COUNT=0

:: Список сервисов для остановки (TransactionSaga удалена)
set SERVICES=ApiGateway Auth User Sklad Delivery Collector Backet Office KefirFrontend

:: 1. Останавливаем через stop-service.bat
for %%s in (%SERVICES%) do (
  echo Останавливаю: %%s
  call "%~dp0\stop-service.bat" %%s
  
  if !errorlevel! equ 0 (
    echo ✅ %%s - остановлен
    set /a STOPPED_COUNT+=1
  ) else (
    echo ❌ %%s - ошибка остановки
    set /a ERROR_COUNT+=1
  )
  
  timeout /t 1 /nobreak >nul
  echo.
)

:: 2. Дополнительная очистка портов (на всякий случай)
echo 🔧 Дополнительная очистка портов...
call :kill_by_port 8080
call :kill_by_port 8097
call :kill_by_port 8081
call :kill_by_port 8082
call :kill_by_port 8088
call :kill_by_port 8086
call :kill_by_port 8083
call :kill_by_port 8085
:: Порт 8090 (TransactionSaga) удален
call :kill_by_port 3000
call :kill_by_port 3333  :: Launcher API

:: 3. Итоги
echo.
echo ===========================================
echo 📊 ИТОГИ ОСТАНОВКИ:
echo ===========================================
echo 📅 Время остановки: %STOP_TIME%
echo.
echo ✅ Успешно остановлено: %STOPPED_COUNT% сервисов
echo ❌ Ошибок остановки: %ERROR_COUNT% сервисов
echo 📋 Всего в системе: 9 сервисов
echo.
if %ERROR_COUNT% gtr 0 (
  echo ⚠️  Некоторые сервисы не были корректно остановлены.
  echo    Возможно, процессы завершены принудительно.
) else (
  echo 🎉 Все сервисы успешно остановлены!
)
echo ===========================================
echo.

pause
exit /b

:kill_by_port
for /f "tokens=5" %%p in ('netstat -ano ^| findstr :%~1') do (
  if not "%%p"=="0" (
    echo   Очистка порта %~1: убиваю PID %%p
    taskkill /PID %%p /F >nul 2>&1
  )
)
goto :eof