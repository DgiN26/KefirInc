@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion

echo ===========================================
echo 🛑 LAUNCHER MANAGER: ОСТАНОВКА СИСТЕМЫ
echo ===========================================

set PIDS_DIR=%~dp0..\pids
set LOGS_DIR=%~dp0..\logs

echo 📁 Поиск PID файлов в: %PIDS_DIR%

set STOPPED_COUNT=0

:: Остановка по PID файлам
for %%f in ("%PIDS_DIR%\*.pid") do (
  set "pid_file=%%f"
  set "service_name=%%~nf"
  
  echo.
  echo Останавливаю: !service_name!
  
  set /p pid=<!pid_file!
  echo 📝 PID: !pid!
  
  taskkill /PID !pid! /F >nul 2>&1
  
  if errorlevel 1 (
    echo ⚠️  Процесс !pid! не найден или уже остановлен
  ) else (
    echo ✅ Остановлен
    set /a STOPPED_COUNT+=1
  )
  
  :: Удаление PID файла
  del "!pid_file!" >nul 2>&1
  
  :: Запись в лог
  echo [%date% %time%] Остановлен !service_name! (PID !pid!) >> "%LOGS_DIR%\!service_name!.log"
  
  timeout /t 1 /nobreak >nul
)

echo.
echo ===========================================
echo 📊 ИТОГО ОСТАНОВЛЕНО: !STOPPED_COUNT! сервисов
echo ===========================================

:: Дополнительная очистка портов на случай если остались процессы
echo.
echo 🔧 Дополнительная очистка портов KEFIR...
call :kill_by_port 8080
call :kill_by_port 8097
call :kill_by_port 8081
call :kill_by_port 8082
call :kill_by_port 8088
call :kill_by_port 8086
call :kill_by_port 8083
call :kill_by_port 8085
call :kill_by_port 8090
call :kill_by_port 3000

echo.
echo ✅ Система полностью остановлена
pause
exit /b

:kill_by_port
for /f "tokens=5" %%p in ('netstat -ano ^| findstr :%~1') do (
  if not "%%p"=="0" (
    echo Очистка порта %~1: убиваю PID %%p
    taskkill /PID %%p /F >nul 2>&1
  )
)
goto :eof