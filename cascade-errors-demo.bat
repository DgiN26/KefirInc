

@echo off
chcp 65001 >nul
setlocal EnableDelayedExpansion
title 🎭 Демо каскадных ошибок - Transaction Saga
color 0A

:: Конфигурация
set DB_HOST=localhost
set DB_PORT=5432
set DB_NAME=kefir_db
set DB_USER=postgres
set SAGA_URL=http://localhost:8090/transaction-saga

:: Запрашиваем пароль у пользователя
echo ===============================================================================
echo 🎭 ДЕМОНСТРАЦИЯ КАСКАДНЫХ ОШИБОК - ОДНА БД: %DB_NAME%
echo ===============================================================================
echo.
echo 📊 Используемая база: %DB_NAME%
echo 👤 Пользователь: %DB_USER%
echo.
echo 🔑 Введите пароль для PostgreSQL (по умолчанию: Ghbdtnbr123)
echo.
set /p DB_PASSWORD_INPUT="🔐 Пароль: "
if "!DB_PASSWORD_INPUT!"=="" set DB_PASSWORD_INPUT=Ghbdtnbr123!
set DB_PASSWORD=!DB_PASSWORD_INPUT!

echo.
echo 🔍 Проверка доступности сервисов...
echo.

:: Глобальные переменные
set TRANSACTION_ID=
set ORDER_ID=
set CLIENT_ID=DEMO-CLIENT-%RANDOM%
set COLLECTOR_ID=DEMO-COLLECTOR-%RANDOM%

echo ===============================================================================
echo 🎭 ДЕМОНСТРАЦИЯ КАСКАДНЫХ ОШИБОК - ОДНА БД: %DB_NAME%
echo ===============================================================================
echo.
echo 📊 Используемая база: %DB_NAME%
echo 🌐 Transaction Saga: %SAGA_URL%
echo 🔐 Пользователь: %DB_USER%
echo.

:: Проверка подключения
echo 🔍 Проверка доступности сервисов...
echo.

:: Сначала проверяем Saga
echo 📡 Проверяем Transaction Saga...
curl -s -o nul -w "%%{http_code}" "%SAGA_URL%/api/health" > response.txt 2>nul
set /p SAGA_STATUS=<response.txt
del response.txt 2>nul

if "%SAGA_STATUS%"=="200" (
    echo ✅ Transaction Saga доступен (HTTP %SAGA_STATUS%)
) else (
    echo ⚠ Transaction Saga не отвечает (HTTP: %SAGA_STATUS%)
    echo 💡 Запустите сервис на порту 8090
    echo.
)

echo.
echo 🗄️ Проверяем подключение к БД %DB_NAME%...
:: Используем PGPASSWORD переменную
echo Используя пароль: %DB_PASSWORD%
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -c "SELECT '✅ БД подключена', current_timestamp;" 2>nul
if %errorlevel% equ 0 (
    echo ✅ База данных %DB_NAME% доступна
    set DB_AVAILABLE=1
) else (
    echo ⚠ Не удалось подключиться к БД %DB_NAME%
    echo.
    echo 🔧 Попробуем создать БД если её нет...
    psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -c "CREATE DATABASE %DB_NAME%;" 2>nul
    if %errorlevel% equ 0 (
        echo ✅ БД %DB_NAME% создана
        set DB_AVAILABLE=1
    ) else (
        echo ❌ Не удалось создать БД
        echo.
        echo 📋 Проверьте:
        echo 1. Запущен ли PostgreSQL
        echo 2. Правильный ли пароль: %DB_PASSWORD%
        echo 3. Существует ли БД %DB_NAME%
        echo.
        echo 💡 Команда для проверки: psql -U postgres -c "\l"
        set DB_AVAILABLE=0
    )
)

echo.
if "%SAGA_STATUS%" neq "200" (
    echo ⚠ ВНИМАНИЕ: Transaction Saga не доступен
    echo Некоторые функции демо будут ограничены
    echo.
)

if "%DB_AVAILABLE%"=="0" (
    echo ⚠ ВНИМАНИЕ: БД не доступна
    echo Демо будет использовать симуляцию данных
    echo.
)

