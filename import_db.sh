#!/bin/bash
# import_all_dbs.sh
PG_USER="postgres"
PG_PASS="Ghbdtnbr123!"
BACKUP_DIR="/home/vboxuser/Desktop/Kefir/bd/"

echo "Начинаем импорт всех баз данных..."

for sql_file in "$BACKUP_DIR"/*.sql; do
    if [ -f "$sql_file" ]; then
        # Извлекаем имя БД из имени файла (без .sql)
        db_name=$(basename "$sql_file" .sql)
        
        echo "Создаем БД: $db_name"
        sudo -u postgres psql -c "CREATE DATABASE \"$db_name\";"
        
        echo "Импортируем данные в $db_name из $sql_file"
        sudo -u postgres psql -d "$db_name" -f "$sql_file"
        
        if [ $? -eq 0 ]; then
            echo "✓ БД $db_name успешно импортирована"
        else
            echo "✗ Ошибка при импорте $db_name"
        fi
    fi
done

echo "Готово!"