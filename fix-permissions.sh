#!/bin/bash

echo "🔄 Исправление прав доступа после Git pull..."

# Меняем владельца на текущего пользователя
sudo chown -R $(whoami):$(whoami) .

# Стандартные права для папок и файлов
find . -type d -exec chmod 755 {} \;
find . -type f -exec chmod 644 {} \;

# Права на выполнение для скриптов
find . -name "*.sh" -exec chmod +x {} \;
find . -name "*.py" -exec chmod +x {} \;
find . -name "*.jar" -exec chmod +x {} \;

# Особые права для node_modules (если есть React)
[ -d "kefir-react-app/node_modules" ] && chmod -R 755 kefir-react-app/node_modules

echo "✅ Права исправлены!"
