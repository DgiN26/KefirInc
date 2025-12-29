#!/bin/bash
# Скрипт для переключения между моковым и реальным API

echo "🔧 Переключение режима API..."

if [ "$1" = "real" ]; then
  echo "REACT_APP_USE_REAL_API=true" > .env
  echo "✅ Переключились на РЕАЛЬНЫЙ API"
  echo "📡 API URL: http://localhost:8080/api"
  
elif [ "$1" = "mock" ]; then
  echo "REACT_APP_USE_REAL_API=false" > .env
  echo "✅ Переключились на МОКОВЫЙ API"
  echo "🧪 Используются демо-данные"
  
elif [ "$1" = "check" ]; then
  if grep -q "REACT_APP_USE_REAL_API=true" .env 2>/dev/null; then
    echo "📡 Текущий режим: РЕАЛЬНЫЙ API"
  else
    echo "🧪 Текущий режим: МОКОВЫЙ API"
  fi
  
elif [ "$1" = "dev" ]; then
  echo "REACT_APP_API_URL=http://localhost:8080/api" > .env
  echo "REACT_APP_USE_REAL_API=false" >> .env
  echo "✅ Настройки разработки установлены"
  
elif [ "$1" = "prod" ]; then
  echo "REACT_APP_API_URL=https://ваш-сервер.ru/api" > .env
  echo "REACT_APP_USE_REAL_API=true" >> .env
  echo "✅ Настройки продакшена установлены"
  
else
  echo "Использование: ./toggle-api.sh [real|mock|check|dev|prod]"
  echo ""
  echo "  real    - использовать реальный API"
  echo "  mock    - использовать моковый API (по умолчанию)"
  echo "  check   - проверить текущий режим"
  echo "  dev     - настройки для разработки"
  echo "  prod    - настройки для продакшена"
fi

# Показываем текущие настройки
echo ""
echo "📋 Текущие настройки .env:"
cat .env 2>/dev/null || echo "Файл .env не существует"