@echo off
chcp 65001 > nul

set SERVICES=ApiGateway Auth User Sklad Delivery Collector Backet Office TransactionSaga
set COUNT=0

for %%s in (%SERVICES%) do call :start_service %%s

echo.
echo Сервисов запущено: %COUNT%
pause
exit /b

:start_service
if not exist "%1\" (
    echo %1: ❌ Папка не найдена
    goto :eof
)

cd /d "%1"

if not exist mvnw.cmd (
    echo %1: ❌ mvnw.cmd не найден
    cd ..
    goto :eof
)

start "%1" /B cmd /c "mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local" >nul 2>&1

timeout /t 2 /nobreak >nul
echo %1: ✅ Запущен

set /a COUNT+=1
cd ..
goto :eof