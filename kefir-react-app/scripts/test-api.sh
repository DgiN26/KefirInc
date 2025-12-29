#!/bin/bash
echo "🧪 Тестирование подключения к API..."
API_URL=$(grep "REACT_APP_API_URL" .env | cut -d '=' -f2)

if [ -z "$API_URL" ]; then
  echo "❌ API_URL не найден в .env"
  exit 1
fi

echo "📡 Проверяем подключение к: $API_URL"

# Проверяем доступность API
if curl -s --head --request GET "$API_URL/clients" | grep "200 OK" > /dev/null; then
  echo "✅ API доступен!"
  
  # Получаем список клиентов (если API работает)
  echo ""
  echo "📋 Тестовый запрос к /clients:"
  curl -s "$API_URL/clients" | head -20
else
  echo "❌ API недоступен!"
  echo "Проверьте:"
  echo "1. Запущен ли Spring Boot бэкенд?"
  echo "2. Правильный ли URL в .env?"
  echo "3. Открыт ли порт 8080?"
fi