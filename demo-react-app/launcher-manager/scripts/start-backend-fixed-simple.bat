@echo off
chcp 65001 > nul

echo ========================================
echo ЗАПУСК БЕКЕНД СЕРВИСОВ KEFIR
echo ========================================
echo Время: %date% %time%
echo.

:: РЕШЕНИЕ ПРОБЛЕМЫ СО СКОБКАМИ В ПУТИ
:: Используем два шага для обхода скобок ()
echo ШАГ 1: Переход в папку Backend...
cd /d "C:\Users\2oleg\Downloads"
if not exist "Persona 5 Royal (2022)" (
    echo ОШИБКА: Папка 'Persona 5 Royal (2022)' не найдена
    pause
    exit /b 1
)
cd /d "Persona 5 Royal (2022)\KefirInc\Backend"

:: Проверяем что мы в правильной папке
echo Текущая папка: %cd%
echo.

if not exist "start.bat" (
    echo ОШИБКА: Файл start.bat не найден
    echo Содержимое папки:
    dir *.bat
    pause
    exit /b 1
)

echo УСПЕХ: Найден start.bat
echo.

:: ЗАПУСК СУЩЕСТВУЮЩЕГО START.BAT
echo ШАГ 2: Запуск start.bat...
echo ВАЖНО: Если start.bat содержит PAUSE, нажмите Enter в новом окне
echo.

:: Запускаем существующий start.bat
call start.bat

:: ЖДЕМ ЗАПУСКА СЕРВИСОВ
echo.
echo ШАГ 3: Ожидание запуска сервисов (30 секунд)...
timeout /t 30 /nobreak >nul

:: ПРОВЕРЯЕМ СЕРВИСЫ
echo.
echo ШАГ 4: Проверка запущенных сервисов...
echo.

set CHECK_COUNT=0

:: Проверка Auth
echo Проверка Auth (порт 8097)...
netstat -ano | findstr ":8097" | findstr "LISTENING" >nul
if errorlevel 1 (
    echo   ОШИБКА: Auth не запущен
) else (
    echo   УСПЕХ: Auth запущен
    set /a CHECK_COUNT+=1
)

:: Проверка User
echo Проверка User (порт 8081)...
netstat -ano | findstr ":8081" | findstr "LISTENING" >nul
if errorlevel 1 (
    echo   ОШИБКА: User не запущен
) else (
    echo   УСПЕХ: User запущен
    set /a CHECK_COUNT+=1
)

:: Проверка ApiGateway
echo Проверка ApiGateway (порт 8080)...
netstat -ano | findstr ":8080" | findstr "LISTENING" >nul
if errorlevel 1 (
    echo   ОШИБКА: ApiGateway не запущен
) else (
    echo   УСПЕХ: ApiGateway запущен
    set /a CHECK_COUNT+=1
)

:: ИТОГИ
echo.
echo ========================================
echo ИТОГИ ЗАПУСКА
echo ========================================
echo Время: %date% %time%
echo.
echo Запущено сервисов: %CHECK_COUNT% из 3
echo.

if %CHECK_COUNT% geq 2 (
    echo УСПЕХ: Бекенд сервисы запущены!
    echo.
    echo ДОСТУПНЫЕ СЕРВИСЫ:
    echo - ApiGateway: http://localhost:8080
    echo - Auth: http://localhost:8097
    echo - User: http://localhost:8081
) else (
    echo ПРЕДУПРЕЖДЕНИЕ: Не все сервисы запустились
    echo.
    echo РЕКОМЕНДАЦИИ:
    echo 1. Проверьте логи в папках сервисов
    echo 2. Запустите start.bat вручную из этой папки
)

echo ========================================

:: Выход с кодом успеха
if %CHECK_COUNT% geq 2 exit /b 0
exit /b 1