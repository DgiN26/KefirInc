#!/bin/bash

echo "🔍 Проверка состояния сервисов KEFIR..."
echo "======================================"

SERVICES=(
  "8080:API Gateway"
  "8097:Auth"
  "8081:User Service"
  "8082:Sklad Service"
  "8083:Delivery Service"
  "8084:Collector Service"
  "8085:Backet Service"
  "8086:Office Service"
)

for service in "${SERVICES[@]}"; do
  port=$(echo $service | cut -d: -f1)
  name=$(echo $service | cut -d: -f2)
  
  if curl -s --head --connect-timeout 2 "http://localhost:$port/actuator/health" > /dev/null; then
    echo "✅ $name (порт $port) - ЗАПУЩЕН"
    
    # Пробуем получить данные если сервис доступен
    case $port in
      8081) # User Service
        echo "   👥 Клиенты: $(curl -s http://localhost:$port/api/clients | jq '. | length' 2>/dev/null || echo 'N/A') записей" ;;
      8082) # Sklad Service
        echo "   📦 Товары: $(curl -s http://localhost:$port/api/products | jq '. | length' 2>/dev/null || echo 'N/A') записей" ;;
    esac
    
  else
    echo "❌ $name (порт $port) - НЕ ДОСТУПЕН"
  fi
done

echo ""
echo "📊 Логи сервисов:"
ls -la Backend/*.log 2>/dev/null || echo "Логи не найдены"