echo 📢 Демо можно запускать в любом случае!
echo Нажмите любую клавишу для продолжения...
pause >nul

:MAIN_MENU
cls
echo ===============================================================================
echo 🎭 ДЕМО КАСКАДНЫХ ОШИБОК - ГЛАВНОЕ МЕНЮ
echo ===============================================================================
if "%SAGA_STATUS%"=="200" (
    echo 🌐 Saga: %SAGA_URL% ✅
) else (
    echo 🌐 Saga: %SAGA_URL% ⚠ (симуляция)
)

if "%DB_AVAILABLE%"=="1" (
    echo 🗄️ БД: %DB_NAME% ✅
) else (
    echo 🗄️ БД: %DB_NAME% ⚠ (симуляция)
)
echo.
echo [1] Подготовить тестовые данные в БД
echo [2] Сценарий 1: Нормальный процесс (базовый)
echo [3] Сценарий 2: Один отсутствующий товар
echo [4] Сценарий 3: Каскадные ошибки (скрытые дефициты) 🔥
echo [5] Сценарий 4: Клиент требует ВСЕ товары
echo [6] Сценарий 5: Ночной кошмар (магазины закрыты)
echo [7] Показать текущее состояние БД
echo [8] Очистить тестовые данные
echo [9] Мониторинг в реальном времени
echo [0] Выход
echo.
set /p choice="Ваш выбор (0-9): "

if "%choice%"=="1" goto PREPARE_DATA
if "%choice%"=="2" goto SCENARIO_1
if "%choice%"=="3" goto SCENARIO_2
if "%choice%"=="4" goto SCENARIO_3
if "%choice%"=="5" goto SCENARIO_4
if "%choice%"=="6" goto SCENARIO_5
if "%choice%"=="7" goto SHOW_DB_STATE
if "%choice%"=="8" goto CLEAR_DATA
if "%choice%"=="9" goto REAL_TIME_MONITOR
if "%choice%"=="0" exit /b
goto MAIN_MENU

:PREPARE_DATA
cls
echo ===============================================================================
echo 📊 ПОДГОТОВКА ТЕСТОВЫХ ДАННЫХ В БД: %DB_NAME%
echo ===============================================================================
echo.

if "%DB_AVAILABLE%"=="0" (
    echo ⚠ БД не доступна, используем симуляцию
    echo 📦 Тестовые данные будут храниться в памяти
    goto :DATA_PREPARED
)

