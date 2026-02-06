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
set SCRIPTS_DIR=%~dp0

echo 🔍 Запуск сервиса: %SERVICE_NAME%

:: Определяем порт по имени сервиса
if "%SERVICE_NAME%"=="ApiGateway" set PORT=8080
if "%SERVICE_NAME%"=="Auth" set PORT=8097
if "%SERVICE_NAME%"=="User" set PORT=8081
if "%SERVICE_NAME%"=="Sklad" set PORT=8082
if "%SERVICE_NAME%"=="Delivery" set PORT=8088
if "%SERVICE_NAME%"=="Collector" set PORT=8086
if "%SERVICE_NAME%"=="Backet" set PORT=8083
if "%SERVICE_NAME%"=="Office" set PORT=8085
:: TransactionSaga удалена - порт 8090 не используется
if "%SERVICE_NAME%"=="KefirFrontend" set PORT=3000

:: Если это фронтенд
if "%SERVICE_NAME%"=="KefirFrontend" (
  echo 🚀 Запуск KefirFrontend (логистическая система)...
  
  if not exist "%KEFIR_FRONTEND_DIR%" (
    echo ❌ Папка не найдена: %KEFIR_FRONTEND_DIR%
    exit /b 1
  )
  
  :: Проверяем, не запущен ли уже фронтенд
  call "%SCRIPTS_DIR%\check-port.bat" %PORT% 2 >nul
  if !errorlevel! equ 0 (
    echo ⚠️  Фронтенд уже запущен на порту %PORT%
    exit /b 0
  )
  
  cd /d "%KEFIR_FRONTEND_DIR%"
  
  :: Запускаем npm start в новом окне
  start "KefirLogisticsSystem" cmd /c "npm start"
  
  :: Ждем запуска фронтенда (React может собираться долго)
  echo ⏳ Ожидание сборки фронтенда (до 60 сек)...
  call "%SCRIPTS_DIR%\check-port.bat" %PORT% 60
  
  if !errorlevel! equ 0 (
    :: Сохраняем PID (для фронтенда используем специальный маркер)
    echo FRONTEND_RUNNING > "%PIDS_DIR%\KefirFrontend.pid"
    
    :: Открываем браузер
    timeout /t 2 /nobreak >nul
    start "" "http://localhost:3000"
    
    echo ✅ KefirFrontend запущен: http://localhost:3000
    echo [%date% %time%] Запущен KefirFrontend (порт 3000) >> "%LOGS_DIR%\KefirFrontend.log"
    exit /b 0
  ) else (
    echo ❌ KefirFrontend не запустился за 60 секунд
    exit /b 1
  )
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

:: Проверяем, не запущен ли уже сервис
call "%SCRIPTS_DIR%\check-port.bat" %PORT% 2 >nul
if !errorlevel! equ 0 (
  echo ⚠️  Сервис уже запущен на порту %PORT%
  exit /b 0
)

:: Останавливаем если уже запущен (по PID файлу)
if exist "%PIDS_DIR%\%SERVICE_NAME%.pid" (
  set /p OLD_PID=<"%PIDS_DIR%\%SERVICE_NAME%.pid"
  echo ⚠️  Обнаружен старый PID: !OLD_PID!, останавливаю...
  "%SCRIPTS_DIR%\stop-service.bat" %SERVICE_NAME% >nul 2>&1
  timeout /t 3 /nobreak >nul
)

echo 🚀 Запуск %SERVICE_NAME% на порту %PORT%...

:: Запускаем Spring Boot
start "%SERVICE_NAME%" /B cmd /c "mvnw.cmd spring-boot:run -Dserver.port=%PORT% -DskipTests"

:: Получаем PID (через timeout, так как start возвращает управление сразу)
timeout /t 1 /nobreak >nul

:: Ищем PID процесса по порту
for /f "tokens=5" %%p in ('netstat -ano ^| findstr :%PORT% ^| findstr LISTENING') do (
  set NEW_PID=%%p
  goto :found_pid
)

:found_pid
if not defined NEW_PID (
  echo ⚠️  Не удалось получить PID, пробуем подождать и проверить порт...
  
  :: Ждем запуска сервиса
  call "%SCRIPTS_DIR%\check-port.bat" %PORT% 30
  
  if !errorlevel! equ 0 (
    echo ✅ %SERVICE_NAME% запущен (проверка порта), но PID не определен
    echo PORT_CHECK_ONLY > "%PIDS_DIR%\%SERVICE_NAME%.pid"
  ) else (
    echo ❌ %SERVICE_NAME% не запустился за 30 секунд
    exit /b 1
  )
) else (
  echo !NEW_PID! > "%PIDS_DIR%\%SERVICE_NAME%.pid"
  echo ✅ %SERVICE_NAME% запущен (PID: !NEW_PID!, порт: %PORT%)
)

:: Запись в лог
echo [%date% %time%] Запущен %SERVICE_NAME% (порт %PORT%, PID !NEW_PID!) >> "%LOGS_DIR%\%SERVICE_NAME%.log"

exit /b 0