@echo off
chcp 65001 >nul
title 🚀 KEFIR Logistics System Launcher
color 0A

echo.
echo ╔═══════════════════════════════════════════════════════╗
echo ║                🚀 Запуск системы KEFIR Logistics     ║
echo ╚═══════════════════════════════════════════════════════╝
echo.

echo 📊 Проверка Java...
java -version 2>nul
if errorlevel 1 (
    echo ❌ Java не установлена или не настроена PATH
    echo Установите Java 17+ и добавьте в переменные окружения
    pause
    exit /b 1
)

echo.
echo 🛑 Останавливаем все предыдущие процессы...
taskkill /F /IM java.exe 2>nul
timeout /t 2 /nobreak >nul

echo.
echo 📁 Переходим в папку Backend...
cd /d "%~dp0Backend" 2>nul
if errorlevel 1 (
    echo ❌ Папка Backend не найдена!
    echo Запустите скрипт из корневой папки проекта
    pause
    exit /b 1
)

echo.
echo 📡 Создаем папку для логов...
if not exist logs mkdir logs

echo.
rem ==========================================
rem API Gateway (порт 8080)
rem ==========================================
if exist ApiGateWay (
    echo 🏗️  Запуск API Gateway (порт: 8080)...
    cd ApiGateWay
    
    rem Создаем конфигурацию для отключения БД
    (
    echo spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
    echo spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
    echo server.port=8080
    echo spring.application.name=api-gateway
    echo logging.level.com.example=INFO
    echo logging.level.org.springframework=WARN
    ) > src/main/resources/application-local.properties
    
    echo 📝 Запускаем API Gateway...
    start "📡 API Gateway (8080)" cmd /c "title 📡 API Gateway (8080) && mvn spring-boot:run -Dspring-boot.run.profiles=local > ..\logs\api-gateway.log 2>&1"
    
    echo ✅ API Gateway запускается...
    cd ..
    
    rem Ждем запуска
    echo ⏳ Ждем запуска API Gateway (15 сек)...
    timeout /t 15 /nobreak >nul
) else (
    echo ❌ Папка ApiGateWay не найдена
)

echo.
rem ==========================================
rem Saga Service (порт 8090)
rem ==========================================
if exist SagaService (
    echo 🔄 Запуск Saga Service (порт: 8090)...
    cd SagaService
    
    echo 📝 Запускаем Saga Service...
    start "🔄 Saga Service (8090)" cmd /c "title 🔄 Saga Service (8090) && mvn spring-boot:run > ..\logs\saga-service.log 2>&1"
    
    echo ✅ Saga Service запускается...
    cd ..
    
    rem Ждем немного
    timeout /t 5 /nobreak >nul
) else (
    echo ⚠ Папка SagaService не найдена (пропускаем)
)

echo.
rem ==========================================
rem Auth Service (порт 8097)
rem ==========================================
if exist Auth (
    echo 🔐 Запуск Auth Service (порт: 8097)...
    cd Auth
    
    rem Проверяем конфигурацию
    if not exist src/main/resources/application.properties (
        echo ⚠ Создаем конфигурацию для Auth...
        (
        echo server.port=8097
        echo spring.application.name=auth-service
        echo spring.datasource.url=jdbc:postgresql://localhost:5432/kefir_db
        echo spring.datasource.username=postgres
        echo spring.datasource.password=Ghbdtnbr123^!
        echo spring.jpa.hibernate.ddl-auto=update
        ) > src/main/resources/application.properties
    )
    
    echo 📝 Запускаем Auth Service...
    start "🔐 Auth Service (8097)" cmd /c "title 🔐 Auth Service (8097) && mvn spring-boot:run > ..\logs\auth-service.log 2>&1"
    
    echo ✅ Auth Service запускается...
    cd ..
    
    rem Ждем немного
    timeout /t 5 /nobreak >nul
) else (
    echo ⚠ Папка Auth не найдена (пропускаем)
)

echo.
rem ==========================================
rem Остальные сервисы
rem ==========================================
echo 📦 Запуск основных сервисов...
echo.

set SERVICES=User Sklad Delivery Collector Backet Office
for %%S in (%SERVICES%) do (
    if exist %%S (
        echo ▶ Запуск %%S Service...
        cd %%S
        
        rem Определяем порт для сервиса
        set "PORT="
        if "%%S"=="User" set "PORT=8081"
        if "%%S"=="Sklad" set "PORT=8082"
        if "%%S"=="Backet" set "PORT=8083"
        if "%%S"=="Office" set "PORT=8085"
        if "%%S"=="Collector" set "PORT=8086"
        if "%%S"=="Delivery" set "PORT=8088"
        
        rem Запускаем сервис
        if defined PORT (
            start "%%S Service (!PORT!)" cmd /c "title %%S Service (!PORT!) && mvn spring-boot:run -Dserver.port=!PORT! > ..\logs\%%S-service.log 2>&1"
            echo ✅ %%S Service запускается (порт: !PORT!)...
        ) else (
            start "%%S Service" cmd /c "title %%S Service && mvn spring-boot:run > ..\logs\%%S-service.log 2>&1"
            echo ✅ %%S Service запускается...
        )
        
        cd ..
        
        rem Пауза между запусками
        timeout /t 3 /nobreak >nul
    ) else (
        echo ❌ Папка %%S не найдена
    )
)

echo.
echo ⏳ Даем время на запуск всех сервисов (20 секунд)...
timeout /t 20 /nobreak >nul

echo.
echo ╔═══════════════════════════════════════════════════════╗
echo ║                    🎉 СИСТЕМА ЗАПУЩЕНА!              ║
echo ╚═══════════════════════════════════════════════════════╝
echo.
echo 📡 АДРЕСА СЕРВИСОВ:
echo.
echo 🌐 API Gateway:    http://localhost:8080
echo 🔄 Saga Service:   http://localhost:8090
echo 🔐 Auth Service:   http://localhost:8097
echo 👤 User Service:   http://localhost:8081
echo 📦 Sklad Service:  http://localhost:8082
echo 🛒 Backet Service: http://localhost:8083
echo 🏢 Office Service: http://localhost:8085
echo 👷 Collector:      http://localhost:8086
echo 🚚 Delivery:       http://localhost:8088
echo.
echo 📋 УТИЛИТЫ:
echo.
echo 📊 Проверить состояние: check-services.bat
echo 📝 Просмотреть логи:    view-logs.bat
echo 🚦 Остановить все:      stop-all.bat
echo 🔧 Перезапустить:       restart.bat
echo.
echo 💡 Для демонстрации откройте: http://localhost:8080
echo.
pause