echo 🗃️ Создаем таблицы если их нет...
:: Используем PGPASSWORD
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -c "
-- Проверяем существование таблиц
CREATE TABLE IF NOT EXISTS products (
    id SERIAL PRIMARY KEY,
    product_id VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    stock INTEGER DEFAULT 0,
    warehouse VARCHAR(50) DEFAULT 'main',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS demo_orders (
    id SERIAL PRIMARY KEY,
    order_id VARCHAR(50) UNIQUE NOT NULL,
    client_id VARCHAR(50) NOT NULL,
    status VARCHAR(20) DEFAULT 'pending',
    total_items INTEGER DEFAULT 0,
    delivered_items INTEGER DEFAULT 0,
    missing_items INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS demo_transactions (
    id SERIAL PRIMARY KEY,
    transaction_id VARCHAR(50) UNIQUE NOT NULL,
    order_id VARCHAR(50) NOT NULL,
    status VARCHAR(20) DEFAULT 'CREATED',
    missing_items JSONB DEFAULT '{}',
    compensation_amount DECIMAL(10,2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
" 2>nul && echo ✅ Таблицы созданы/проверены

echo.
echo 📦 Добавляем тестовые товары...
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -c "
INSERT INTO products (product_id, name, price, stock, warehouse) VALUES
('MILK-1L', 'Молоко 1л', 89.99, 50, 'main'),
('BREAD', 'Хлеб', 45.50, 30, 'main'),
('BUTTER', 'Масло', 199.99, 20, 'main'),
('EGGS', 'Яйца 10шт', 129.99, 100, 'main'),
('CHEESE', 'Сыр', 349.99, 15, 'main'),
('YOGURT', 'Йогурт', 59.99, 40, 'main'),
('MEAT', 'Мясо', 499.99, 25, 'cold'),
('VEGGIES', 'Овощи', 199.99, 60, 'cold'),
('FRUITS', 'Фрукты', 299.99, 35, 'cold'),
('WATER', 'Вода 5л', 99.99, 100, 'main')
ON CONFLICT (product_id) DO UPDATE SET
stock = EXCLUDED.stock,
warehouse = EXCLUDED.warehouse;
" 2>nul && echo ✅ Товары добавлены/обновлены

echo.
:DATA_PREPARED
echo 📊 Создаем демо-клиента и сборщика...
set CLIENT_ID=CLIENT-%RANDOM%
set COLLECTOR_ID=COLLECTOR-%RANDOM%

echo.
echo ✅ Данные подготовлены!
echo 👤 Клиент: %CLIENT_ID%
echo 👷 Сборщик: %COLLECTOR_ID%
echo.
pause
goto MAIN_MENU

:SCENARIO_1
cls
echo ===============================================================================
echo 📈 СЦЕНАРИЙ 1: НОРМАЛЬНЫЙ ПРОЦЕСС
echo ===============================================================================
echo.

echo 🛒 Создаем нормальный заказ...
set ORDER_ID=ORDER-NORMAL-%RANDOM%

echo 📝 Пытаемся создать транзакцию через Saga API...
if "%SAGA_STATUS%"=="200" (
    curl -s -X POST "%SAGA_URL%/api/transactions" ^
      -H "Content-Type: application/json" ^
      -d "{ \
        \"orderId\": \"%ORDER_ID%\", \
        \"collectorId\": \"%COLLECTOR_ID%\", \
        \"clientId\": \"%CLIENT_ID%\" \
      }" > temp_response.json 2>nul
    
    call :EXTRACT_JSON_FIELD "transactionId" temp_response.json
    set TRANSACTION_ID=%JSON_VALUE%
    
    if "%TRANSACTION_ID%"=="" (
        echo ⚠ Не удалось создать транзакцию (Saga может быть не готов)
        echo 💡 Используем симуляцию
        set TRANSACTION_ID=SIM-TX-%RANDOM%
    ) else (
        echo ✅ Транзакция создана: %TRANSACTION_ID%
    )
) else (
    echo ⚠ Saga не доступен, используем симуляцию
    set TRANSACTION_ID=SIM-TX-%RANDOM%
    echo ✅ Симуляция транзакции: %TRANSACTION_ID%
)

echo.
echo 📦 Симулируем сканирование товаров...
echo 📊 Остатки до сканирования:
call :SHOW_PRODUCTS_STOCK "MILK-1L,BREAD,BUTTER,EGGS,CHEESE"

echo.
for %%P in (MILK-1L BREAD BUTTER EGGS CHEESE) do (
    echo - Сканируем %%P...
    
    if "%DB_AVAILABLE%"=="1" (
        :: РЕАЛЬНОЕ уменьшение в БД
        psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -c ^
            "UPDATE products SET stock = GREATEST(0, stock - 1) WHERE product_id = '%%P';" 2>nul && echo "  ✅ Уменьшили остаток"
    ) else (
        echo "  ✅ Симуляция сканирования"
    )
    
    timeout /t 1 /nobreak >nul
)

echo.
echo ✅ Все товары отсканированы!
echo.

if "%DB_AVAILABLE%"=="1" (
    echo 📊 Остатки после сканирования:
    call :SHOW_PRODUCTS_STOCK "MILK-1L,BREAD,BUTTER,EGGS,CHEESE"
) else (
    echo 📊 Симуляция: все товары успешно собраны
)

echo.
pause
goto MAIN_MENU

:SCENARIO_2
cls
echo ===============================================================================
echo ⚠ СЦЕНАРИЙ 2: ОДИН ОТСУТСТВУЮЩИЙ ТОВАР
echo ===============================================================================

echo 🔍 Подготовка: создаем дефицит одного товара...
if "%DB_AVAILABLE%"=="1" (
    psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -c ^
        "UPDATE products SET stock = 0 WHERE product_id = 'YOGURT';" 2>nul && echo "✅ Йогурт теперь отсутствует на складе"
) else (
    echo "✅ Симуляция: йогурт отсутствует на складе"
)

echo.
echo 🛒 Создаем заказ с проблемным товаром...
set ORDER_ID=ORDER-ONE-MISSING-%RANDOM%

echo 📝 Пытаемся создать транзакцию...
if "%SAGA_STATUS%"=="200" (
    curl -s -X POST "%SAGA_URL%/api/transactions" ^
      -H "Content-Type: application/json" ^
      -d "{ \
        \"orderId\": \"%ORDER_ID%\", \
        \"collectorId\": \"%COLLECTOR_ID%\", \
        \"clientId\": \"%CLIENT_ID%\", \
        \"items\": [ \
          {\"productId\": \"MILK-1L\", \"quantity\": 1}, \
          {\"productId\": \"BREAD\", \"quantity\": 1}, \
          {\"productId\": \"YOGURT\", \"quantity\": 2} \
        ] \
      }" > temp_response.json 2>nul
    
    call :EXTRACT_JSON_FIELD "transactionId" temp_response.json
    set TRANSACTION_ID=%JSON_VALUE%
) else (
    set TRANSACTION_ID=SIM-TX-%RANDOM%
)

echo.
echo 🔍 Проверяем наличие товаров перед сборкой...
call :SHOW_PRODUCTS_STOCK "MILK-1L,BREAD,YOGURT"

echo.
echo ⚠ Симулируем процесс сборки с ошибкой...
timeout /t 2 /nobreak >nul

echo 📦 Сканируем молоко и хлеб...
for %%P in (MILK-1L BREAD) do (
    echo - ✓ %%P отсканирован
    timeout /t 1 /nobreak >nul
)

echo.
echo ❌ ОШИБКА: Йогурт отсутствует!
if "%SAGA_STATUS%"=="200" (
    curl -s -X POST "%SAGA_URL%/api/transactions/%TRANSACTION_ID%/simulate-error" ^
      -H "Content-Type: application/json" ^
      -d "{ \
        \"productId\": \"YOGURT\", \
        \"quantity\": 2, \
        \"reason\": \"Товар отсутствует на основном складе\" \
      }" >nul && echo "✅ Ошибка отправлена в Saga"
) else (
    echo "✅ Симуляция: ошибка зафиксирована"
)

echo.
echo 📊 Транзакция перешла в состояние PAUSED
echo 📞 Офис уведомлен, клиенту отправлен запрос
echo.

pause
goto MAIN_MENU

:SCENARIO_3
cls
echo ===============================================================================
echo 💥 СЦЕНАРИЙ 3: КАСКАДНЫЕ ОШИБКИ (СКРЫТЫЕ ДЕФИЦИТЫ) 🔥
echo ===============================================================================

echo 🎯 ЭТО ГЛАВНЫЙ СЦЕНАРИЙ ДЕМОНСТРАЦИИ!
echo.

echo 🔧 Подготовка: создаем МНОЖЕСТВЕННЫЕ дефициты...
if "%DB_AVAILABLE%"=="1" (
    psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -c "
    UPDATE products SET stock = 0 WHERE product_id = 'YOGURT';
    UPDATE products SET stock = 1 WHERE product_id = 'EGGS';
    UPDATE products SET stock = 2 WHERE product_id = 'CHEESE';
    UPDATE products SET stock = 5 WHERE product_id = 'BUTTER';
    " 2>nul && echo "✅ Созданы скрытые дефициты"
) else (
    echo "✅ Симуляция: созданы скрытые дефициты"
)

echo.
echo 📊 ТЕКУЩИЕ ОСТАТКИ:
call :SHOW_PRODUCTS_STOCK "MILK-1L,BREAD,BUTTER,EGGS,CHEESE,YOGURT"

echo.
echo 🛒 Создаем БОЛЬШОЙ заказ...
set ORDER_ID=ORDER-CASCADE-%RANDOM%

echo 📝 Создаем транзакцию через Saga...
if "%SAGA_STATUS%"=="200" (
    curl -s -X POST "%SAGA_URL%/api/transactions" ^
      -H "Content-Type: application/json" ^
      -d "{ \
        \"orderId\": \"%ORDER_ID%\", \
        \"collectorId\": \"%COLLECTOR_ID%\", \
        \"clientId\": \"%CLIENT_ID%\", \
        \"items\": [ \
          {\"productId\": \"MILK-1L\", \"quantity\": 2}, \
          {\"productId\": \"BREAD\", \"quantity\": 1}, \
          {\"productId\": \"BUTTER\", \"quantity\": 3}, \
          {\"productId\": \"EGGS\", \"quantity\": 10}, \
          {\"productId\": \"CHEESE\", \"quantity\": 5}, \
          {\"productId\": \"YOGURT\", \"quantity\": 4} \
        ] \
      }" > temp_response.json 2>nul
    
    call :EXTRACT_JSON_FIELD "transactionId" temp_response.json
    set TRANSACTION_ID=%JSON_VALUE%
) else (
    set TRANSACTION_ID=SIM-TX-%RANDOM%
)

echo.
echo ⚠ АНАЛИЗ ПРОБЛЕМ:
echo - Йогурт: 0 (явная проблема - будет обнаружена сразу)
echo - Яйца: 1 из 10 (скрытая - обнаружится при проверке)
echo - Сыр: 2 из 5 (скрытая - обнаружится при проверке)
echo - Масло: 5 из 3? (ошибка: нужно 3, есть 5 - ОК)
echo.

echo ⏳ Начинаем процесс сборки...
echo.

echo 📦 Этап 1: Сканируем то, что ЕСТЬ...
for %%P in (MILK-1L BREAD) do (
    echo - Сканируем %%P...
    if "%DB_AVAILABLE%"=="1" (
        psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -c ^
            "UPDATE products SET stock = GREATEST(0, stock - 1) WHERE product_id = '%%P';" 2>nul
    )
    timeout /t 1 /nobreak >nul
)

echo.
echo ❌ Этап 2: Обнаруживаем ПЕРВУЮ ошибку - нет Йогурта!
if "%SAGA_STATUS%"=="200" (
    curl -s -X POST "%SAGA_URL%/api/transactions/%TRANSACTION_ID%/simulate-error" ^
      -H "Content-Type: application/json" ^
      -d "{ \
        \"productId\": \"YOGURT\", \
        \"quantity\": 4, \
        \"reason\": \"Йогурт закончился на складе\" \
      }" >nul
)

echo.
echo ⏳ Ждем 3 секунды (в реальности - 15 минут, другие клиенты забирают товары)...
timeout /t 3 /nobreak >nul

echo.
echo 💥 КАТАСТРОФА: За время ожидания другие клиенты забрали товары!
if "%DB_AVAILABLE%"=="1" (
    echo 📊 РЕАЛЬНЫЕ остатки СЕЙЧАС:
    psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -c "
    SELECT 
        p.product_id as товар,
        p.stock as \"текущий остаток\",
        CASE p.product_id 
            WHEN 'EGGS' THEN 10
            WHEN 'CHEESE' THEN 5
            WHEN 'BUTTER' THEN 3
            ELSE 1
        END as \"нужно для заказа\",
        CASE 
            WHEN p.product_id = 'EGGS' AND p.stock < 10 THEN '❌ НЕ ХВАТИТ ' || (10 - p.stock)
            WHEN p.product_id = 'CHEESE' AND p.stock < 5 THEN '❌ НЕ ХВАТИТ ' || (5 - p.stock)
            WHEN p.product_id = 'BUTTER' AND p.stock < 3 THEN '❌ НЕ ХВАТИТ ' || (3 - p.stock)
            WHEN p.stock = 0 THEN '❌ НЕТ'
            ELSE '✓ ХВАТИТ'
        END as результат
    FROM products p 
    WHERE p.product_id IN ('BUTTER', 'EGGS', 'CHEESE');" 2>nul
)

echo.
echo 📢 ВЫВОД: Клиент согласился на заказ без 1 товара
echo          Но получит заказ без 3-4 товаров!
echo          И узнает об этом ТОЛЬКО при получении!
echo.

pause
goto MAIN_MENU

:SCENARIO_4
goto SCENARIO_4_SIM
:SCENARIO_5
goto SCENARIO_5_SIM
:SHOW_DB_STATE
goto SHOW_DB_SIM
:CLEAR_DATA
goto CLEAR_SIM
:REAL_TIME_MONITOR
goto MONITOR_SIM

:: ================================================================
:: СИМУЛЯЦИОННЫЕ РАЗДЕЛЫ (для случая когда БД/Saga не доступны)
:: ================================================================

:SCENARIO_4_SIM
cls
echo ===============================================================================
echo 🏃 СЦЕНАРИЙ 4: КЛИЕНТ ТРЕБУЕТ ВСЕ ТОВАРЫ
echo ===============================================================================

echo 👑 Клиент: \"Я заплатил за всё - хочу получить ВСЕ товары!\"
echo 🔎 \"Найдите йогурт на других складах!\"
echo.

echo 🌐 Поиск по складам (симуляция)...
timeout /t 2 /nobreak >nul
echo ✅ Найден йогурт на холодном складе: 15 шт
echo.

echo 🚚 Межскладская перевозка: 50 минут
echo 💰 Дополнительные затраты: 700 рублей
echo.

echo 🎯 ИТОГ: Клиент получил ВСЕ товары через 75+ минут
echo 💸 Дополнительные затраты: 700+ рублей
echo 😠 Клиент недоволен долгим ожиданием
echo.
pause
goto MAIN_MENU

:SCENARIO_5_SIM
cls
echo ===============================================================================
echo 🌙 СЦЕНАРИЙ 5: НОЧНОЙ КОШМАР (МАГАЗИНЫ ЗАКРЫТЫ)
echo ===============================================================================

echo 🕖 ВРЕМЯ: 19:45 | Магазины до: 20:00
echo 📦 Клиент получает: Молоко и Хлеб (из 6 товаров)
echo 😡 \"ГДЕ ОСТАЛЬНОЕ?! ГОСТИ ЧЕРЕЗ 15 МИНУТ!\"
echo.

echo 🕗 20:00 - Магазины ЗАКРЫТЫ
echo 🚫 Альтернатив нет
echo 💰 ФИНАНСОВЫЕ ПОТЕРИ: ~17,730 рублей
echo ⚖️ ЮРИДИЧЕСКИЕ РИСКИ: +8,000 рублей
echo.
pause
goto MAIN_MENU

:SHOW_DB_SIM
cls
echo ===============================================================================
echo 📊 СОСТОЯНИЕ СИСТЕМЫ (СИМУЛЯЦИЯ)
echo ===============================================================================
echo.

if "%DB_AVAILABLE%"=="1" (
    echo 🏪 ТОВАРЫ В БД %DB_NAME%:
    psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -c "
    SELECT 
        p.product_id as \"ID\",
        p.name as \"Название\",
        p.stock as \"Остаток\",
        CASE 
            WHEN p.stock = 0 THEN '❌ НЕТ'
            WHEN p.stock < 5 THEN '⚠ МАЛО'
            ELSE '✓ ЕСТЬ'
        END as \"Статус\"
    FROM products p 
    ORDER BY p.stock, p.product_id
    LIMIT 10;" 2>nul
) else (
    echo 🏪 ТОВАРЫ (симуляция):
    echo MILK-1L: 49 шт ✓
    echo BREAD: 29 шт ✓
    echo BUTTER: 5 шт ⚠
    echo EGGS: 1 шт ❌
    echo CHEESE: 2 шт ⚠
    echo YOGURT: 0 шт ❌
)

echo.
if "%SAGA_STATUS%"=="200" (
    echo 🔄 АКТИВНЫЕ ТРАНЗАКЦИИ:
    curl -s "%SAGA_URL%/api/transactions" 2>nul | python -c "import json,sys; data=json.load(sys.stdin) if sys.stdin.readable() else []; [print(f'• {t.get(\"transactionId\",\"?\")}: {t.get(\"status\",\"?\")}') for t in data[:3]]" 2>nul || echo "Нет данных или ошибка"
) else (
    echo 🔄 ТРАНЗАКЦИИ: Saga не доступен
)

echo.
pause
goto MAIN_MENU

:CLEAR_SIM
cls
echo ===============================================================================
echo 🧹 ОЧИСТКА ДАННЫХ
echo ===============================================================================
echo.

set /p confirm="❓ Очистить все тестовые данные? (y/n): "
if not "%confirm%"=="y" goto MAIN_MENU

echo.
if "%DB_AVAILABLE%"=="1" (
    echo 🗑️ Очищаем БД...
    psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -c "
    DELETE FROM demo_transactions;
    DELETE FROM demo_orders;
    UPDATE products SET stock = 50 WHERE product_id = 'MILK-1L';
    UPDATE products SET stock = 30 WHERE product_id = 'BREAD';
    UPDATE products SET stock = 20 WHERE product_id = 'BUTTER';
    UPDATE products SET stock = 100 WHERE product_id = 'EGGS';
    UPDATE products SET stock = 15 WHERE product_id = 'CHEESE';
    UPDATE products SET stock = 40 WHERE product_id = 'YOGURT';
    " 2>nul && echo "✅ Данные очищены"
) else (
    echo ✅ Симуляция: данные сброшены
)

echo.
pause
goto MAIN_MENU

:MONITOR_SIM
cls
echo ===============================================================================
echo 📡 МОНИТОРИГ (СИМУЛЯЦИЯ)
echo ===============================================================================
echo.

echo 🎮 Нажмите Ctrl+C для выхода
echo.
:MONITOR_LOOP
echo 📍 Время: %TIME%
echo 📦 Состояние системы:
echo.
echo 🏪 КРИТИЧЕСКИЕ ТОВАРЫ:
echo - YOGURT: 0 шт 🔴
echo - EGGS: 1 шт 🟡
echo - CHEESE: 2 шт 🟡
echo - BUTTER: 5 шт 🟢
echo.
echo ⏳ Следующее обновление через 5 сек...
timeout /t 5 /nobreak >nul
goto MONITOR_LOOP

:: ===============================================================================
:: ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ
:: ===============================================================================

:SHOW_PRODUCTS_STOCK
set PRODUCT_LIST=%~1

if "%DB_AVAILABLE%"=="0" (
    echo 📊 Симуляция остатков:
    for %%p in (%PRODUCT_LIST%) do (
        if "%%p"=="YOGURT" echo - %%p: 0 шт ❌
        if "%%p"=="EGGS" echo - %%p: 1 шт ⚠
        if "%%p"=="CHEESE" echo - %%p: 2 шт ⚠
        if "%%p"=="BUTTER" echo - %%p: 5 шт ✓
        if "%%p"=="MILK-1L" echo - %%p: 50 шт ✓
        if "%%p"=="BREAD" echo - %%p: 30 шт ✓
    )
    exit /b
)

echo 📊 Остатки из БД:
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -c "
SELECT 
    p.product_id as товар,
    p.stock as остаток,
    CASE 
        WHEN p.stock = 0 THEN '❌ НЕТ'
        WHEN p.stock < 3 THEN '⚠ ОЧЕНЬ МАЛО'
        WHEN p.stock < 10 THEN '⚠ МАЛО'
        ELSE '✓ НОРМА'
    END as статус
FROM products p 
WHERE p.product_id IN ('%PRODUCT_LIST:,%')
ORDER BY p.stock;" 2>nul
exit /b

:EXTRACT_JSON_FIELD
set FIELD=%~1
set FILE=%~2

if not exist "%FILE%" (
    set JSON_VALUE=
    exit /b
)

:: Простой парсинг JSON (без PowerShell)
for /f "usebackq tokens=2 delims=:," %%a in (`type "%FILE%" ^| findstr /i "\"%FIELD%\""`) do (
    set JSON_VALUE=%%a
    set JSON_VALUE=%JSON_VALUE:"=%
    set JSON_VALUE=%JSON_VALUE: =%
    goto :EXTRACT_DONE
)

:EXTRACT_DONE
del "%FILE%" 2>nul
exit /b