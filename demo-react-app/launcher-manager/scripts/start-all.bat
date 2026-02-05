@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion

echo ===========================================
echo 🚀 LAUNCHER MANAGER: ЗАПУСК ВСЕЙ СИСТЕМЫ KEFIR
echo ===========================================
echo 📅 Время начала: %date% %time%
echo.

set BACKEND_DIR=%~dp0..\..\..\Backend
set FRONTEND_DIR=%~dp0..\..\..\kefir-react-app
set SCRIPTS_DIR=%~dp0
set LOGS_DIR=%~dp0..\logs

:: Создаем папку для логов если нет
if not exist "%LOGS_DIR%" mkdir "%LOGS_DIR%"

echo 📍 Директории:
echo   Backend: %BACKEND_DIR%
echo   Frontend: %FRONTEND_DIR%
echo   Logs: %LOGS_DIR%
echo.

:: 1. ПРЕДВАРИТЕЛЬНАЯ ОЧИСТКА
echo 🔧 ШАГ 1: Предварительная очистка...
echo   1.1. Останавливаем все сервисы...

:: Используем существующий stop.bat в Backend если есть
if exist "%BACKEND_DIR%\stop.bat" (
  echo   Использую существующий stop.bat...
  cd /d "%BACKEND_DIR%"
  call stop.bat >nul 2>&1
) else (
  echo   Останавливаю вручную...
  call :kill_port 8080
  call :kill_port 8097
  call :kill_port 8081
  call :kill_port 8082
  call :kill_port 8088
  call :kill_port 8086
  call :kill_port 8083
  call :kill_port 8085
  call :kill_port 3000
)

timeout /t 3 /nobreak >nul
echo   ✅ Очистка завершена
echo.

:: 2. ЗАПУСК BACKEND СЕРВИСОВ
echo 🔧 ШАГ 2: Запуск бекенд сервисов...
echo.

:: Вариант A: Используем существующий start.bat если он работает
if exist "%BACKEND_DIR%\start.bat" (
  echo   🚀 Использую существующий start.bat...
  cd /d "%BACKEND_DIR%"
  
  :: Запускаем start.bat и ждем завершения
  start "KefirBackendServices" /B cmd /c "start.bat"
  
  echo   ⏳ Ожидаю запуска бекенд сервисов (30 сек)...
  timeout /t 30 /nobreak >nul
  
  :: Проверяем ключевые порты
  echo   🔍 Проверка запущенных сервисов...
  call :check_port_and_log 8097 "Auth"
  call :check_port_and_log 8081 "User"
  call :check_port_and_log 8083 "Backet"
  call :check_port_and_log 8080 "ApiGateway"
  
) else (
  :: Вариант B: Запускаем вручную по очереди
  echo   🚀 Запускаю сервисы вручную...
  
  set SERVICES=Auth User Sklad Delivery Collector Backet Office ApiGateway
  set SUCCESS_COUNT=0
  
  for %%s in (%SERVICES%) do (
    echo   🚀 Запуск %%s...
    
    if exist "%BACKEND_DIR%\%%s\" (
      cd /d "%BACKEND_DIR%\%%s"
      
      :: Определяем порт
      if "%%s"=="ApiGateway" set PORT=8080
      if "%%s"=="Auth" set PORT=8097
      if "%%s"=="User" set PORT=8081
      if "%%s"=="Sklad" set PORT=8082
      if "%%s"=="Delivery" set PORT=8088
      if "%%s"=="Collector" set PORT=8086
      if "%%s"=="Backet" set PORT=8083
      if "%%s"=="Office" set PORT=8085
      
      :: Запускаем Spring Boot
      if exist "mvnw.cmd" (
        start "%%s" /B cmd /c "mvnw.cmd spring-boot:run -Dserver.port=!PORT! -DskipTests"
        echo   ⏳ Ожидаю 5 секунд...
        timeout /t 5 /nobreak >nul
        
        :: Проверяем порт
        call :check_port !PORT!
        if !errorlevel! equ 0 (
          echo   ✅ %%s запущен (порт !PORT!)
          set /a SUCCESS_COUNT+=1
        ) else (
          echo   ⚠️  %%s не запустился (порт !PORT!)
        )
      ) else (
        echo   ❌ mvnw.cmd не найден для %%s
      )
    ) else (
      echo   ❌ Папка не найдена: %%s
    )
    
    echo.
  )
  
  echo   📊 Запущено бекенд сервисов: !SUCCESS_COUNT! из 8
)

