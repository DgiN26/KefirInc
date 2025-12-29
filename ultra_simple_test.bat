@echo off
chcp 65001 >nul
title KEFIR Демо

:MAIN
cls
echo 🎭 ДЕМО КАСКАДНЫХ ОШИБОК KEFIR
echo ================================
echo.
echo [1] Проверить БД
echo [2] Создать тестовые данные
echo [3] Сценарий 1: Нормальный процесс
echo [4] Сценарий 3: Каскадные ошибки
echo [5] Выход
echo.
set /p c="Выбор: "

if "%c%"=="1" goto CHECK
if "%c%"=="2" goto PREPARE
if "%c%"=="3" goto SCEN1
if "%c%"=="4" goto SCEN3
if "%c%"=="5" exit

goto MAIN

:CHECK
psql -U postgres -d kefir_db -c "SELECT 'БД работает', now();"
pause
goto MAIN

:PREPARE
psql -U postgres -d kefir_db -c "CREATE TABLE IF NOT EXISTS products(id SERIAL, name TEXT, stock INT);"
echo Данные созданы
pause
goto MAIN

:SCEN1
echo Нормальный процесс...
pause
goto MAIN

:SCEN3
echo Каскадные ошибки...
pause
goto MAIN