@echo off
chcp 65001 >nul
echo =========================================
echo ШАГ 1: СОЗДАНИЕ ФАЙЛА ПАРОЛЯ
echo =========================================
echo.

:: ШАГ 1.1: Создаем файл с паролем
echo Ghbdtnbr123! > C:\temp\pgpass.txt
echo ✅ Файл пароля создан: C:\temp\pgpass.txt
echo Содержимое файла:
type C:\temp\pgpass.txt
echo.

:: ШАГ 1.2: Устанавливаем права на файл
echo Устанавливаем права доступа...
icacls C:\temp\pgpass.txt /inheritance:r /grant:r "%USERNAME%:R" 2>nul
echo.

:: ШАГ 1.3: Настраиваем PGPASSFILE
set PGPASSFILE=C:\temp\pgpass.txt
echo Переменная PGPASSFILE установлена: %PGPASSFILE%
echo.
echo =========================================
echo ШАГ 2: ТЕСТ ПОДКЛЮЧЕНИЯ
echo =========================================
echo.

:: Тест 1: Подключение к серверу
echo Тест 1: Подключение к PostgreSQL...
psql -U postgres -c "SELECT version();" 2>nul
if %errorlevel% equ 0 (
    echo ✅ Тест 1 УСПЕШЕН: PostgreSQL доступен
) else (
    echo ❌ Тест 1 ПРОВАЛЕН: Не удалось подключиться
    echo.
    goto :ERROR_HANDLING
)

echo.
:: Тест 2: Проверка базы kefir_db
echo Тест 2: Проверка базы kefir_db...
psql -U postgres -d kefir_db -c "SELECT current_database();" 2>nul
if %errorlevel% equ 0 (
    echo ✅ Тест 2 УСПЕШЕН: База kefir_db доступна
) else (
    echo ⚠ База kefir_db не существует, создаем...
    psql -U postgres -c "CREATE DATABASE kefir_db;" 2>nul
    if %errorlevel% equ 0 (
        echo ✅ База kefir_db создана
    ) else (
        echo ❌ Не удалось создать базу
    )
)

echo.
echo =========================================
echo ШАГ 3: СОЗДАНИЕ ТЕСТОВЫХ ДАННЫХ
echo =========================================
echo.

psql -U postgres -d kefir_db -c "
-- Создаем простую таблицу
CREATE TABLE IF NOT EXISTS test_products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100),
    stock INTEGER
);

-- Вставляем тестовые данные
INSERT INTO test_products (name, stock) VALUES 
('Молоко', 50),
('Хлеб', 30),
('Йогурт', 0)
ON CONFLICT DO NOTHING;

-- Проверяем
SELECT '✅ Данные созданы:', COUNT(*) FROM test_products;
" 2>nul

echo.
echo =========================================
echo РЕЗУЛЬТАТ
echo =========================================
echo.
echo 📊 Показываем данные из базы:
psql -U postgres -d kefir_db -c "SELECT * FROM test_products;" 2>nul

echo.
echo =========================================
echo ДЕМОНСТРАЦИЯ РАБОТЫ ПАРОЛЯ ИЗ ФАЙЛА
echo =========================================
echo.
echo Теперь демо, что пароль читается из файла:
echo Удаляем переменную PGPASSFILE...
set PGPASSFILE=
echo Пробуем подключиться БЕЗ файла пароля (должно не получиться):
psql -U postgres -c "SELECT 1;" 2>nul && echo ❌ НЕОЖИДАННО: Подключилось без пароля! || echo ✅ ОЖИДАЕМО: Не подключилось без пароля

echo.
echo Восстанавливаем PGPASSFILE...
set PGPASSFILE=C:\temp\pgpass.txt
echo Пробуем подключиться С файлом пароля:
psql -U postgres -c "SELECT '✅ УСПЕХ! Пароль из файла работает!';" 2>nul

echo.
pause
goto :END

:ERROR_HANDLING
echo =========================================
echo ДИАГНОСТИКА ОШИБКИ
echo =========================================
echo.
echo Проверяем файл пароля:
if exist C:\temp\pgpass.txt (
    echo Файл существует, размер: 
    for %%A in (C:\temp\pgpass.txt) do echo %%~zA байт
    echo Содержимое:
    type C:\temp\pgpass.txt
) else (
    echo ❌ Файл не существует!
)

echo.
echo Проверяем переменную:
echo PGPASSFILE=%PGPASSFILE%

echo.
echo Проверяем PostgreSQL вручную:
echo ЗАПУСТИТЕ КОМАНДУ ВРУЧНУЮ:
echo psql -U postgres
echo и введите пароль: Ghbdtnbr123!
echo.
pause

:END
echo Очистка...
set PGPASSFILE=
echo Скрипт завершен.
pause