echo.
:: 3. ЗАПУСК FRONTEND
echo 🔧 ШАГ 3: Запуск фронтенда...
echo.

if not exist "%FRONTEND_DIR%" (
  echo ❌ Папка фронтенда не найдена: %FRONTEND_DIR%
  goto :summary
)

:: Проверяем, не запущен ли уже фронтенд
call :check_port 3000
if !errorlevel! equ 0 (
  echo ⚠️  Фронтенд уже запущен на порту 3000
  goto :open_browser
)

cd /d "%FRONTEND_DIR%"

echo 🚀 Запускаю React приложение...
echo ⚠️  ВНИМАНИЕ: npm start может спросить про порт 3001
echo    Если порт 3000 занят, нажмите 'Y' в новом окне
echo.

:: Запускаем npm start в новом окне (видимом)
start "KefirFrontend" cmd /k "npm start"

echo ⏳ Ожидаю запуска фронтенда (40 секунд - React собирается долго)...
timeout /t 40 /nobreak >nul

:: Проверяем фронтенд
call :check_port 3000
if !errorlevel! equ 0 (
  echo ✅ Фронтенд запущен на порту 3000
  goto :open_browser
)

:: Пробуем порт 3001 (если пользователь выбрал другой порт)
call :check_port 3001
if !errorlevel! equ 0 (
  echo ✅ Фронтенд запущен на порту 3001
  set FRONTEND_PORT=3001
  goto :open_browser
)

echo ❌ Фронтенд не запустился за 40 секунд
goto :summary

:open_browser
echo 🌐 Открываю браузер...
timeout /t 2 /nobreak >nul

if defined FRONTEND_PORT (
  start "" "http://localhost:%FRONTEND_PORT%"
  echo ✅ Браузер открыт: http://localhost:%FRONTEND_PORT%
) else (
  start "" "http://localhost:3000"
  echo ✅ Браузер открыт: http://localhost:3000"
)

:summary
echo.
echo ===========================================
echo 📊 ИТОГИ ЗАПУСКА
echo ===========================================
echo 📅 Время начала: %START_TIME%
echo 📅 Время окончания: %time%
echo.
echo 🌐 СЕРВИСЫ:
echo   - Frontend: http://localhost:3000 (или 3001)
echo   - Auth API: http://localhost:8097
echo   - ApiGateway: http://localhost:8080
echo   - Launcher API: http://localhost:3333
echo   - Демо-панель: http://localhost:3099
echo.
echo 📋 РУЧНАЯ ПРОВЕРКА:
echo   1. Откройте http://localhost:3000
echo   2. Если видите "KEFIR Logistics" - фронтенд работает
echo   3. Если авторизация работает - бекенд работает
echo.
echo 🔧 ЕСЛИ НЕ РАБОТАЕТ:
echo   1. Проверьте логи в: %LOGS_DIR%
echo   2. Запустите сервисы вручную через start.bat в Backend
echo   3. Запустите фронтенд: cd to kefir-react-app, npm start
echo ===========================================
echo.

:: Запись в лог
echo [%date% %time%] Запуск системы выполнен >> "%LOGS_DIR%\system-start.log"

pause
exit /b 0

:: ============ ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ ============

:check_port
:: Проверка порта - возвращает errorlevel 0 если открыт
setlocal
set PORT=%~1

:: Простая проверка через netstat
netstat -ano | findstr ":%PORT%[^0-9]" | findstr "LISTENING" >nul
endlocal & exit /b %errorlevel%

:check_port_and_log
:: Проверка порта с логированием
setlocal
set PORT=%~1
set SERVICE=%~2

call :check_port %PORT%
if !errorlevel! equ 0 (
  echo     ✅ %SERVICE% запущен (порт %PORT%)
  echo [%date% %time%] %SERVICE% запущен на порту %PORT% >> "%LOGS_DIR%\system-start.log"
) else (
  echo     ❌ %SERVICE% НЕ запущен (порт %PORT%)
  echo [%date% %time%] %SERVICE% НЕ запущен на порту %PORT% >> "%LOGS_DIR%\system-start.log"
)
endlocal
goto :eof

:kill_port
:: Убить все процессы на порту
setlocal
set PORT=%~1

echo   Очистка порта %PORT%...
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":%PORT%[^0-9]"') do (
  if not "%%p"=="0" (
    echo     Найден процесс: PID %%p
    taskkill /PID %%p /F >nul 2>&1
    timeout /t 1 /nobreak >nul
  )
)
endlocal
goto :eof

:START_TIME
echo %time%
goto :eof